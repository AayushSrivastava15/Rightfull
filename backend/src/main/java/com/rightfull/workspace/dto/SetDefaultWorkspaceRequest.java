package com.rightfull.workspace.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record SetDefaultWorkspaceRequest(
        @NotNull(message = "Workspace ID is required")
        UUID workspaceId
) {}

