package com.rightfull.product.dto;

import com.rightfull.product.entity.KeyStatus;

import java.time.Instant;

/**
 * Public signing key response safe for distribution to client-side verification engines.
 * NEVER includes encrypted or plaintext private key bytes.
 */
public record PublicKeyResponse(
        String keyId,
        String algorithm,
        String publicKey,
        KeyStatus status,
        Instant createdAt,
        Instant retiredAt
) {}

