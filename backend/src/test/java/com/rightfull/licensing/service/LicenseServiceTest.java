package com.rightfull.licensing.service;

import com.rightfull.activation.dto.ActivationResponse;
import com.rightfull.activation.entity.ActivationStatus;
import com.rightfull.activation.entity.LicenseActivation;
import com.rightfull.activation.repository.LicenseActivationRepository;
import com.rightfull.audit.WorkspaceAuditService;
import com.rightfull.common.exception.BusinessException;
import com.rightfull.common.exception.ResourceNotFoundException;
import com.rightfull.customer.entity.Customer;
import com.rightfull.customer.repository.CustomerRepository;
import com.rightfull.licensing.crypto.LicenseKeyGenerator;
import com.rightfull.licensing.dto.IssueLicenseRequest;
import com.rightfull.licensing.dto.IssueLicenseResponse;
import com.rightfull.licensing.dto.LicenseResponse;
import com.rightfull.licensing.dto.RenewLicenseRequest;
import com.rightfull.licensing.entity.License;
import com.rightfull.licensing.entity.LicenseStatus;
import com.rightfull.licensing.repository.LicenseRepository;
import com.rightfull.plan.entity.LicensePlan;
import com.rightfull.plan.repository.LicensePlanRepository;
import com.rightfull.plan.validator.EntitlementValidator;
import com.rightfull.product.entity.KeyStatus;
import com.rightfull.product.entity.Product;
import com.rightfull.product.entity.ProductSigningKey;
import com.rightfull.product.repository.ProductRepository;
import com.rightfull.product.repository.ProductSigningKeyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("License Service Unit Tests")
class LicenseServiceTest {

    @Mock
    private LicenseRepository licenseRepository;

    @Mock
    private LicenseActivationRepository activationRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private LicensePlanRepository planRepository;

    @Mock
    private ProductSigningKeyRepository signingKeyRepository;

    @Mock
    private WorkspaceAuditService auditService;

    private LicenseKeyGenerator keyGenerator;
    private EntitlementValidator entitlementValidator;
    private LicenseService licenseService;

    private UUID workspaceId;
    private UUID productId;
    private UUID actorId;
    private Product product;
    private ProductSigningKey signingKey;

    @BeforeEach
    void setUp() {
        keyGenerator = new LicenseKeyGenerator("super_secure_hmac_secret_for_licensing_tests_32_chars");
        keyGenerator.validateConfiguration();
        entitlementValidator = new EntitlementValidator();

        licenseService = new LicenseService(
                licenseRepository,
                activationRepository,
                productRepository,
                customerRepository,
                planRepository,
                signingKeyRepository,
                keyGenerator,
                entitlementValidator,
                auditService
        );

        workspaceId = UUID.randomUUID();
        productId = UUID.randomUUID();
        actorId = UUID.randomUUID();

        product = new Product(workspaceId, "VectorDB Engine", "vectordb-engine", "Core search engine", "v2.0", actorId);
        signingKey = new ProductSigningKey(
                workspaceId,
                productId,
                "kid_active_123",
                "base64_pub_key",
                new byte[]{1, 2, 3},
                new byte[]{4, 5, 6},
                "v1"
        );
    }

    @Test
    @DisplayName("Successfully issues a license with direct configuration and plaintext key")
    void shouldIssueLicenseWithDirectConfig() {
        when(productRepository.findByIdAndWorkspaceId(productId, workspaceId)).thenReturn(Optional.of(product));
        when(signingKeyRepository.findByProductIdAndStatus(productId, KeyStatus.ACTIVE)).thenReturn(Optional.of(signingKey));
        when(licenseRepository.save(any(License.class))).thenAnswer(inv -> inv.getArgument(0));

        IssueLicenseRequest request = new IssueLicenseRequest(
                null,
                null,
                365,
                3,
                10,
                25200,
                300,
                Map.of("max_nodes", 3, "telemetry_enabled", true),
                "ORDER-1001",
                null
        );

        IssueLicenseResponse response = licenseService.issueLicense(workspaceId, productId, request, actorId);

        assertThat(response).isNotNull();
        assertThat(response.licenseKey()).startsWith("RF-");
        assertThat(response.licenseKeyMasked()).contains("••••");
        assertThat(response.maxActivations()).isEqualTo(3);
        assertThat(response.seatLimit()).isEqualTo(10);
        assertThat(response.signingKeyId()).isEqualTo("kid_active_123");
        assertThat(response.status()).isEqualTo(LicenseStatus.ACTIVE);
        assertThat(response.isPerpetual()).isFalse();

        verify(auditService).recordEvent(any());
        verify(licenseRepository).save(any(License.class));
    }

    @Test
    @DisplayName("Successfully issues a perpetual license (durationDays = null)")
    void shouldIssuePerpetualLicense() {
        when(productRepository.findByIdAndWorkspaceId(productId, workspaceId)).thenReturn(Optional.of(product));
        when(signingKeyRepository.findByProductIdAndStatus(productId, KeyStatus.ACTIVE)).thenReturn(Optional.of(signingKey));
        when(licenseRepository.save(any(License.class))).thenAnswer(inv -> inv.getArgument(0));

        IssueLicenseRequest request = new IssueLicenseRequest(
                null,
                null,
                null, // Perpetual
                1,
                1,
                25200,
                300,
                Map.of(),
                null,
                null
        );

        IssueLicenseResponse response = licenseService.issueLicense(workspaceId, productId, request, actorId);

        assertThat(response.isPerpetual()).isTrue();
        assertThat(response.expiresAt()).isNull();
    }

    @Test
    @DisplayName("Successfully issues a license using a reusable configuration preset")
    void shouldIssueLicenseFromPreset() {
        UUID planId = UUID.randomUUID();
        LicensePlan plan = new LicensePlan(
                workspaceId,
                productId,
                "Enterprise Cluster Plan",
                "Cluster plan with 5 nodes",
                180,
                5,
                25,
                14400,
                120,
                Map.of("cluster_mode", true)
        );

        when(productRepository.findByIdAndWorkspaceId(productId, workspaceId)).thenReturn(Optional.of(product));
        when(planRepository.findByIdAndWorkspaceId(planId, workspaceId)).thenReturn(Optional.of(plan));
        when(signingKeyRepository.findByProductIdAndStatus(productId, KeyStatus.ACTIVE)).thenReturn(Optional.of(signingKey));
        when(licenseRepository.save(any(License.class))).thenAnswer(inv -> inv.getArgument(0));

        IssueLicenseRequest request = new IssueLicenseRequest(
                null,
                planId,
                null, null, null, null, null, null,
                "ORDER-554",
                null
        );

        IssueLicenseResponse response = licenseService.issueLicense(workspaceId, productId, request, actorId);

        assertThat(response.planId()).isEqualTo(plan.getId());
        assertThat(response.planVersion()).isEqualTo(plan.getVersion());
        assertThat(response.maxActivations()).isEqualTo(5);
        assertThat(response.seatLimit()).isEqualTo(25);
        assertThat(response.offlineGracePeriodSeconds()).isEqualTo(14400);
        assertThat(response.entitlements().get("cluster_mode")).isEqualTo(true);
    }

    @Test
    @DisplayName("Fails to issue license when product is archived")
    void shouldFailWhenProductIsArchived() {
        product.archive();
        when(productRepository.findByIdAndWorkspaceId(productId, workspaceId)).thenReturn(Optional.of(product));

        IssueLicenseRequest request = new IssueLicenseRequest(null, null, 365, 1, 1, 25200, 300, null, null, null);

        assertThatThrownBy(() -> licenseService.issueLicense(workspaceId, productId, request, actorId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Cannot issue licenses for an archived product");
    }

    @Test
    @DisplayName("Fails when product has no active signing key")
    void shouldFailWhenNoActiveSigningKey() {
        when(productRepository.findByIdAndWorkspaceId(productId, workspaceId)).thenReturn(Optional.of(product));
        when(signingKeyRepository.findByProductIdAndStatus(productId, KeyStatus.ACTIVE)).thenReturn(Optional.empty());

        IssueLicenseRequest request = new IssueLicenseRequest(null, null, 365, 1, 1, 25200, 300, null, null, null);

        assertThatThrownBy(() -> licenseService.issueLicense(workspaceId, productId, request, actorId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("no active Ed25519 signing key");
    }

    @Test
    @DisplayName("Successfully suspends and resumes an active license")
    void shouldSuspendAndResumeLicense() {
        UUID licenseId = UUID.randomUUID();
        License license = new License(
                workspaceId, productId, null, null, null,
                "hash", "RF-TEST", "W9A1",
                Instant.now(), Instant.now().plus(30, ChronoUnit.DAYS),
                1, 1, 25200, 300, Map.of(), "kid_123", null, null, actorId
        );

        when(licenseRepository.findByIdAndWorkspaceId(licenseId, workspaceId)).thenReturn(Optional.of(license));
        when(licenseRepository.save(any(License.class))).thenAnswer(inv -> inv.getArgument(0));

        LicenseResponse suspended = licenseService.suspendLicense(workspaceId, licenseId, "Billing dispute", actorId);
        assertThat(suspended.status()).isEqualTo(LicenseStatus.SUSPENDED);
        assertThat(suspended.suspendedReason()).isEqualTo("Billing dispute");

        LicenseResponse resumed = licenseService.resumeLicense(workspaceId, licenseId, actorId);
        assertThat(resumed.status()).isEqualTo(LicenseStatus.ACTIVE);
        assertThat(resumed.suspendedReason()).isNull();
    }

    @Test
    @DisplayName("Successfully revokes a license and prevents further activation")
    void shouldRevokeLicense() {
        UUID licenseId = UUID.randomUUID();
        License license = new License(
                workspaceId, productId, null, null, null,
                "hash", "RF-TEST", "W9A1",
                Instant.now(), Instant.now().plus(30, ChronoUnit.DAYS),
                1, 1, 25200, 300, Map.of(), "kid_123", null, null, actorId
        );

        when(licenseRepository.findByIdAndWorkspaceId(licenseId, workspaceId)).thenReturn(Optional.of(license));
        when(licenseRepository.save(any(License.class))).thenAnswer(inv -> inv.getArgument(0));

        LicenseResponse revoked = licenseService.revokeLicense(workspaceId, licenseId, "Terms violation", actorId);
        assertThat(revoked.status()).isEqualTo(LicenseStatus.REVOKED);
        assertThat(revoked.revokedReason()).isEqualTo("Terms violation");

        // Revoked licenses cannot be resumed
        assertThatThrownBy(() -> licenseService.resumeLicense(workspaceId, licenseId, actorId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot resume a revoked license");
    }

    @Test
    @DisplayName("Successfully renews a license by extending expiration days")
    void shouldRenewLicenseWithExtensionDays() {
        UUID licenseId = UUID.randomUUID();
        Instant originalExpires = Instant.now().plus(10, ChronoUnit.DAYS);
        License license = new License(
                workspaceId, productId, null, null, null,
                "hash", "RF-TEST", "W9A1",
                Instant.now(), originalExpires,
                1, 1, 25200, 300, Map.of(), "kid_123", null, null, actorId
        );

        when(licenseRepository.findByIdAndWorkspaceId(licenseId, workspaceId)).thenReturn(Optional.of(license));
        when(licenseRepository.save(any(License.class))).thenAnswer(inv -> inv.getArgument(0));

        RenewLicenseRequest renewReq = new RenewLicenseRequest(365, null);
        LicenseResponse renewed = licenseService.renewLicense(workspaceId, licenseId, renewReq, actorId);

        assertThat(renewed.expiresAt()).isAfter(originalExpires);
        assertThat(renewed.renewedAt()).isNotNull();
    }
}

