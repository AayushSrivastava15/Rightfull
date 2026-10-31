package com.rightfull.workspace.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record TransferOwnershipRequest(
        @NotNull(message = "New owner user ID is required")
        UUID newOwnerUserId
) {}

