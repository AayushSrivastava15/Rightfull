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
 * Immutable historical version snapshot for a software license configuration preset.
 * Guarantees that past entitlement definitions and restriction parameters remain recoverable.
 */
@Entity
@Table(
        name = "license_plan_versions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_plan_versions_plan_ver", columnNames = {"plan_id", "version"})
        }
)
public class LicensePlanVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "plan_id", updatable = false, nullable = false)
    private UUID planId;

    @Column(name = "workspace_id", updatable = false, nullable = false)
    private UUID workspaceId;

    @Column(name = "product_id", updatable = false, nullable = false)
    private UUID productId;

    @Column(name = "version", updatable = false, nullable = false)
    private int version;

    @Column(name = "name", updatable = false, nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT", updatable = false)
    private String description;

    @Column(name = "duration_days", updatable = false)
    private Integer durationDays;

    @Column(name = "maximum_activations", updatable = false, nullable = false)
    private int maximumActivations;

    @Column(name = "seat_limit", updatable = false, nullable = false)
    private int seatLimit;

    @Column(name = "offline_grace_period_seconds", updatable = false, nullable = false)
    private int offlineGracePeriodSeconds;

    @Column(name = "revalidation_interval_seconds", updatable = false, nullable = false)
    private int revalidationIntervalSeconds;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "entitlements", columnDefinition = "jsonb", updatable = false, nullable = false)
    private Map<String, Object> entitlements = new HashMap<>();

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    protected LicensePlanVersion() {}

    public LicensePlanVersion(LicensePlan plan, UUID actorId) {
        this.planId = plan.getId();
        this.workspaceId = plan.getWorkspaceId();
        this.productId = plan.getProductId();
        this.version = plan.getVersion();
        this.name = plan.getName();
        this.description = plan.getDescription();
        this.durationDays = plan.getDurationDays();
        this.maximumActivations = plan.getMaximumActivations();
        this.seatLimit = plan.getSeatLimit();
        this.offlineGracePeriodSeconds = plan.getOfflineGracePeriodSeconds();
        this.revalidationIntervalSeconds = plan.getRevalidationIntervalSeconds();
        this.entitlements = plan.getEntitlements() != null ? new HashMap<>(plan.getEntitlements()) : new HashMap<>();
        this.createdAt = Instant.now();
        this.createdBy = actorId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPlanId() {
        return planId;
    }

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    public UUID getProductId() {
        return productId;
    }

    public int getVersion() {
        return version;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Integer getDurationDays() {
        return durationDays;
    }

    public int getMaximumActivations() {
        return maximumActivations;
    }

    public int getSeatLimit() {
        return seatLimit;
    }

    public int getOfflineGracePeriodSeconds() {
        return offlineGracePeriodSeconds;
    }

    public int getRevalidationIntervalSeconds() {
        return revalidationIntervalSeconds;
    }

    public Map<String, Object> getEntitlements() {
        return entitlements;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LicensePlanVersion that)) return false;
        return version == that.version && Objects.equals(planId, that.planId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(planId, version);
    }
}

