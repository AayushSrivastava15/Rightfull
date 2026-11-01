package com.rightfull.plan.dto;

import com.rightfull.plan.entity.LicensePlanVersion;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Historical version record of a license configuration preset.
 * Allows retrieving any past entitlement definition and parameter set.
 */
public record PlanVersionResponse(
        UUID id,
        UUID planId,
        UUID productId,
        UUID workspaceId,
        int version,
        String name,
        String description,
        Integer durationDays,
        int maximumActivations,
        int seatLimit,
        int offlineGracePeriodSeconds,
        int revalidationIntervalSeconds,
        Map<String, Object> entitlements,
        Instant createdAt,
        UUID createdBy
) {
    public static PlanVersionResponse fromEntity(LicensePlanVersion entity) {
        return new PlanVersionResponse(
                entity.getId(),
                entity.getPlanId(),
                entity.getProductId(),
                entity.getWorkspaceId(),
                entity.getVersion(),
                entity.getName(),
                entity.getDescription(),
                entity.getDurationDays(),
                entity.getMaximumActivations(),
                entity.getSeatLimit(),
                entity.getOfflineGracePeriodSeconds(),
                entity.getRevalidationIntervalSeconds(),
                entity.getEntitlements(),
                entity.getCreatedAt(),
                entity.getCreatedBy()
        );
    }
}

