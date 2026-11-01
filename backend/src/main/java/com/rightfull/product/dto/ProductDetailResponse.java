package com.rightfull.product.dto;

import com.rightfull.product.entity.ProductStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Detailed product view including current active public key and historical rotation keys.
 */
public record ProductDetailResponse(
        UUID id,
        UUID workspaceId,
        String name,
        String slug,
        String description,
        String version,
        ProductStatus status,
        PublicKeyResponse activeKey,
        List<PublicKeyResponse> historicalKeys,
        Instant createdAt,
        Instant updatedAt,
        Instant archivedAt
) {}

