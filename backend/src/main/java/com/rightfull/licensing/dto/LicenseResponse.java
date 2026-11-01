package com.rightfull.licensing.dto;

import com.rightfull.licensing.entity.LicenseStatus;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Standard license details DTO without raw plaintext key.
 */
public record LicenseResponse(
        UUID id,
        UUID workspaceId,
        UUID productId,
        UUID customerId,
        UUID planId,
        Integer planVersion,
        String licenseKeyPrefix,
        String licenseKeyMasked,
        String licenseKeyLastFour,
        LicenseStatus status,
        Instant startsAt,
        Instant expiresAt,
        boolean isPerpetual,
        int maxActivations,
        int currentActivations,
        int seatLimit,
        int offlineGracePeriodSeconds,
        int revalidationIntervalSeconds,
        Map<String, Object> entitlements,
        String signingKeyId,
        String externalReference,
        Instant suspendedAt,
        String suspendedReason,
        Instant revokedAt,
        String revokedReason,
        Instant renewedAt,
        Instant createdAt,
        Instant updatedAt
) {}

