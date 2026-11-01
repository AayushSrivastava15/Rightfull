package com.rightfull.licensing.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.Map;
import java.util.UUID;

/**
 * Request payload for issuing a software product license.
 */
public record IssueLicenseRequest(
        UUID customerId,
        UUID planId,
        @Min(value = 0, message = "Duration days cannot be negative") Integer durationDays,
        @Min(value = 1, message = "Maximum activations must be at least 1") Integer maximumActivations,
        @Min(value = 1, message = "Seat limit must be at least 1") Integer seatLimit,
        @Min(value = 300, message = "Offline grace period must be at least 300 seconds (5 minutes)")
        @Max(value = 2592000, message = "Offline grace period cannot exceed 30 days (2592000 seconds)")
        Integer offlineGracePeriodSeconds,
        @Min(value = 60, message = "Revalidation interval must be at least 60 seconds (1 minute)")
        @Max(value = 86400, message = "Revalidation interval cannot exceed 24 hours (86400 seconds)")
        Integer revalidationIntervalSeconds,
        Map<String, Object> entitlements,
        @Size(max = 255, message = "External reference cannot exceed 255 characters") String externalReference,
        @Size(max = 128, message = "Idempotency key cannot exceed 128 characters") String idempotencyKey
) {}

