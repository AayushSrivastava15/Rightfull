package com.rightfull.identity.dto;

import com.rightfull.workspace.entity.WorkspaceRole;
import com.rightfull.workspace.entity.WorkspaceTier;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Verified user profile response returned by GET /api/v1/users/me.
 */
public record UserProfileResponse(
        UUID id,
        UUID supabaseUserId,
        String email,
        String fullName,
        String avatarUrl,
        boolean active,
        Instant createdAt,
        WorkspaceSummaryDto defaultWorkspace,
        List<WorkspaceMembershipDto> memberships
) {
    public record WorkspaceSummaryDto(
            UUID id,
            String name,
            String slug,
            WorkspaceTier tier,
            WorkspaceRole role
    ) {}

    public record WorkspaceMembershipDto(
            UUID workspaceId,
            String workspaceName,
            String workspaceSlug,
            WorkspaceTier workspaceTier,
            WorkspaceRole role,
            Instant joinedAt
    ) {}
}

