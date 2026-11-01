package com.rightfull.validation.service;

import com.rightfull.activation.entity.ActivationStatus;
import com.rightfull.activation.entity.LicenseActivation;
import com.rightfull.activation.repository.LicenseActivationRepository;
import com.rightfull.common.util.CryptoUtils;
import com.rightfull.licensing.crypto.EntitlementTokenSigner;
import com.rightfull.licensing.crypto.EntitlementTokenSigner.EntitlementTokenClaims;
import com.rightfull.licensing.crypto.LicenseKeyGenerator;
import com.rightfull.licensing.entity.License;
import com.rightfull.licensing.entity.LicenseStatus;
import com.rightfull.licensing.repository.LicenseRepository;
import com.rightfull.product.entity.Product;
import com.rightfull.product.entity.ProductStatus;
import com.rightfull.product.repository.ProductRepository;
import com.rightfull.product.service.ProductSigningKeyService;
import com.rightfull.security.tenant.TenantContextManager;
import com.rightfull.validation.dto.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.PrivateKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

/**
 * Public high-throughput runtime validation and device activation engine.
 * Unauthenticated endpoint execution with strict product-tenant resolution and pessimistic concurrency locking.
 */
@Service
public class LicenseValidationService {

    private static final Logger log = LoggerFactory.getLogger(LicenseValidationService.class);

    private final LicenseRepository licenseRepository;
    private final LicenseActivationRepository activationRepository;
    private final ProductRepository productRepository;
    private final ProductSigningKeyService signingKeyService;
    private final LicenseKeyGenerator keyGenerator;
    private final EntitlementTokenSigner tokenSigner;
    private final TenantContextManager tenantContextManager;

    @PersistenceContext
    private EntityManager entityManager;

    public LicenseValidationService(
            LicenseRepository licenseRepository,
            LicenseActivationRepository activationRepository,
            ProductRepository productRepository,
            ProductSigningKeyService signingKeyService,
            LicenseKeyGenerator keyGenerator,
            EntitlementTokenSigner tokenSigner,
            TenantContextManager tenantContextManager) {
        this.licenseRepository = licenseRepository;
        this.activationRepository = activationRepository;
        this.productRepository = productRepository;
        this.signingKeyService = signingKeyService;
        this.keyGenerator = keyGenerator;
        this.tokenSigner = tokenSigner;
        this.tenantContextManager = tenantContextManager;
    }

    /**
     * Publicly validates a license key against a registered product.
     */
    @Transactional
    public ValidateLicenseResponse validate(ValidateLicenseRequest request) {
        UUID workspaceId = resolveProductWorkspace(request.productId());
        if (workspaceId == null) {
            log.warn("Validation failed: Product not found or inactive: {}", request.productId());
            return ValidateLicenseResponse.invalid(ValidationResultStatus.PRODUCT_MISMATCH, "Product not found or inactive");
        }

        tenantContextManager.applyTenantContext(workspaceId, null, null, "public-validator");

        String keyHash = keyGenerator.hashKey(request.licenseKey());
        Optional<License> optLicense = licenseRepository.findByProductIdAndLicenseKeyHash(request.productId(), keyHash);

        if (optLicense.isEmpty()) {
            return ValidateLicenseResponse.invalid(ValidationResultStatus.INVALID_KEY, "Invalid license key or product mismatch");
        }

        License license = optLicense.get();

        if (license.getStatus() == LicenseStatus.REVOKED) {
            return ValidateLicenseResponse.invalid(ValidationResultStatus.REVOKED, "License has been permanently revoked");
        }
        if (license.getStatus() == LicenseStatus.SUSPENDED) {
            return ValidateLicenseResponse.invalid(ValidationResultStatus.SUSPENDED, "License is temporarily suspended");
        }
        if (license.isExpired()) {
            return ValidateLicenseResponse.invalid(ValidationResultStatus.EXPIRED, "License has expired");
        }

        String deviceHash = null;
        if (request.deviceId() != null && !request.deviceId().isBlank()) {
            deviceHash = CryptoUtils.sha256Hex(request.deviceId().trim());
            Optional<LicenseActivation> optActivation = activationRepository
                    .findByLicenseIdAndDeviceIdentifierHash(license.getId(), deviceHash);

            if (optActivation.isPresent()) {
                LicenseActivation activation = optActivation.get();
                if (activation.getStatus() == ActivationStatus.DEACTIVATED) {
                    return ValidateLicenseResponse.invalid(ValidationResultStatus.DEVICE_DEACTIVATED, "Device has been deactivated");
                }
                activation.updateHeartbeat(request.appVersion());
                activationRepository.save(activation);
            }
        }

        // Mint signed JWS EdDSA entitlement token
        String token = mintValidationToken(license, deviceHash);

        return ValidateLicenseResponse.valid(
                token,
                license.getExpiresAt(),
                license.getOfflineGracePeriodSeconds(),
                license.getRevalidationIntervalSeconds(),
                license.getEntitlements(),
                license.getSigningKeyId()
        );
    }

    /**
     * Registers and binds a device to an issued license under atomic pessimistic concurrency control.
     */
    @Transactional
    public ActivateDeviceResponse activateDevice(ActivateDeviceRequest request) {
        UUID workspaceId = resolveProductWorkspace(request.productId());
        if (workspaceId == null) {
            return ActivateDeviceResponse.error(ValidationResultStatus.PRODUCT_MISMATCH, "Product not found or inactive");
        }

        tenantContextManager.applyTenantContext(workspaceId, null, null, "public-activator");

        String keyHash = keyGenerator.hashKey(request.licenseKey());
        Optional<License> optLicense = licenseRepository.findByProductIdAndLicenseKeyHash(request.productId(), keyHash);

        if (optLicense.isEmpty()) {
            return ActivateDeviceResponse.error(ValidationResultStatus.INVALID_KEY, "Invalid license key");
        }

        // Acquire row lock on the license to prevent concurrent activation over-allocation
        License license = licenseRepository.findByIdWithPessimisticLock(optLicense.get().getId())
                .orElse(optLicense.get());

        if (license.getStatus() == LicenseStatus.REVOKED) {
            return ActivateDeviceResponse.error(ValidationResultStatus.REVOKED, "License has been permanently revoked");
        }
        if (license.getStatus() == LicenseStatus.SUSPENDED) {
            return ActivateDeviceResponse.error(ValidationResultStatus.SUSPENDED, "License is temporarily suspended");
        }
        if (license.isExpired()) {
            return ActivateDeviceResponse.error(ValidationResultStatus.EXPIRED, "License has expired");
        }

        String deviceHash = CryptoUtils.sha256Hex(request.deviceId().trim());
        Optional<LicenseActivation> existing = activationRepository
                .findByLicenseIdAndDeviceIdentifierHash(license.getId(), deviceHash);

        LicenseActivation activation;
        if (existing.isPresent()) {
            activation = existing.get();
            if (activation.getStatus() == ActivationStatus.ACTIVE) {
                // Idempotent reactivation
                activation.updateHeartbeat(request.appVersion());
                activationRepository.save(activation);
            } else {
                // Previously deactivated: check if slots available
                if (license.getCurrentActivations() >= license.getMaxActivations()) {
                    return ActivateDeviceResponse.error(ValidationResultStatus.ACTIVATION_LIMIT_REACHED,
                            "Maximum activation limit reached (" + license.getMaxActivations() + ")");
                }
                activation.reactivate(request.deviceName(), request.devicePlatform(), request.appVersion());
                license.incrementActivations();
                licenseRepository.save(license);
                activationRepository.save(activation);
            }
        } else {
            // New device registration
            if (license.getCurrentActivations() >= license.getMaxActivations()) {
                return ActivateDeviceResponse.error(ValidationResultStatus.ACTIVATION_LIMIT_REACHED,
                        "Maximum activation limit reached (" + license.getMaxActivations() + ")");
            }

            license.incrementActivations();
            licenseRepository.save(license);

            activation = new LicenseActivation(
                    workspaceId,
                    license.getId(),
                    license.getProductId(),
                    deviceHash,
                    request.deviceName(),
                    request.devicePlatform(),
                    request.appVersion()
            );
            activation = activationRepository.save(activation);
        }

        String token = mintValidationToken(license, deviceHash);
        return ActivateDeviceResponse.success(token, activation.getId(), license.getCurrentActivations(), license.getMaxActivations());
    }

    /**
     * Deactivates a device registration, freeing up an activation slot.
     */
    @Transactional
    public boolean deactivateDevice(DeactivateDeviceRequest request) {
        UUID workspaceId = resolveProductWorkspace(request.productId());
        if (workspaceId == null) {
            return false;
        }

        tenantContextManager.applyTenantContext(workspaceId, null, null, "public-activator");

        String keyHash = keyGenerator.hashKey(request.licenseKey());
        Optional<License> optLicense = licenseRepository.findByProductIdAndLicenseKeyHash(request.productId(), keyHash);

        if (optLicense.isEmpty()) {
            return false;
        }

        License license = licenseRepository.findByIdWithPessimisticLock(optLicense.get().getId())
                .orElse(optLicense.get());

        String deviceHash = CryptoUtils.sha256Hex(request.deviceId().trim());
        Optional<LicenseActivation> existing = activationRepository
                .findByLicenseIdAndDeviceIdentifierHash(license.getId(), deviceHash);

        if (existing.isPresent() && existing.get().getStatus() == ActivationStatus.ACTIVE) {
            LicenseActivation activation = existing.get();
            activation.deactivate("CLIENT");
            license.decrementActivations();
            licenseRepository.save(license);
            activationRepository.save(activation);
            return true;
        }

        return false;
    }

    /**
     * Refreshes last validation heartbeat timestamp and issues updated signed token.
     */
    @Transactional
    public ValidateLicenseResponse heartbeat(HeartbeatRequest request) {
        ValidateLicenseRequest valReq = new ValidateLicenseRequest(
                request.productId(),
                request.licenseKey(),
                request.deviceId(),
                request.appVersion(),
                null
        );
        return validate(valReq);
    }

    private String mintValidationToken(License license, String deviceHash) {
        PrivateKey privateKey = signingKeyService.getActivePrivateKey(license.getProductId());

        Instant now = Instant.now();
        Instant tokenExpiry = (license.getExpiresAt() != null)
                ? license.getExpiresAt()
                : now.plus(license.getOfflineGracePeriodSeconds(), ChronoUnit.SECONDS);

        EntitlementTokenClaims claims = new EntitlementTokenClaims(
                "rightfull.com",
                license.getId(),
                license.getProductId(),
                license.getStartsAt(),
                now,
                tokenExpiry,
                now,
                "VALID",
                deviceHash,
                license.getEntitlements(),
                license.getOfflineGracePeriodSeconds(),
                license.getRevalidationIntervalSeconds()
        );

        return tokenSigner.mintToken(claims, license.getSigningKeyId(), privateKey);
    }

    private UUID resolveProductWorkspace(UUID productId) {
        try {
            Object result = entityManager.createNativeQuery("SELECT fn_resolve_product_workspace(:pid)")
                    .setParameter("pid", productId)
                    .getSingleResult();
            if (result instanceof UUID uuid) return uuid;
            if (result instanceof String str && !str.isBlank()) return UUID.fromString(str);
            return null;
        } catch (Exception e) {
            // Graceful fallback when function is not yet installed or in unit test mock environments
            return productRepository.findById(productId)
                    .filter(p -> p.getStatus() == ProductStatus.ACTIVE)
                    .map(Product::getWorkspaceId)
                    .orElse(null);
        }
    }
}

