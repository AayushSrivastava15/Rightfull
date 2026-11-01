package com.rightfull.activation.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Registered client device or hardware node bound to an issued software license.
 */
@Entity
@Table(
        name = "license_activations",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_activations_license_device", columnNames = {"license_id", "device_identifier_hash"}),
                @UniqueConstraint(name = "uq_activations_id_workspace", columnNames = {"id", "workspace_id"})
        }
)
public class LicenseActivation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id = UUID.randomUUID();

    @Column(name = "workspace_id", updatable = false, nullable = false)
    private UUID workspaceId;

    @Column(name = "license_id", updatable = false, nullable = false)
    private UUID licenseId;

    @Column(name = "product_id", updatable = false, nullable = false)
    private UUID productId;

    @Column(name = "device_identifier_hash", nullable = false, length = 64, updatable = false)
    private String deviceIdentifierHash;

    @Column(name = "device_name")
    private String deviceName;

    @Column(name = "device_platform", length = 100)
    private String devicePlatform;

    @Column(name = "app_version", length = 64)
    private String appVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ActivationStatus status = ActivationStatus.ACTIVE;

    @Column(name = "activated_at", updatable = false, nullable = false)
    private Instant activatedAt = Instant.now();

    @Column(name = "last_validated_at", nullable = false)
    private Instant lastValidatedAt = Instant.now();

    @Column(name = "deactivated_at")
    private Instant deactivatedAt;

    @Column(name = "deactivated_by", length = 32)
    private String deactivatedBy;

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected LicenseActivation() {
    }

    public LicenseActivation(
            UUID workspaceId,
            UUID licenseId,
            UUID productId,
            String deviceIdentifierHash,
            String deviceName,
            String devicePlatform,
            String appVersion
    ) {
        this.workspaceId = Objects.requireNonNull(workspaceId, "workspaceId cannot be null");
        this.licenseId = Objects.requireNonNull(licenseId, "licenseId cannot be null");
        this.productId = Objects.requireNonNull(productId, "productId cannot be null");
        this.deviceIdentifierHash = Objects.requireNonNull(deviceIdentifierHash, "deviceIdentifierHash cannot be null");
        this.deviceName = deviceName;
        this.devicePlatform = devicePlatform;
        this.appVersion = appVersion;
        this.status = ActivationStatus.ACTIVE;
        this.activatedAt = Instant.now();
        this.lastValidatedAt = Instant.now();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void updateHeartbeat(String appVersion) {
        this.lastValidatedAt = Instant.now();
        if (appVersion != null && !appVersion.isBlank()) {
            this.appVersion = appVersion;
        }
        this.updatedAt = Instant.now();
    }

    public void deactivate(String deactivatedBy) {
        this.status = ActivationStatus.DEACTIVATED;
        this.deactivatedAt = Instant.now();
        this.deactivatedBy = deactivatedBy;
        this.updatedAt = Instant.now();
    }

    public void reactivate(String deviceName, String devicePlatform, String appVersion) {
        this.status = ActivationStatus.ACTIVE;
        this.deactivatedAt = null;
        this.deactivatedBy = null;
        this.lastValidatedAt = Instant.now();
        if (deviceName != null) this.deviceName = deviceName;
        if (devicePlatform != null) this.devicePlatform = devicePlatform;
        if (appVersion != null) this.appVersion = appVersion;
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // --- Getters ---

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    public UUID getLicenseId() {
        return licenseId;
    }

    public UUID getProductId() {
        return productId;
    }

    public String getDeviceIdentifierHash() {
        return deviceIdentifierHash;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getDevicePlatform() {
        return devicePlatform;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public ActivationStatus getStatus() {
        return status;
    }

    public Instant getActivatedAt() {
        return activatedAt;
    }

    public Instant getLastValidatedAt() {
        return lastValidatedAt;
    }

    public Instant getDeactivatedAt() {
        return deactivatedAt;
    }

    public String getDeactivatedBy() {
        return deactivatedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
