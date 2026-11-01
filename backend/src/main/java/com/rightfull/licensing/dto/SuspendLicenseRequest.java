package com.rightfull.licensing.dto;

import jakarta.validation.constraints.Size;

public record SuspendLicenseRequest(
        @Size(max = 500, message = "Reason cannot exceed 500 characters") String reason
) {}

