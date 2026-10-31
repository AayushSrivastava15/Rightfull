package com.rightfull.workspace.dto;

import java.time.Instant;
import java.util.UUID;

public record WorkspaceSummaryResponse(
        UUID id,
        String name,
        String slug,
        String tier,
        String role,
        Instant joinedAt
) {}

