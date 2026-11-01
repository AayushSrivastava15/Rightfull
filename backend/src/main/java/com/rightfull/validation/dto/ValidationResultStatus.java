package com.rightfull.validation.dto;

/**
 * Result status codes returned by the public license validation API.
 */
public enum ValidationResultStatus {
    VALID,
    INVALID_KEY,
    PRODUCT_MISMATCH,
    EXPIRED,
    SUSPENDED,
    REVOKED,
    ACTIVATION_REQUIRED,
    ACTIVATION_LIMIT_REACHED,
    DEVICE_DEACTIVATED
}

