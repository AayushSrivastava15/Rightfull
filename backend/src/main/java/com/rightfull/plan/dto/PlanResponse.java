package com.rightfull.plan.dto;

import com.rightfull.plan.entity.LicensePlan;
import com.rightfull.plan.entity.PlanStatus;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Verified license plan representation returned by REST endpoints.
 */
public record PlanResponse(
        UUID id,
        UUID workspaceId,
        UUID productId,
        String name,
        String description,
        Integer durationDays,
        int maximumActivations,
        int seatLimit,
        int offlineGracePeriodSeconds,
        int revalidationIntervalSeconds,
        Map<String, Object> entitlements,
        PlanStatus status,
        int version,
        Instant createdAt,
        Instant updatedAt,
        Instant archivedAt
) {
    public static PlanResponse fromEntity(LicensePlan plan) {
        return new PlanResponse(
                plan.getId(),
                plan.getWorkspaceId(),
                plan.getProductId(),
                plan.getName(),
                plan.getDescription(),
                plan.getDurationDays(),
                plan.getMaximumActivations(),
                plan.getSeatLimit(),
                plan.getOfflineGracePeriodSeconds(),
                plan.getRevalidationIntervalSeconds(),
                plan.getEntitlements(),
                plan.getStatus(),
                plan.getVersion(),
                plan.getCreatedAt(),
                plan.getUpdatedAt(),
                plan.getArchivedAt()
        );
    }
}

