package com.rightfull.licensing.dto;

import jakarta.validation.constraints.Min;

import java.time.Instant;

public record RenewLicenseRequest(
        @Min(value = 1, message = "Extension days must be at least 1") Integer extensionDays,
        Instant newExpiresAt
) {}

