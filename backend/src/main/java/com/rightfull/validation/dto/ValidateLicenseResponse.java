package com.rightfull.validation.dto;

import java.time.Instant;
import java.util.Map;

public record ValidateLicenseResponse(
        ValidationResultStatus status,
        boolean valid,
        String token,
        Instant expiresAt,
        int offlineGracePeriodSeconds,
        int revalidationIntervalSeconds,
        Map<String, Object> entitlements,
        String signingKeyId,
        String message
) {
    public static ValidateLicenseResponse valid(
            String token,
            Instant expiresAt,
            int graceSeconds,
            int revalSeconds,
            Map<String, Object> entitlements,
            String signingKeyId
    ) {
        return new ValidateLicenseResponse(
                ValidationResultStatus.VALID,
                true,
                token,
                expiresAt,
                graceSeconds,
                revalSeconds,
                entitlements,
                signingKeyId,
                "License is valid and verified"
        );
    }

    public static ValidateLicenseResponse invalid(ValidationResultStatus status, String message) {
        return new ValidateLicenseResponse(
                status,
                false,
                null,
                null,
                0,
                0,
                null,
                null,
                message
        );
    }
}

