package com.rightfull.plan.dto;

import com.rightfull.plan.entity.LicensePlan;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Immutable entitlement and parameter snapshot captured at license issuance time.
 * Guarantees that future plan edits do not silently mutate active issued licenses.
 */
public record PlanSnapshot(
        UUID planId,
        UUID productId,
        UUID workspaceId,
        int planVersion,
        String planName,
        Integer durationDays,
        int maximumActivations,
        int seatLimit,
        int offlineGracePeriodSeconds,
        int revalidationIntervalSeconds,
        Map<String, Object> entitlements,
        Instant snapshotTimestamp
) {
    public PlanSnapshot {
        entitlements = entitlements != null
                ? Collections.unmodifiableMap(new HashMap<>(entitlements))
                : Collections.emptyMap();
    }

    public static PlanSnapshot fromPlan(LicensePlan plan) {
        return new PlanSnapshot(
                plan.getId(),
                plan.getProductId(),
                plan.getWorkspaceId(),
                plan.getVersion(),
                plan.getName(),
                plan.getDurationDays(),
                plan.getMaximumActivations(),
                plan.getSeatLimit(),
                plan.getOfflineGracePeriodSeconds(),
                plan.getRevalidationIntervalSeconds(),
                plan.getEntitlements(),
                Instant.now()
        );
    }
}

