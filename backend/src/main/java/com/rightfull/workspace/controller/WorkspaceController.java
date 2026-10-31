package com.rightfull.workspace.controller;

import com.rightfull.common.response.ApiResponse;
import com.rightfull.identity.entity.User;
import com.rightfull.identity.service.UserProvisioningService;
import com.rightfull.workspace.dto.CreateWorkspaceRequest;
import com.rightfull.workspace.dto.TransferOwnershipRequest;
import com.rightfull.workspace.dto.UpdateWorkspaceRequest;
import com.rightfull.workspace.dto.WorkspaceResponse;
import com.rightfull.workspace.dto.WorkspaceSummaryResponse;
import com.rightfull.workspace.service.WorkspaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for workspace CRUD, retrieval, archiving, and ownership management.
 */
@RestController
@RequestMapping("/api/v1/workspaces")
@Tag(name = "Workspaces", description = "Endpoints for managing workspace boundaries, tenant settings, and ownership")
public class WorkspaceController {

    private final WorkspaceService workspaceService;
    private final UserProvisioningService userProvisioningService;

    public WorkspaceController(
            WorkspaceService workspaceService,
            UserProvisioningService userProvisioningService) {
        this.workspaceService = workspaceService;
        this.userProvisioningService = userProvisioningService;
    }

    @GetMapping
    @Operation(
            summary = "List workspaces for current user",
            description = "Returns summaries of all workspaces where the authenticated user is an active member.",
            security = @SecurityRequirement(name = "SupabaseBearerAuth")
    )
    public ResponseEntity<ApiResponse<List<WorkspaceSummaryResponse>>> getUserWorkspaces(
            @AuthenticationPrincipal Jwt jwt) {
        User user = userProvisioningService.getOrCreateUser(jwt);
        List<WorkspaceSummaryResponse> workspaces = workspaceService.getUserWorkspaces(user);
        return ResponseEntity.ok(ApiResponse.ok(workspaces, "User workspaces retrieved successfully"));
    }

    @PostMapping
    @Operation(
            summary = "Create a new workspace",
            description = "Creates a new workspace and automatically provisions the authenticated creator as OWNER.",
            security = @SecurityRequirement(name = "SupabaseBearerAuth")
    )
    public ResponseEntity<ApiResponse<WorkspaceResponse>> createWorkspace(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateWorkspaceRequest request) {
        User user = userProvisioningService.getOrCreateUser(jwt);
        WorkspaceResponse workspace = workspaceService.createWorkspace(user, request);
        return new ResponseEntity<>(ApiResponse.ok(workspace, "Workspace created successfully"), HttpStatus.CREATED);
    }

    @GetMapping("/{workspaceId}")
    @PreAuthorize("@workspaceSecurity.hasRole(#workspaceId, 'VIEWER')")
    @Operation(
            summary = "Get workspace details",
            description = "Accessible to all workspace members with at least VIEWER role.",
            security = @SecurityRequirement(name = "SupabaseBearerAuth")
    )
    public ResponseEntity<ApiResponse<WorkspaceResponse>> getWorkspace(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID workspaceId) {
        User user = userProvisioningService.getOrCreateUser(jwt);
        WorkspaceResponse workspace = workspaceService.getWorkspaceDetails(workspaceId, user);
        return ResponseEntity.ok(ApiResponse.ok(workspace, "Workspace details retrieved successfully"));
    }

    @RequestMapping(value = "/{workspaceId}", method = {org.springframework.web.bind.annotation.RequestMethod.PATCH, org.springframework.web.bind.annotation.RequestMethod.PUT})
    @PreAuthorize("@workspaceSecurity.hasRole(#workspaceId, 'ADMIN')")
    @Operation(
            summary = "Update workspace settings",
            description = "Updates workspace name. Restricted to members with at least ADMIN role.",
            security = @SecurityRequirement(name = "SupabaseBearerAuth")
    )
    public ResponseEntity<ApiResponse<WorkspaceResponse>> updateWorkspace(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID workspaceId,
            @Valid @RequestBody UpdateWorkspaceRequest request) {
        User user = userProvisioningService.getOrCreateUser(jwt);
        WorkspaceResponse workspace = workspaceService.updateWorkspace(workspaceId, request, user);
        return ResponseEntity.ok(ApiResponse.ok(workspace, "Workspace updated successfully"));
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/{workspaceId}")
    @PreAuthorize("@workspaceSecurity.hasRole(#workspaceId, 'OWNER')")
    @Operation(
            summary = "Delete workspace",
            description = "Permanently deletes a workspace. Restricted exclusively to workspace OWNER.",
            security = @SecurityRequirement(name = "SupabaseBearerAuth")
    )
    public ResponseEntity<ApiResponse<Void>> deleteWorkspace(@PathVariable UUID workspaceId) {
        workspaceService.deleteWorkspace(workspaceId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Workspace deleted successfully"));
    }

    @PostMapping("/{workspaceId}/archive")
    @PreAuthorize("@workspaceSecurity.hasRole(#workspaceId, 'OWNER')")
    @Operation(
            summary = "Archive workspace",
            description = "Archives a workspace, marking it inactive. Restricted exclusively to workspace OWNER.",
            security = @SecurityRequirement(name = "SupabaseBearerAuth")
    )
    public ResponseEntity<ApiResponse<WorkspaceResponse>> archiveWorkspace(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID workspaceId) {
        User user = userProvisioningService.getOrCreateUser(jwt);
        WorkspaceResponse workspace = workspaceService.archiveWorkspace(workspaceId, user);
        return ResponseEntity.ok(ApiResponse.ok(workspace, "Workspace archived successfully"));
    }

    @PostMapping("/{workspaceId}/transfer-ownership")
    @PreAuthorize("@workspaceSecurity.hasRole(#workspaceId, 'OWNER')")
    @Operation(
            summary = "Transfer workspace ownership",
            description = "Transfers workspace ownership to an existing active member. Restricted exclusively to workspace OWNER.",
            security = @SecurityRequirement(name = "SupabaseBearerAuth")
    )
    public ResponseEntity<ApiResponse<WorkspaceResponse>> transferOwnership(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID workspaceId,
            @Valid @RequestBody TransferOwnershipRequest request) {
        User user = userProvisioningService.getOrCreateUser(jwt);
        WorkspaceResponse workspace = workspaceService.transferOwnership(workspaceId, request, user);
        return ResponseEntity.ok(ApiResponse.ok(workspace, "Workspace ownership transferred successfully"));
    }
}
