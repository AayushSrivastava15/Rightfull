package com.rightfull.licensing.dto;

import com.rightfull.licensing.entity.LicenseStatus;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Issuance response containing the full plaintext license key displayed ONLY ONCE.
 */
public record IssueLicenseResponse(
        UUID id,
        UUID workspaceId,
        UUID productId,
        UUID customerId,
        UUID planId,
        Integer planVersion,
        String licenseKey, // Plaintext key - returned ONLY upon issuance
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
        Instant createdAt
) {}

