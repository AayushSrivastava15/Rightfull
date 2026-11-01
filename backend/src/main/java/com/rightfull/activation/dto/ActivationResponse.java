package com.rightfull.activation.dto;

import com.rightfull.activation.entity.ActivationStatus;

import java.time.Instant;
import java.util.UUID;

public record ActivationResponse(
        UUID id,
        UUID workspaceId,
        UUID licenseId,
        UUID productId,
        String deviceIdentifierHash,
        String deviceName,
        String devicePlatform,
        String appVersion,
        ActivationStatus status,
        Instant activatedAt,
        Instant lastValidatedAt,
        Instant deactivatedAt,
        String deactivatedBy
) {}

