package com.rightfull.workspace.dto;

import com.rightfull.workspace.entity.WorkspaceInvitationStatus;
import com.rightfull.workspace.entity.WorkspaceRole;

import java.time.Instant;
import java.util.UUID;

public record WorkspaceInvitationResponse(
        UUID id,
        UUID workspaceId,
        String workspaceName,
        String email,
        WorkspaceRole role,
        WorkspaceInvitationStatus status,
        String invitedByEmail,
        Instant expiresAt,
        Instant createdAt,
        String invitationToken
) {}

