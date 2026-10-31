package com.rightfull.workspace.dto;

import com.rightfull.workspace.entity.WorkspaceRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateInvitationRequest(
        @NotBlank(message = "Invitee email is required")
        @Email(message = "Valid email address is required")
        String email,

        @NotNull(message = "Invitation role is required")
        WorkspaceRole role
) {}

