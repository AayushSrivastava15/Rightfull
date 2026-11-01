package com.rightfull.product.dto;

import com.rightfull.product.entity.ProductStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Summary product response returned by list and state-change endpoints.
 */
public record ProductResponse(
        UUID id,
        UUID workspaceId,
        String name,
        String slug,
        String description,
        String version,
        ProductStatus status,
        String activeKeyId,
        Instant createdAt,
        Instant updatedAt,
        Instant archivedAt
) {}

