package com.rightfull.workspace.dto;

import com.rightfull.workspace.entity.WorkspaceRole;

import java.time.Instant;
import java.util.UUID;

public record WorkspaceMemberResponse(
        UUID id,
        UUID userId,
        String email,
        String fullName,
        String avatarUrl,
        WorkspaceRole role,
        Instant joinedAt
) {}

