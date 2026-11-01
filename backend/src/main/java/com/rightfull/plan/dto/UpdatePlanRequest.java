package com.rightfull.plan.dto;

import jakarta.validation.constraints.*;
import java.util.Map;

/**
 * Request payload for updating an existing license plan.
 * Modifying entitlements or parameters increments the plan version.
 */
public record UpdatePlanRequest(
        @NotBlank(message = "Plan name is required")
        @Size(max = 255, message = "Plan name must not exceed 255 characters")
        String name,

        @Size(max = 2000, message = "Description must not exceed 2000 characters")
        String description,

        @Min(value = 0, message = "Duration days must be non-negative (0 or null = perpetual)")
        @Max(value = 36500, message = "Duration days must not exceed 100 years")
        Integer durationDays,

        @Min(value = 1, message = "Maximum activations must be at least 1")
        @Max(value = 100000, message = "Maximum activations cannot exceed 100,000")
        int maximumActivations,

        @Min(value = 1, message = "Seat limit must be at least 1")
        @Max(value = 100000, message = "Seat limit cannot exceed 100,000")
        int seatLimit,

        @Min(value = 300, message = "Offline grace period must be at least 300 seconds (5 minutes)")
        @Max(value = 2592000, message = "Offline grace period must not exceed 2,592,000 seconds (30 days)")
        int offlineGracePeriodSeconds,

        @Min(value = 60, message = "Revalidation interval must be at least 60 seconds (1 minute)")
        @Max(value = 86400, message = "Revalidation interval must not exceed 86,400 seconds (24 hours)")
        int revalidationIntervalSeconds,

        Map<String, Object> entitlements
) {}

