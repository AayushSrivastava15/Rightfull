package com.rightfull.validation.dto;

import java.util.UUID;

public record ActivateDeviceResponse(
        ValidationResultStatus status,
        boolean valid,
        String token,
        UUID activationId,
        int currentActivations,
        int maxActivations,
        String message
) {
    public static ActivateDeviceResponse success(String token, UUID activationId, int current, int max) {
        return new ActivateDeviceResponse(
                ValidationResultStatus.VALID,
                true,
                token,
                activationId,
                current,
                max,
                "Device activated successfully"
        );
    }

    public static ActivateDeviceResponse error(ValidationResultStatus status, String message) {
        return new ActivateDeviceResponse(
                status,
                false,
                null,
                null,
                0,
                0,
                message
        );
    }
}

