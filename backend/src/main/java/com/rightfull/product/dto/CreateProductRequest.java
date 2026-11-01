package com.rightfull.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Payload for registering a new software product within a workspace.
 */
public record CreateProductRequest(
        @NotBlank(message = "Product name cannot be blank")
        @Size(min = 1, max = 255, message = "Product name must be between 1 and 255 characters")
        String name,

        @Pattern(
                regexp = "^[a-z0-9]+(-[a-z0-9]+)*$",
                message = "Product slug must contain only lowercase alphanumeric characters and single hyphens"
        )
        @Size(min = 2, max = 100, message = "Product slug must be between 2 and 100 characters")
        String slug,

        @Size(max = 2000, message = "Description cannot exceed 2000 characters")
        String description,

        @Size(max = 50, message = "Version identifier cannot exceed 50 characters")
        String version
) {}

