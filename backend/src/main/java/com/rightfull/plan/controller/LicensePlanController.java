package com.rightfull.plan.controller;

import com.rightfull.common.response.ApiResponse;
import com.rightfull.plan.dto.CreatePlanRequest;
import com.rightfull.plan.dto.PlanResponse;
import com.rightfull.plan.dto.PlanSnapshot;
import com.rightfull.plan.dto.PlanVersionResponse;
import com.rightfull.plan.dto.UpdatePlanRequest;
import com.rightfull.plan.entity.PlanStatus;
import com.rightfull.plan.service.LicensePlanService;
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

import java.util.List;
import java.util.UUID;

/**
 * REST controller for workspace-scoped software product license plans and entitlements.
 */
@RestController
@RequestMapping({"/api/v1/products/{productId}/license-configurations", "/api/v1/products/{productId}/plans"})
@Tag(name = "License Configurations", description = "Endpoints for managing workspace-scoped product license configurations, restriction presets, and structured entitlements")
public class LicensePlanController {

    private final LicensePlanService planService;

    public LicensePlanController(LicensePlanService planService) {
        this.planService = planService;
    }

    @PostMapping
    @PreAuthorize("@workspaceSecurity.hasRole(#workspaceId, 'ADMIN')")
    @Operation(
            summary = "Create license configuration",
            description = "Creates a new reusable license configuration preset with duration, activations, seats, grace periods, and structured entitlements for a product. Restricted to ADMIN / OWNER.",
            security = @SecurityRequirement(name = "SupabaseBearerAuth")
    )
    public ResponseEntity<ApiResponse<PlanResponse>> createPlan(
            @RequestParam UUID workspaceId,
            @PathVariable UUID productId,
            @Valid @RequestBody CreatePlanRequest request) {
        UUID actorId = TenantContext.getUserId().orElse(null);
        PlanResponse response = planService.createPlan(workspaceId, productId, request, actorId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "License configuration created successfully"));
    }

    @GetMapping
    @PreAuthorize("@workspaceSecurity.hasRole(#workspaceId, 'VIEWER')")
    @Operation(
            summary = "List license configurations",
            description = "Returns a paginated list of license configurations for a product in the authorized workspace. Accessible to all members (VIEWER+).",
            security = @SecurityRequirement(name = "SupabaseBearerAuth")
    )
    public ResponseEntity<ApiResponse<Page<PlanResponse>>> listPlans(
            @RequestParam UUID workspaceId,
            @PathVariable UUID productId,
            @RequestParam(required = false) PlanStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<PlanResponse> page = planService.listPlansPaged(workspaceId, productId, status, pageable);
        return ResponseEntity.ok(ApiResponse.ok(page, "License configurations retrieved successfully"));
    }

    @GetMapping("/all")
    @PreAuthorize("@workspaceSecurity.hasRole(#workspaceId, 'VIEWER')")
    @Operation(
            summary = "List all active license configurations without pagination",
            description = "Returns all active license configurations for dropdown selectors and configuration presets.",
            security = @SecurityRequirement(name = "SupabaseBearerAuth")
    )
    public ResponseEntity<ApiResponse<List<PlanResponse>>> listAllPlans(
            @RequestParam UUID workspaceId,
            @PathVariable UUID productId,
            @RequestParam(required = false) PlanStatus status) {
        List<PlanResponse> list = planService.listPlans(workspaceId, productId, status);
        return ResponseEntity.ok(ApiResponse.ok(list, "All license configurations retrieved successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@workspaceSecurity.hasRole(#workspaceId, 'VIEWER')")
    @Operation(
            summary = "Get license configuration details",
            description = "Retrieves full details and structured entitlements for a specific license configuration.",
            security = @SecurityRequirement(name = "SupabaseBearerAuth")
    )
    public ResponseEntity<ApiResponse<PlanResponse>> getPlan(
            @RequestParam UUID workspaceId,
            @PathVariable UUID productId,
            @PathVariable("id") UUID id) {
        PlanResponse response = planService.getPlan(workspaceId, productId, id);
        return ResponseEntity.ok(ApiResponse.ok(response, "License configuration retrieved successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@workspaceSecurity.hasRole(#workspaceId, 'ADMIN')")
    @Operation(
            summary = "Update license configuration",
            description = "Updates configuration parameters or entitlements. If entitlements or limits change, configuration version is automatically incremented. Restricted to ADMIN / OWNER.",
            security = @SecurityRequirement(name = "SupabaseBearerAuth")
    )
    public ResponseEntity<ApiResponse<PlanResponse>> updatePlan(
            @RequestParam UUID workspaceId,
            @PathVariable UUID productId,
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdatePlanRequest request) {
        UUID actorId = TenantContext.getUserId().orElse(null);
        PlanResponse response = planService.updatePlan(workspaceId, productId, id, request, actorId);
        return ResponseEntity.ok(ApiResponse.ok(response, "License configuration updated successfully"));
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("@workspaceSecurity.hasRole(#workspaceId, 'ADMIN')")
    @Operation(
            summary = "Archive license configuration",
            description = "Transitions a license configuration to ARCHIVED. Hard delete is denied. Restricted to ADMIN / OWNER.",
            security = @SecurityRequirement(name = "SupabaseBearerAuth")
    )
    public ResponseEntity<ApiResponse<PlanResponse>> archivePlan(
            @RequestParam UUID workspaceId,
            @PathVariable UUID productId,
            @PathVariable("id") UUID id) {
        UUID actorId = TenantContext.getUserId().orElse(null);
        PlanResponse response = planService.archivePlan(workspaceId, productId, id, actorId);
        return ResponseEntity.ok(ApiResponse.ok(response, "License configuration archived successfully"));
    }

    @GetMapping("/{id}/snapshot")
    @PreAuthorize("@workspaceSecurity.hasRole(#workspaceId, 'VIEWER')")
    @Operation(
            summary = "Get immutable configuration entitlement snapshot",
            description = "Generates an immutable snapshot of configuration parameters and entitlements for license issuance.",
            security = @SecurityRequirement(name = "SupabaseBearerAuth")
    )
    public ResponseEntity<ApiResponse<PlanSnapshot>> getPlanSnapshot(
            @RequestParam UUID workspaceId,
            @PathVariable UUID productId,
            @PathVariable("id") UUID id) {
        PlanSnapshot snapshot = planService.getPlanSnapshot(workspaceId, productId, id);
        return ResponseEntity.ok(ApiResponse.ok(snapshot, "Configuration snapshot retrieved successfully"));
    }

    @GetMapping("/{id}/versions")
    @PreAuthorize("@workspaceSecurity.hasRole(#workspaceId, 'VIEWER')")
    @Operation(
            summary = "List configuration historical versions",
            description = "Retrieves all historical version records for a license configuration preset.",
            security = @SecurityRequirement(name = "SupabaseBearerAuth")
    )
    public ResponseEntity<ApiResponse<List<PlanVersionResponse>>> getPlanVersions(
            @RequestParam UUID workspaceId,
            @PathVariable UUID productId,
            @PathVariable("id") UUID id) {
        List<PlanVersionResponse> versions = planService.getPlanVersions(workspaceId, productId, id);
        return ResponseEntity.ok(ApiResponse.ok(versions, "Configuration versions retrieved successfully"));
    }

    @GetMapping("/{id}/versions/{version}")
    @PreAuthorize("@workspaceSecurity.hasRole(#workspaceId, 'VIEWER')")
    @Operation(
            summary = "Get specific configuration historical version",
            description = "Retrieves a specific historical version of a configuration with full past entitlements.",
            security = @SecurityRequirement(name = "SupabaseBearerAuth")
    )
    public ResponseEntity<ApiResponse<PlanVersionResponse>> getPlanVersion(
            @RequestParam UUID workspaceId,
            @PathVariable UUID productId,
            @PathVariable("id") UUID id,
            @PathVariable("version") int version) {
        PlanVersionResponse response = planService.getPlanVersion(workspaceId, productId, id, version);
        return ResponseEntity.ok(ApiResponse.ok(response, "Configuration version retrieved successfully"));
    }
}

