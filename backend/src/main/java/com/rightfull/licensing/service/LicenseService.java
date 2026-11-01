package com.rightfull.licensing.service;

import com.rightfull.activation.dto.ActivationResponse;
import com.rightfull.activation.entity.ActivationStatus;
import com.rightfull.activation.entity.LicenseActivation;
import com.rightfull.activation.repository.LicenseActivationRepository;
import com.rightfull.audit.WorkspaceAuditEvent;
import com.rightfull.audit.WorkspaceAuditEventType;
import com.rightfull.audit.WorkspaceAuditService;
import com.rightfull.common.exception.BusinessException;
import com.rightfull.common.exception.ResourceNotFoundException;
import com.rightfull.customer.entity.Customer;
import com.rightfull.customer.entity.CustomerStatus;
import com.rightfull.customer.repository.CustomerRepository;
import com.rightfull.licensing.crypto.LicenseKeyGenerator;
import com.rightfull.licensing.crypto.LicenseKeyGenerator.GeneratedLicenseKey;
import com.rightfull.licensing.dto.*;
import com.rightfull.licensing.entity.License;
import com.rightfull.licensing.entity.LicenseStatus;
import com.rightfull.licensing.repository.LicenseRepository;
import com.rightfull.plan.entity.LicensePlan;
import com.rightfull.plan.entity.PlanStatus;
import com.rightfull.plan.repository.LicensePlanRepository;
import com.rightfull.plan.validator.EntitlementValidator;
import com.rightfull.product.entity.KeyStatus;
import com.rightfull.product.entity.Product;
import com.rightfull.product.entity.ProductSigningKey;
import com.rightfull.product.entity.ProductStatus;
import com.rightfull.product.repository.ProductRepository;
import com.rightfull.product.repository.ProductSigningKeyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Core lifecycle service managing license issuance, state transitions, renewal,
 * archival/revocation, and activation oversight.
 */
@Service
public class LicenseService {

    private static final Logger log = LoggerFactory.getLogger(LicenseService.class);

    private final LicenseRepository licenseRepository;
    private final LicenseActivationRepository activationRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final LicensePlanRepository planRepository;
    private final ProductSigningKeyRepository signingKeyRepository;
    private final LicenseKeyGenerator keyGenerator;
    private final EntitlementValidator entitlementValidator;
    private final WorkspaceAuditService auditService;

    public LicenseService(
            LicenseRepository licenseRepository,
            LicenseActivationRepository activationRepository,
            ProductRepository productRepository,
            CustomerRepository customerRepository,
            LicensePlanRepository planRepository,
            ProductSigningKeyRepository signingKeyRepository,
            LicenseKeyGenerator keyGenerator,
            EntitlementValidator entitlementValidator,
            WorkspaceAuditService auditService) {
        this.licenseRepository = licenseRepository;
        this.activationRepository = activationRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.planRepository = planRepository;
        this.signingKeyRepository = signingKeyRepository;
        this.keyGenerator = keyGenerator;
        this.entitlementValidator = entitlementValidator;
        this.auditService = auditService;
    }

    /**
     * Atomically issues a new software product license.
     */
    @Transactional
    public IssueLicenseResponse issueLicense(
            UUID workspaceId,
            UUID productId,
            IssueLicenseRequest request,
            UUID actorId) {

        // 1. Validate Product Ownership and Status
        Product product = productRepository.findByIdAndWorkspaceId(productId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));

        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new BusinessException("Cannot issue licenses for an archived product", "PRODUCT_NOT_ACTIVE", HttpStatus.BAD_REQUEST);
        }

        // 2. Idempotency Check
        if (request.idempotencyKey() != null && !request.idempotencyKey().isBlank()) {
            Optional<License> existing = licenseRepository.findByWorkspaceIdAndIdempotencyKey(workspaceId, request.idempotencyKey().trim());
            if (existing.isPresent()) {
                log.info("Returning existing license for idempotency key: {}", request.idempotencyKey());
                License l = existing.get();
                return toIssueResponse(l, "[IDEMPOTENT_REISSUE_PROTECTED]");
            }
        }

        // 3. Validate Customer if provided
        Customer customer = null;
        if (request.customerId() != null) {
            customer = customerRepository.findByIdAndWorkspaceId(request.customerId(), workspaceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + request.customerId()));
            if (customer.getStatus() != CustomerStatus.ACTIVE) {
                throw new BusinessException("Cannot issue license to an archived customer", "CUSTOMER_NOT_ACTIVE", HttpStatus.BAD_REQUEST);
            }
        }

        // 4. Resolve Parameters: From Reusable Preset OR Direct Configuration
        Instant startsAt = Instant.now();
        Instant expiresAt;
        int maxActivations;
        int seatLimit;
        int offlineGracePeriodSeconds;
        int revalidationIntervalSeconds;
        Map<String, Object> entitlements;
        UUID planId = null;
        Integer planVersion = null;

        if (request.planId() != null) {
            LicensePlan plan = planRepository.findByIdAndWorkspaceId(request.planId(), workspaceId)
                    .orElseThrow(() -> new ResourceNotFoundException("License configuration preset not found: " + request.planId()));

            if (!plan.getProductId().equals(productId)) {
                throw new BusinessException("Selected configuration preset does not belong to this product", "PLAN_PRODUCT_MISMATCH", HttpStatus.BAD_REQUEST);
            }
            if (plan.getStatus() != PlanStatus.ACTIVE) {
                throw new BusinessException("Cannot issue license from an archived configuration preset", "PLAN_NOT_ACTIVE", HttpStatus.BAD_REQUEST);
            }

            planId = plan.getId();
            planVersion = plan.getVersion();
            expiresAt = (plan.getDurationDays() != null)
                    ? startsAt.plus(plan.getDurationDays(), ChronoUnit.DAYS)
                    : null;
            maxActivations = plan.getMaximumActivations();
            seatLimit = plan.getSeatLimit();
            offlineGracePeriodSeconds = plan.getOfflineGracePeriodSeconds();
            revalidationIntervalSeconds = plan.getRevalidationIntervalSeconds();
            entitlements = new LinkedHashMap<>(plan.getEntitlements());
        } else {
            // Direct Configuration Mode
            if (request.durationDays() != null) {
                expiresAt = startsAt.plus(request.durationDays(), ChronoUnit.DAYS);
            } else {
                expiresAt = null; // Perpetual
            }
            maxActivations = (request.maximumActivations() != null) ? request.maximumActivations() : 1;
            seatLimit = (request.seatLimit() != null) ? request.seatLimit() : 1;
            offlineGracePeriodSeconds = (request.offlineGracePeriodSeconds() != null) ? request.offlineGracePeriodSeconds() : 25200;
            revalidationIntervalSeconds = (request.revalidationIntervalSeconds() != null) ? request.revalidationIntervalSeconds() : 300;

            if (request.entitlements() != null) {
                entitlementValidator.validate(request.entitlements());
                entitlements = new LinkedHashMap<>(request.entitlements());
            } else {
                entitlements = new LinkedHashMap<>();
            }
        }

        // 5. Locate Active Ed25519 Signing Key
        ProductSigningKey activeSigningKey = signingKeyRepository.findByProductIdAndStatus(productId, KeyStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException("Product has no active Ed25519 signing key. Rotate or provision a key first.", "NO_ACTIVE_SIGNING_KEY", HttpStatus.PRECONDITION_FAILED));

        // 6. Generate Cryptographic License Key
        GeneratedLicenseKey key = keyGenerator.generate();

        // 7. Persist License
        License license = new License(
                workspaceId,
                productId,
                customer != null ? customer.getId() : null,
                planId,
                planVersion,
                key.keyHash(),
                key.keyPrefix(),
                key.lastFour(),
                startsAt,
                expiresAt,
                maxActivations,
                seatLimit,
                offlineGracePeriodSeconds,
                revalidationIntervalSeconds,
                entitlements,
                activeSigningKey.getKeyId(),
                request.externalReference(),
                request.idempotencyKey(),
                actorId
        );

        License saved = licenseRepository.save(license);
        log.info("Issued license id={} for product id={} (masked: {})", saved.getId(), productId, key.maskedKey());

        // 8. Record Compliance Audit Event
        auditService.recordEvent(WorkspaceAuditEvent.of(
                WorkspaceAuditEventType.LICENSE_ISSUED,
                workspaceId,
                actorId,
                Map.of(
                        "licenseId", saved.getId().toString(),
                        "productId", productId.toString(),
                        "keyPrefix", saved.getLicenseKeyPrefix(),
                        "isPerpetual", saved.isPerpetual(),
                        "customerId", (customer != null) ? customer.getId().toString() : "NONE"
                )
        ));

        // 9. Return One-Time Plaintext Response
        return toIssueResponse(saved, key.rawKey());
    }

    @Transactional(readOnly = true)
    public LicenseResponse getLicense(UUID workspaceId, UUID licenseId) {
        License license = licenseRepository.findByIdAndWorkspaceId(licenseId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("License not found: " + licenseId));
        return toResponse(license);
    }

    @Transactional(readOnly = true)
    public Page<LicenseResponse> listLicenses(
            UUID workspaceId,
            UUID productId,
            UUID customerId,
            LicenseStatus status,
            String search,
            Pageable pageable) {
        String cleanSearch = (search != null && !search.isBlank()) ? search.trim() : null;
        return licenseRepository.findWithFilters(workspaceId, productId, customerId, status, cleanSearch, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public LicenseSummaryResponse getLicenseSummary(UUID workspaceId) {
        long total = licenseRepository.countByWorkspaceId(workspaceId);
        long active = licenseRepository.countByWorkspaceIdAndStatus(workspaceId, LicenseStatus.ACTIVE);
        long expired = licenseRepository.countByWorkspaceIdAndStatus(workspaceId, LicenseStatus.EXPIRED);
        long suspended = licenseRepository.countByWorkspaceIdAndStatus(workspaceId, LicenseStatus.SUSPENDED);
        long revoked = licenseRepository.countByWorkspaceIdAndStatus(workspaceId, LicenseStatus.REVOKED);
        return new LicenseSummaryResponse(total, active, expired, suspended, revoked);
    }

    @Transactional
    public LicenseResponse suspendLicense(UUID workspaceId, UUID licenseId, String reason, UUID actorId) {
        License license = licenseRepository.findByIdAndWorkspaceId(licenseId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("License not found: " + licenseId));

        license.suspend(reason);
        License saved = licenseRepository.save(license);

        auditService.recordEvent(WorkspaceAuditEvent.of(
                WorkspaceAuditEventType.LICENSE_SUSPENDED,
                workspaceId,
                actorId,
                Map.of(
                        "licenseId", saved.getId().toString(),
                        "reason", reason != null ? reason : "Administrative suspension"
                )
        ));

        log.info("Suspended license id={} in workspace id={}", licenseId, workspaceId);
        return toResponse(saved);
    }

    @Transactional
    public LicenseResponse resumeLicense(UUID workspaceId, UUID licenseId, UUID actorId) {
        License license = licenseRepository.findByIdAndWorkspaceId(licenseId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("License not found: " + licenseId));

        license.resume();
        License saved = licenseRepository.save(license);

        auditService.recordEvent(WorkspaceAuditEvent.of(
                WorkspaceAuditEventType.LICENSE_RESUMED,
                workspaceId,
                actorId,
                Map.of("licenseId", saved.getId().toString())
        ));

        log.info("Resumed license id={} in workspace id={}", licenseId, workspaceId);
        return toResponse(saved);
    }

    @Transactional
    public LicenseResponse revokeLicense(UUID workspaceId, UUID licenseId, String reason, UUID actorId) {
        License license = licenseRepository.findByIdAndWorkspaceId(licenseId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("License not found: " + licenseId));

        license.revoke(reason);
        License saved = licenseRepository.save(license);

        auditService.recordEvent(WorkspaceAuditEvent.of(
                WorkspaceAuditEventType.LICENSE_REVOKED,
                workspaceId,
                actorId,
                Map.of(
                        "licenseId", saved.getId().toString(),
                        "reason", reason != null ? reason : "Administrative revocation"
                )
        ));

        log.warn("Revoked license id={} in workspace id={}", licenseId, workspaceId);
        return toResponse(saved);
    }

    @Transactional
    public LicenseResponse renewLicense(UUID workspaceId, UUID licenseId, RenewLicenseRequest request, UUID actorId) {
        License license = licenseRepository.findByIdAndWorkspaceId(licenseId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("License not found: " + licenseId));

        Instant newExpiresAt;
        if (request.newExpiresAt() != null) {
            newExpiresAt = request.newExpiresAt();
        } else if (request.extensionDays() != null) {
            Instant baseTime = (license.getExpiresAt() != null && license.getExpiresAt().isAfter(Instant.now()))
                    ? license.getExpiresAt()
                    : Instant.now();
            newExpiresAt = baseTime.plus(request.extensionDays(), ChronoUnit.DAYS);
        } else {
            throw new BusinessException("Must provide either extensionDays or newExpiresAt", "INVALID_RENEWAL_PARAMS", HttpStatus.BAD_REQUEST);
        }

        license.renew(newExpiresAt);
        License saved = licenseRepository.save(license);

        auditService.recordEvent(WorkspaceAuditEvent.of(
                WorkspaceAuditEventType.LICENSE_RENEWED,
                workspaceId,
                actorId,
                Map.of(
                        "licenseId", saved.getId().toString(),
                        "newExpiresAt", newExpiresAt.toString()
                )
        ));

        log.info("Renewed license id={} with new expiry {}", licenseId, newExpiresAt);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<ActivationResponse> getLicenseActivations(UUID workspaceId, UUID licenseId, Pageable pageable) {
        // Validate license belongs to workspace
        licenseRepository.findByIdAndWorkspaceId(licenseId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("License not found: " + licenseId));

        return activationRepository.findByLicenseId(licenseId, pageable)
                .map(this::toActivationResponse);
    }

    @Transactional
    public ActivationResponse deactivateDevice(UUID workspaceId, UUID licenseId, UUID activationId, UUID actorId) {
        License license = licenseRepository.findByIdAndWorkspaceId(licenseId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("License not found: " + licenseId));

        LicenseActivation activation = activationRepository.findByIdAndWorkspaceId(activationId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Activation not found: " + activationId));

        if (!activation.getLicenseId().equals(licenseId)) {
            throw new BusinessException("Activation does not belong to this license", "ACTIVATION_LICENSE_MISMATCH", HttpStatus.BAD_REQUEST);
        }

        if (activation.getStatus() == ActivationStatus.ACTIVE) {
            activation.deactivate("ADMIN");
            license.decrementActivations();
            licenseRepository.save(license);
            activationRepository.save(activation);

            auditService.recordEvent(WorkspaceAuditEvent.of(
                    WorkspaceAuditEventType.DEVICE_DEACTIVATED,
                    workspaceId,
                    actorId,
                    Map.of(
                            "licenseId", licenseId.toString(),
                            "activationId", activationId.toString(),
                            "deactivatedBy", "ADMIN"
                    )
            ));
        }

        return toActivationResponse(activation);
    }

    public LicenseResponse toResponse(License l) {
        String maskedKey = keyGenerator.mask(l.getLicenseKeyPrefix(), l.getLicenseKeyLastFour());
        return new LicenseResponse(
                l.getId(),
                l.getWorkspaceId(),
                l.getProductId(),
                l.getCustomerId(),
                l.getPlanId(),
                l.getPlanVersion(),
                l.getLicenseKeyPrefix(),
                maskedKey,
                l.getLicenseKeyLastFour(),
                l.getStatus(),
                l.getStartsAt(),
                l.getExpiresAt(),
                l.isPerpetual(),
                l.getMaxActivations(),
                l.getCurrentActivations(),
                l.getSeatLimit(),
                l.getOfflineGracePeriodSeconds(),
                l.getRevalidationIntervalSeconds(),
                l.getEntitlements(),
                l.getSigningKeyId(),
                l.getExternalReference(),
                l.getSuspendedAt(),
                l.getSuspendedReason(),
                l.getRevokedAt(),
                l.getRevokedReason(),
                l.getRenewedAt(),
                l.getCreatedAt(),
                l.getUpdatedAt()
        );
    }

    private IssueLicenseResponse toIssueResponse(License l, String rawKey) {
        String maskedKey = keyGenerator.mask(l.getLicenseKeyPrefix(), l.getLicenseKeyLastFour());
        return new IssueLicenseResponse(
                l.getId(),
                l.getWorkspaceId(),
                l.getProductId(),
                l.getCustomerId(),
                l.getPlanId(),
                l.getPlanVersion(),
                rawKey,
                l.getLicenseKeyPrefix(),
                maskedKey,
                l.getLicenseKeyLastFour(),
                l.getStatus(),
                l.getStartsAt(),
                l.getExpiresAt(),
                l.isPerpetual(),
                l.getMaxActivations(),
                l.getCurrentActivations(),
                l.getSeatLimit(),
                l.getOfflineGracePeriodSeconds(),
                l.getRevalidationIntervalSeconds(),
                l.getEntitlements(),
                l.getSigningKeyId(),
                l.getExternalReference(),
                l.getCreatedAt()
        );
    }

    public ActivationResponse toActivationResponse(LicenseActivation a) {
        return new ActivationResponse(
                a.getId(),
                a.getWorkspaceId(),
                a.getLicenseId(),
                a.getProductId(),
                a.getDeviceIdentifierHash(),
                a.getDeviceName(),
                a.getDevicePlatform(),
                a.getAppVersion(),
                a.getStatus(),
                a.getActivatedAt(),
                a.getLastValidatedAt(),
                a.getDeactivatedAt(),
                a.getDeactivatedBy()
        );
    }
}

