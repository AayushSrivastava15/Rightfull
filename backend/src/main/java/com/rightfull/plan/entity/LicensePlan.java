package com.rightfull.plan.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Product licensing plan defining duration, seats, activations, grace periods, and structured entitlements.
 * Workspace-isolated with composite foreign key constraint enforcing product/workspace consistency.
 */
@Entity
@Table(
        name = "license_plans",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_plans_product_name", columnNames = {"product_id", "name"}),
                @UniqueConstraint(name = "uq_plans_id_workspace", columnNames = {"id", "workspace_id"})
        }
)
public class LicensePlan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "workspace_id", updatable = false, nullable = false)
    private UUID workspaceId;

    @Column(name = "product_id", updatable = false, nullable = false)
    private UUID productId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "duration_days")
    private Integer durationDays = 365;

    @Column(name = "maximum_activations", nullable = false)
    private int maximumActivations = 1;

    @Column(name = "seat_limit", nullable = false)
    private int seatLimit = 1;

    @Column(name = "offline_grace_period_seconds", nullable = false)
    private int offlineGracePeriodSeconds = 25200; // 7 hours

    @Column(name = "revalidation_interval_seconds", nullable = false)
    private int revalidationIntervalSeconds = 300; // 5 minutes

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "entitlements", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> entitlements = new HashMap<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private PlanStatus status = PlanStatus.ACTIVE;

    @Column(name = "version", nullable = false)
    private int version = 1;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "archived_at")
    private Instant archivedAt;

    public LicensePlan() {
    }

    public LicensePlan(UUID workspaceId, UUID productId, String name, String description,
                       Integer durationDays, int maximumActivations, int seatLimit,
                       int offlineGracePeriodSeconds, int revalidationIntervalSeconds,
                       Map<String, Object> entitlements) {
        this.workspaceId = workspaceId;
        this.productId = productId;
        this.name = name;
        this.description = description;
        this.durationDays = durationDays;
        this.maximumActivations = maximumActivations;
        this.seatLimit = seatLimit;
        this.offlineGracePeriodSeconds = offlineGracePeriodSeconds;
        this.revalidationIntervalSeconds = revalidationIntervalSeconds;
        this.entitlements = entitlements != null ? new HashMap<>(entitlements) : new HashMap<>();
        this.status = PlanStatus.ACTIVE;
        this.version = 1;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
        if (status == null) {
            status = PlanStatus.ACTIVE;
        }
        if (version < 1) {
            version = 1;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public void archive() {
        this.status = PlanStatus.ARCHIVED;
        this.archivedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void incrementVersion() {
        this.version++;
        this.updatedAt = Instant.now();
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(UUID workspaceId) {
        this.workspaceId = workspaceId;
    }

    public UUID getProductId() {
        return productId;
    }

    public void setProductId(UUID productId) {
        this.productId = productId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getDurationDays() {
        return durationDays;
    }

    public void setDurationDays(Integer durationDays) {
        this.durationDays = durationDays;
    }

    public int getMaximumActivations() {
        return maximumActivations;
    }

    public void setMaximumActivations(int maximumActivations) {
        this.maximumActivations = maximumActivations;
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
        this.entitlements = entitlements != null ? new HashMap<>(entitlements) : new HashMap<>();
    }

    public PlanStatus getStatus() {
        return status;
    }

    public void setStatus(PlanStatus status) {
        this.status = status;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getArchivedAt() {
        return archivedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LicensePlan that = (LicensePlan) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}

