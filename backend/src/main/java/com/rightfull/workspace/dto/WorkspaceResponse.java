package com.rightfull.workspace.dto;

import java.time.Instant;
import java.util.UUID;

public record WorkspaceResponse(
        UUID id,
        String name,
        String slug,
        String tier,
        String status,
        UUID ownerId,
        String currentUserRole,
        long memberCount,
        Instant createdAt
) {}

