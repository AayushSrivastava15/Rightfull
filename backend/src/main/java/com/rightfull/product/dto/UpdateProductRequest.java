package com.rightfull.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload for updating an existing software product.
 * Note: Product slug and owning workspace are immutable to maintain integrity of cryptographic keys.
 */
public record UpdateProductRequest(
        @NotBlank(message = "Product name cannot be blank")
        @Size(min = 1, max = 255, message = "Product name must be between 1 and 255 characters")
        String name,

        @Size(max = 2000, message = "Description cannot exceed 2000 characters")
        String description,

        @Size(max = 50, message = "Version identifier cannot exceed 50 characters")
        String version
) {}

