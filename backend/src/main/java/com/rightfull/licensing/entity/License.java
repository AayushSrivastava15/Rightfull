package com.rightfull.licensing.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Issued software license representing an active or historical grant of entitlements,
 * device activation restrictions, and cryptographic signing metadata.
 */
@Entity
@Table(
        name = "licenses",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_licenses_key_hash", columnNames = {"license_key_hash"}),
                @UniqueConstraint(name = "uq_licenses_id_workspace", columnNames = {"id", "workspace_id"})
        }
)
public class License {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id = UUID.randomUUID();

    @Column(name = "workspace_id", updatable = false, nullable = false)
    private UUID workspaceId;

    @Column(name = "product_id", updatable = false, nullable = false)
    private UUID productId;

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "plan_id")
    private UUID planId;

    @Column(name = "plan_version")
    private Integer planVersion;

    @Column(name = "license_key_hash", nullable = false, length = 64, updatable = false)
    private String licenseKeyHash;

    @Column(name = "license_key_prefix", nullable = false, length = 16, updatable = false)
    private String licenseKeyPrefix;

    @Column(name = "license_key_last_four", nullable = false, length = 8, updatable = false)
    private String licenseKeyLastFour;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private LicenseStatus status = LicenseStatus.ACTIVE;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt = Instant.now();

    @Column(name = "expires_at")
    private Instant expiresAt; // Nullable for perpetual licenses

    @Column(name = "max_activations", nullable = false)
    private int maxActivations = 1;

    @Column(name = "current_activations", nullable = false)
    private int currentActivations = 0;

    @Column(name = "seat_limit", nullable = false)
    private int seatLimit = 1;

    @Column(name = "offline_grace_period_seconds", nullable = false)
    private int offlineGracePeriodSeconds = 25200; // 7 hours

    @Column(name = "revalidation_interval_seconds", nullable = false)
    private int revalidationIntervalSeconds = 300; // 5 minutes

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "entitlements", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> entitlements = new LinkedHashMap<>();

    @Column(name = "signing_key_id", nullable = false, length = 64)
    private String signingKeyId;

    @Column(name = "external_reference")
    private String externalReference;

    @Column(name = "idempotency_key", length = 128)
    private String idempotencyKey;

    @Column(name = "suspended_at")
    private Instant suspendedAt;

    @Column(name = "suspended_reason")
    private String suspendedReason;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "revoked_reason")
    private String revokedReason;

    @Column(name = "renewed_at")
    private Instant renewedAt;

    @Version
    @Column(name = "version", nullable = false)
    private int version = 0;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected License() {
    }

    public License(
            UUID workspaceId,
            UUID productId,
            UUID customerId,
            UUID planId,
            Integer planVersion,
            String licenseKeyHash,
            String licenseKeyPrefix,
            String licenseKeyLastFour,
            Instant startsAt,
            Instant expiresAt,
            int maxActivations,
            int seatLimit,
            int offlineGracePeriodSeconds,
            int revalidationIntervalSeconds,
            Map<String, Object> entitlements,
            String signingKeyId,
            String externalReference,
            String idempotencyKey,
            UUID createdBy
    ) {
        this.workspaceId = Objects.requireNonNull(workspaceId, "workspaceId cannot be null");
        this.productId = Objects.requireNonNull(productId, "productId cannot be null");
        this.customerId = customerId;
        this.planId = planId;
        this.planVersion = planVersion;
        this.licenseKeyHash = Objects.requireNonNull(licenseKeyHash, "licenseKeyHash cannot be null");
        this.licenseKeyPrefix = Objects.requireNonNull(licenseKeyPrefix, "licenseKeyPrefix cannot be null");
        this.licenseKeyLastFour = Objects.requireNonNull(licenseKeyLastFour, "licenseKeyLastFour cannot be null");
        this.startsAt = (startsAt != null) ? startsAt : Instant.now();
        this.expiresAt = expiresAt;
        this.maxActivations = Math.max(1, maxActivations);
        this.currentActivations = 0;
        this.seatLimit = Math.max(1, seatLimit);
        this.offlineGracePeriodSeconds = offlineGracePeriodSeconds;
        this.revalidationIntervalSeconds = revalidationIntervalSeconds;
        this.entitlements = (entitlements != null) ? new LinkedHashMap<>(entitlements) : new LinkedHashMap<>();
        this.signingKeyId = Objects.requireNonNull(signingKeyId, "signingKeyId cannot be null");
        this.externalReference = externalReference;
        this.idempotencyKey = idempotencyKey;
        this.createdBy = createdBy;
        this.status = LicenseStatus.ACTIVE;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // --- Domain Lifecycle Methods ---

    public boolean isExpired() {
        if (expiresAt == null) {
            return false; // Perpetual license never expires
        }
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isPerpetual() {
        return expiresAt == null;
    }

    public void suspend(String reason) {
        if (this.status == LicenseStatus.REVOKED) {
            throw new IllegalStateException("Cannot suspend a revoked license");
        }
        this.status = LicenseStatus.SUSPENDED;
        this.suspendedAt = Instant.now();
        this.suspendedReason = reason;
        this.updatedAt = Instant.now();
    }

    public void resume() {
        if (this.status == LicenseStatus.REVOKED) {
            throw new IllegalStateException("Cannot resume a revoked license");
        }
        if (isExpired()) {
            this.status = LicenseStatus.EXPIRED;
            throw new IllegalStateException("Cannot resume an expired license without renewal");
        }
        this.status = LicenseStatus.ACTIVE;
        this.suspendedAt = null;
        this.suspendedReason = null;
        this.updatedAt = Instant.now();
    }

    public void revoke(String reason) {
        this.status = LicenseStatus.REVOKED;
        this.revokedAt = Instant.now();
        this.revokedReason = reason;
        this.updatedAt = Instant.now();
    }

    public void renew(Instant newExpiresAt) {
        if (this.status == LicenseStatus.REVOKED) {
            throw new IllegalStateException("Cannot renew a revoked license");
        }
        this.expiresAt = newExpiresAt;
        this.renewedAt = Instant.now();
        if (this.status == LicenseStatus.EXPIRED) {
            this.status = LicenseStatus.ACTIVE;
        }
        this.updatedAt = Instant.now();
    }

    public void incrementActivations() {
        if (this.currentActivations >= this.maxActivations) {
            throw new IllegalStateException("Activation limit reached: " + this.maxActivations);
        }
        this.currentActivations++;
        this.updatedAt = Instant.now();
    }

    public void decrementActivations() {
        if (this.currentActivations > 0) {
            this.currentActivations--;
            this.updatedAt = Instant.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // --- Getters & Setters ---

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    public UUID getProductId() {
        return productId;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public UUID getPlanId() {
        return planId;
    }

    public Integer getPlanVersion() {
        return planVersion;
    }

    public String getLicenseKeyHash() {
        return licenseKeyHash;
    }

    public String getLicenseKeyPrefix() {
        return licenseKeyPrefix;
    }

    public String getLicenseKeyLastFour() {
        return licenseKeyLastFour;
    }

    public LicenseStatus getStatus() {
        if (status == LicenseStatus.ACTIVE && isExpired()) {
            return LicenseStatus.EXPIRED;
        }
        return status;
    }

    public void setStatus(LicenseStatus status) {
        this.status = status;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public int getMaxActivations() {
        return maxActivations;
    }

    public void setMaxActivations(int maxActivations) {
        this.maxActivations = maxActivations;
    }

    public int getCurrentActivations() {
        return currentActivations;
    }

    public int getSeatLimit() {
        return seatLimit;
    }

    public void setSeatLimit(int seatLimit) {
        this.seatLimit = seatLimit;
    }

    public int getOfflineGracePeriodSeconds() {
        return offlineGracePeriodSeconds;
    }

    public void setOfflineGracePeriodSeconds(int offlineGracePeriodSeconds) {
        this.offlineGracePeriodSeconds = offlineGracePeriodSeconds;
    }

    public int getRevalidationIntervalSeconds() {
        return revalidationIntervalSeconds;
    }

    public void setRevalidationIntervalSeconds(int revalidationIntervalSeconds) {
        this.revalidationIntervalSeconds = revalidationIntervalSeconds;
    }

    public Map<String, Object> getEntitlements() {
        return entitlements;
    }

    public void setEntitlements(Map<String, Object> entitlements) {
        this.entitlements = entitlements;
    }

    public String getSigningKeyId() {
        return signingKeyId;
    }

    public void setSigningKeyId(String signingKeyId) {
        this.signingKeyId = signingKeyId;
    }

    public String getExternalReference() {
        return externalReference;
    }

    public void setExternalReference(String externalReference) {
        this.externalReference = externalReference;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Instant getSuspendedAt() {
        return suspendedAt;
    }

    public String getSuspendedReason() {
        return suspendedReason;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public String getRevokedReason() {
        return revokedReason;
    }

    public Instant getRenewedAt() {
        return renewedAt;
    }

    public int getVersion() {
        return version;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
