package com.rightfull.workspace.dto;

import com.rightfull.workspace.entity.WorkspaceRole;
import jakarta.validation.constraints.NotNull;

public record UpdateMemberRoleRequest(
        @NotNull(message = "Role is required")
        WorkspaceRole role
) {}

