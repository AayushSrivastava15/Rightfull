package com.rightfull.validation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rightfull.activation.entity.ActivationStatus;
import com.rightfull.activation.entity.LicenseActivation;
import com.rightfull.activation.repository.LicenseActivationRepository;
import com.rightfull.common.util.CryptoUtils;
import com.rightfull.licensing.crypto.EntitlementTokenSigner;
import com.rightfull.licensing.crypto.LicenseKeyGenerator;
import com.rightfull.licensing.entity.License;
import com.rightfull.licensing.repository.LicenseRepository;
import com.rightfull.product.crypto.Ed25519KeyService;
import com.rightfull.product.entity.Product;
import com.rightfull.product.repository.ProductRepository;
import com.rightfull.product.service.ProductSigningKeyService;
import com.rightfull.security.tenant.TenantContextManager;
import com.rightfull.validation.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.KeyPair;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("License Validation & Device Activation Unit Tests")
class LicenseValidationServiceTest {

    @Mock
    private LicenseRepository licenseRepository;

    @Mock
    private LicenseActivationRepository activationRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductSigningKeyService signingKeyService;

    @Mock
    private TenantContextManager tenantContextManager;

    private LicenseKeyGenerator keyGenerator;
    private EntitlementTokenSigner tokenSigner;
    private Ed25519KeyService ed25519KeyService;
    private KeyPair keyPair;
    private LicenseValidationService validationService;

    private UUID workspaceId;
    private UUID productId;
    private Product product;
    private License license;
    private String rawKey;
    private String keyHash;

    @BeforeEach
    void setUp() {
        keyGenerator = new LicenseKeyGenerator("super_secure_hmac_secret_for_licensing_tests_32_chars");
        keyGenerator.validateConfiguration();

        ed25519KeyService = new Ed25519KeyService();
        keyPair = ed25519KeyService.generateKeyPair();
        tokenSigner = new EntitlementTokenSigner(ed25519KeyService, new ObjectMapper());

        validationService = new LicenseValidationService(
                licenseRepository,
                activationRepository,
                productRepository,
                signingKeyService,
                keyGenerator,
                tokenSigner,
                tenantContextManager
        );

        workspaceId = UUID.randomUUID();
        productId = UUID.randomUUID();

        product = new Product(workspaceId, "Database Engine", "db-engine", "Core engine", "v1.0", null);

        LicenseKeyGenerator.GeneratedLicenseKey gen = keyGenerator.generate();
        rawKey = gen.rawKey();
        keyHash = gen.keyHash();

        license = new License(
                workspaceId,
                productId,
                null,
                null,
                null,
                keyHash,
                gen.keyPrefix(),
                gen.lastFour(),
                Instant.now(),
                Instant.now().plus(365, ChronoUnit.DAYS),
                2, // Max 2 activations
                5,
                25200,
                300,
                Map.of("cluster_nodes", 2, "pro_features", true),
                "kid_active_1",
                null,
                null,
                null
        );
    }

    @Test
    @DisplayName("Validates active license and returns verified JWS EdDSA signed token")
    void shouldValidateActiveLicense() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(licenseRepository.findByProductIdAndLicenseKeyHash(productId, keyHash)).thenReturn(Optional.of(license));
        when(signingKeyService.getActivePrivateKey(productId)).thenReturn(keyPair.getPrivate());

        ValidateLicenseRequest request = new ValidateLicenseRequest(productId, rawKey, null, "1.0.0", "nonce-1");
        ValidateLicenseResponse response = validationService.validate(request);

        assertThat(response.valid()).isTrue();
        assertThat(response.status()).isEqualTo(ValidationResultStatus.VALID);
        assertThat(response.token()).isNotNull();

        // Verify the returned cryptographic token signature
        boolean sigValid = ed25519KeyService.verify(
                (response.token().split("\\.")[0] + "." + response.token().split("\\.")[1]).getBytes(),
                java.util.Base64.getUrlDecoder().decode(response.token().split("\\.")[2]),
                keyPair.getPublic()
        );
        assertThat(sigValid).isTrue();
    }

    @Test
    @DisplayName("Rejects invalid / unknown license key")
    void shouldRejectInvalidKey() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(licenseRepository.findByProductIdAndLicenseKeyHash(eq(productId), any())).thenReturn(Optional.empty());

        ValidateLicenseRequest request = new ValidateLicenseRequest(productId, "RF-FAKE-0000-0000-0000-0000", null, null, null);
        ValidateLicenseResponse response = validationService.validate(request);

        assertThat(response.valid()).isFalse();
        assertThat(response.status()).isEqualTo(ValidationResultStatus.INVALID_KEY);
    }

    @Test
    @DisplayName("Rejects revoked license with REVOKED status")
    void shouldRejectRevokedLicense() {
        license.revoke("Compromised");
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(licenseRepository.findByProductIdAndLicenseKeyHash(productId, keyHash)).thenReturn(Optional.of(license));

        ValidateLicenseRequest request = new ValidateLicenseRequest(productId, rawKey, null, null, null);
        ValidateLicenseResponse response = validationService.validate(request);

        assertThat(response.valid()).isFalse();
        assertThat(response.status()).isEqualTo(ValidationResultStatus.REVOKED);
    }

    @Test
    @DisplayName("Rejects suspended license with SUSPENDED status")
    void shouldRejectSuspendedLicense() {
        license.suspend("Pending invoice payment");
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(licenseRepository.findByProductIdAndLicenseKeyHash(productId, keyHash)).thenReturn(Optional.of(license));

        ValidateLicenseRequest request = new ValidateLicenseRequest(productId, rawKey, null, null, null);
        ValidateLicenseResponse response = validationService.validate(request);

        assertThat(response.valid()).isFalse();
        assertThat(response.status()).isEqualTo(ValidationResultStatus.SUSPENDED);
    }

    @Test
    @DisplayName("Rejects expired license with EXPIRED status")
    void shouldRejectExpiredLicense() {
        license.setExpiresAt(Instant.now().minus(1, ChronoUnit.DAYS));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(licenseRepository.findByProductIdAndLicenseKeyHash(productId, keyHash)).thenReturn(Optional.of(license));

        ValidateLicenseRequest request = new ValidateLicenseRequest(productId, rawKey, null, null, null);
        ValidateLicenseResponse response = validationService.validate(request);

        assertThat(response.valid()).isFalse();
        assertThat(response.status()).isEqualTo(ValidationResultStatus.EXPIRED);
    }

    @Test
    @DisplayName("Successfully activates new device within limit")
    void shouldActivateDeviceWithinLimit() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(licenseRepository.findByProductIdAndLicenseKeyHash(productId, keyHash)).thenReturn(Optional.of(license));
        when(licenseRepository.findByIdWithPessimisticLock(license.getId())).thenReturn(Optional.of(license));
        when(activationRepository.findByLicenseIdAndDeviceIdentifierHash(eq(license.getId()), any())).thenReturn(Optional.empty());
        when(activationRepository.save(any(LicenseActivation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(signingKeyService.getActivePrivateKey(productId)).thenReturn(keyPair.getPrivate());

        ActivateDeviceRequest request = new ActivateDeviceRequest(
                productId,
                rawKey,
                "client-hardware-uuid-9999",
                "Prod Server 1",
                "linux-x64",
                "v2.1"
        );

        ActivateDeviceResponse response = validationService.activateDevice(request);

        assertThat(response.valid()).isTrue();
        assertThat(response.status()).isEqualTo(ValidationResultStatus.VALID);
        assertThat(response.currentActivations()).isEqualTo(1);
        assertThat(response.maxActivations()).isEqualTo(2);
        assertThat(license.getCurrentActivations()).isEqualTo(1);

        verify(activationRepository).save(any(LicenseActivation.class));
        verify(licenseRepository).save(license);
    }

    @Test
    @DisplayName("Rejects device activation when activation limit is reached")
    void shouldRejectActivationWhenLimitReached() {
        license.incrementActivations();
        license.incrementActivations(); // current = 2, max = 2

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(licenseRepository.findByProductIdAndLicenseKeyHash(productId, keyHash)).thenReturn(Optional.of(license));
        when(licenseRepository.findByIdWithPessimisticLock(license.getId())).thenReturn(Optional.of(license));
        when(activationRepository.findByLicenseIdAndDeviceIdentifierHash(eq(license.getId()), any())).thenReturn(Optional.empty());

        ActivateDeviceRequest request = new ActivateDeviceRequest(
                productId,
                rawKey,
                "device-node-3",
                "Prod Server 3",
                "linux-x64",
                "v2.1"
        );

        ActivateDeviceResponse response = validationService.activateDevice(request);

        assertThat(response.valid()).isFalse();
        assertThat(response.status()).isEqualTo(ValidationResultStatus.ACTIVATION_LIMIT_REACHED);
        assertThat(license.getCurrentActivations()).isEqualTo(2);

        verify(activationRepository, never()).save(any(LicenseActivation.class));
    }

    @Test
    @DisplayName("Successfully deactivates active device and decrements license count")
    void shouldDeactivateDevice() {
        license.incrementActivations(); // current = 1
        String deviceId = "device-node-1";
        String deviceHash = CryptoUtils.sha256Hex(deviceId);

        LicenseActivation activation = new LicenseActivation(
                workspaceId, license.getId(), productId, deviceHash, "Server 1", "linux-x64", "v1.0"
        );

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(licenseRepository.findByProductIdAndLicenseKeyHash(productId, keyHash)).thenReturn(Optional.of(license));
        when(licenseRepository.findByIdWithPessimisticLock(license.getId())).thenReturn(Optional.of(license));
        when(activationRepository.findByLicenseIdAndDeviceIdentifierHash(license.getId(), deviceHash)).thenReturn(Optional.of(activation));

        DeactivateDeviceRequest request = new DeactivateDeviceRequest(productId, rawKey, deviceId);
        boolean deactivated = validationService.deactivateDevice(request);

        assertThat(deactivated).isTrue();
        assertThat(activation.getStatus()).isEqualTo(ActivationStatus.DEACTIVATED);
        assertThat(license.getCurrentActivations()).isEqualTo(0);

        verify(activationRepository).save(activation);
        verify(licenseRepository).save(license);
    }
}

