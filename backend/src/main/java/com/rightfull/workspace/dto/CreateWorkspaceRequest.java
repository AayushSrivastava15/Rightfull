package com.rightfull.workspace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateWorkspaceRequest(
        @NotBlank(message = "Workspace name is required")
        @Size(min = 2, max = 128, message = "Workspace name must be between 2 and 128 characters")
        String name,

        @Pattern(regexp = "^[a-z0-9-]+$", message = "Slug must contain only lowercase alphanumeric characters and hyphens")
        @Size(min = 3, max = 64, message = "Slug must be between 3 and 64 characters")
        String slug
) {}

