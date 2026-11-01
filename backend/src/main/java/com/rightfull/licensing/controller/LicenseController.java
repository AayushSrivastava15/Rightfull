package com.rightfull.licensing.controller;

import com.rightfull.activation.dto.ActivationResponse;
import com.rightfull.common.response.ApiResponse;
import com.rightfull.licensing.dto.*;
import com.rightfull.licensing.entity.LicenseStatus;
import com.rightfull.licensing.service.LicenseService;
import com.rightfull.security.tenant.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Endpoints for workspace administrators to issue and manage product licenses,
 * manage device activations, and control license lifecycle.
 */
@RestController
@Tag(name = "Licenses", description = "Endpoints for issuing, querying, renewing, and revoking software product licenses")
public class LicenseController {

    private final LicenseService licenseService;

    public LicenseController(LicenseService licenseService) {
        this.licenseService = licenseService;
    }

    @PostMapping("/api/v1/products/{productId}/licenses")
    @PreAuthorize("@workspaceSecurity.hasRole(#workspaceId, 'ADMIN')")
    @Operation(
            summary = "Issue a software license",
            description = "Issues a new software product license key. Plaintext key is returned ONLY once in this response.",
            security = @SecurityRequirement(name = "SupabaseBearerAuth")
    )
    public ResponseEntity<ApiResponse<IssueLicenseResponse>> issueLicense(
            @RequestParam UUID workspaceId,
            @PathVariable UUID productId,
            @Valid @RequestBody IssueLicenseRequest request) {
        UUID actorId = TenantContext.getUserId().orElse(null);
        IssueLicenseResponse response = licenseService.issueLicense(workspaceId, productId, request, actorId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "License issued successfully. Securely record the plaintext key."));
    }

    @GetMapping("/api/v1/products/{productId}/licenses")
    @PreAuthorize("@workspaceSecurity.hasRole(#workspaceId, 'VIEWER')")
    @Operation(
            summary = "List licenses for a product",
            description = "Returns paginated list of masked licenses for a product.",
            security = @SecurityRequirement(name = "SupabaseBearerAuth")
    )
    public ResponseEntity<ApiResponse<Page<LicenseResponse>>> listProductLicenses(
            @RequestParam UUID workspaceId,
            @PathVariable UUID productId,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) LicenseStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<LicenseResponse> page = licenseService.listLicenses(workspaceId, productId, customerId, status, search, pageable);
        return ResponseEntity.ok(ApiResponse.ok(page, "Product licenses retrieved successfully"));
    }

    @GetMapping("/api/v1/workspaces/{workspaceId}/licenses")
    @PreAuthorize("@workspaceSecurity.hasRole(#workspaceId, 'VIEWER')")
    @Operation(
            summary = "List all workspace licenses",
            description = "Returns paginated list of licenses across all products in the workspace.",
            security = @SecurityRequirement(name = "SupabaseBearerAuth")
    )
    public ResponseEntity<ApiResponse<Page<LicenseResponse>>> listWorkspaceLicenses(
            @PathVariable UUID workspaceId,
            @RequestParam(required = false) UUID productId,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) LicenseStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<LicenseResponse> page = licenseService.listLicenses(workspaceId, productId, customerId, status, search, pageable);
        return ResponseEntity.ok(ApiResponse.ok(page, "Workspace licenses retrieved successfully"));
    }

    @GetMapping("/api/v1/workspaces/{workspaceId}/licenses/summary")
    @PreAuthorize("@workspaceSecurity.hasRole(#workspaceId, 'VIEWER')")
    @Operation(
            summary = "License metrics summary",
            description = "Returns summary counts of licenses grouped by status for dashboard cards.",
            security = @SecurityRequirement(name = "SupabaseBearerAuth")
    )
    public ResponseEntity<ApiResponse<LicenseSummaryResponse>> getSummary(@PathVariable UUID workspaceId) {
        LicenseSummaryResponse summary = licenseService.getLicenseSummary(workspaceId);
        return ResponseEntity.ok(ApiResponse.ok(summary, "License summary retrieved successfully"));
    }

    @GetMapping("/api/v1/licenses/{licenseId}")
    @PreAuthorize("@workspaceSecurity.hasRole(#workspaceId, 'VIEWER')")
    @Operation(
            summary = "Get license details",
            description = "Returns full details, limits, and entitlement snapshot for an issued license.",
            security = @SecurityRequirement(name = "SupabaseBearerAuth")
    )
    public ResponseEntity<ApiResponse<LicenseResponse>> getLicense(
            @RequestParam UUID workspaceId,
            @PathVariable UUID licenseId) {
        LicenseResponse response = licenseService.getLicense(workspaceId, licenseId);
        return ResponseEntity.ok(ApiResponse.ok(response, "License retrieved successfully"));
    }

    @PostMapping("/api/v1/licenses/{licenseId}/suspend")
    @PreAuthorize("@workspaceSecurity.hasRole(#workspaceId, 'ADMIN')")
    @Operation(
            summary = "Suspend a license",
            description = "Temporarily suspends an active license. External validations will fail until resumed.",
            security = @SecurityRequirement(name = "SupabaseBearerAuth")
    )
    public ResponseEntity<ApiResponse<LicenseResponse>> suspendLicense(
            @RequestParam UUID workspaceId,
            @PathVariable UUID licenseId,
            @Valid @RequestBody(required = false) SuspendLicenseRequest request) {
        UUID actorId = TenantContext.getUserId().orElse(null);
        String reason = (request != null) ? request.reason() : "Administrative suspension";
        LicenseResponse response = licenseService.suspendLicense(workspaceId, licenseId, reason, actorId);
        return ResponseEntity.ok(ApiResponse.ok(response, "License suspended successfully"));
    }

    @PostMapping("/api/v1/licenses/{licenseId}/resume")
    @PreAuthorize("@workspaceSecurity.hasRole(#workspaceId, 'ADMIN')")
    @Operation(
            summary = "Resume a suspended license",
            description = "Re-activates a suspended license if it is not expired.",
            security = @SecurityRequirement(name = "SupabaseBearerAuth")
    )
    public ResponseEntity<ApiResponse<LicenseResponse>> resumeLicense(
            @RequestParam UUID workspaceId,
            @PathVariable UUID licenseId) {
        UUID actorId = TenantContext.getUserId().orElse(null);
        LicenseResponse response = licenseService.resumeLicense(workspaceId, licenseId, actorId);
        return ResponseEntity.ok(ApiResponse.ok(response, "License resumed successfully"));
    }

    @PostMapping("/api/v1/licenses/{licenseId}/revoke")
    @PreAuthorize("@workspaceSecurity.hasRole(#workspaceId, 'ADMIN')")
    @Operation(
            summary = "Revoke a license",
            description = "Permanently revokes a license. Revoked licenses can never be resumed or validated.",
            security = @SecurityRequirement(name = "SupabaseBearerAuth")
    )
    public ResponseEntity<ApiResponse<LicenseResponse>> revokeLicense(
            @RequestParam UUID workspaceId,
            @PathVariable UUID licenseId,
            @Valid @RequestBody(required = false) RevokeLicenseRequest request) {
        UUID actorId = TenantContext.getUserId().orElse(null);
        String reason = (request != null) ? request.reason() : "Administrative revocation";
        LicenseResponse response = licenseService.revokeLicense(workspaceId, licenseId, reason, actorId);
        return ResponseEntity.ok(ApiResponse.ok(response, "License revoked permanently"));
    }

    @PostMapping("/api/v1/licenses/{licenseId}/renew")
    @PreAuthorize("@workspaceSecurity.hasRole(#workspaceId, 'ADMIN')")
    @Operation(
            summary = "Renew a license",
            description = "Extends the validity period of an active or expired license.",
            security = @SecurityRequirement(name = "SupabaseBearerAuth")
    )
    public ResponseEntity<ApiResponse<LicenseResponse>> renewLicense(
            @RequestParam UUID workspaceId,
            @PathVariable UUID licenseId,
            @Valid @RequestBody RenewLicenseRequest request) {
        UUID actorId = TenantContext.getUserId().orElse(null);
        LicenseResponse response = licenseService.renewLicense(workspaceId, licenseId, request, actorId);
        return ResponseEntity.ok(ApiResponse.ok(response, "License renewed successfully"));
    }

    @GetMapping("/api/v1/licenses/{licenseId}/activations")
    @PreAuthorize("@workspaceSecurity.hasRole(#workspaceId, 'VIEWER')")
    @Operation(
            summary = "List license device activations",
            description = "Returns paginated list of client device activations bound to this license.",
            security = @SecurityRequirement(name = "SupabaseBearerAuth")
    )
    public ResponseEntity<ApiResponse<Page<ActivationResponse>>> getActivations(
            @RequestParam UUID workspaceId,
            @PathVariable UUID licenseId,
            @PageableDefault(size = 20, sort = "activatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ActivationResponse> page = licenseService.getLicenseActivations(workspaceId, licenseId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(page, "License activations retrieved successfully"));
    }

    @PostMapping("/api/v1/licenses/{licenseId}/activations/{activationId}/deactivate")
    @PreAuthorize("@workspaceSecurity.hasRole(#workspaceId, 'ADMIN')")
    @Operation(
            summary = "Admin deactivate device",
            description = "Deactivates a device registration, freeing up an activation slot on the license.",
            security = @SecurityRequirement(name = "SupabaseBearerAuth")
    )
    public ResponseEntity<ApiResponse<ActivationResponse>> deactivateDevice(
            @RequestParam UUID workspaceId,
            @PathVariable UUID licenseId,
            @PathVariable UUID activationId) {
        UUID actorId = TenantContext.getUserId().orElse(null);
        ActivationResponse response = licenseService.deactivateDevice(workspaceId, licenseId, activationId, actorId);
        return ResponseEntity.ok(ApiResponse.ok(response, "Device deactivated successfully"));
    }
}

