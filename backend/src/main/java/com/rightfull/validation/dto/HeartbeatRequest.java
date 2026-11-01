package com.rightfull.validation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record HeartbeatRequest(
        @NotNull(message = "Product ID is required") UUID productId,
        @NotBlank(message = "License key is required") @Size(max = 128) String licenseKey,
        @NotBlank(message = "Device ID is required") @Size(max = 255) String deviceId,
        @Size(max = 64) String appVersion
) {}

