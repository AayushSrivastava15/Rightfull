package com.rightfull.licensing.dto;

public record LicenseSummaryResponse(
        long totalLicenses,
        long activeLicenses,
        long expiredLicenses,
        long suspendedLicenses,
        long revokedLicenses
) {}

