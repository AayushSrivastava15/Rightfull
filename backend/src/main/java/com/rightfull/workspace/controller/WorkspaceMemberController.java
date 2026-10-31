package com.rightfull.workspace.controller;

import com.rightfull.common.response.ApiResponse;
import com.rightfull.identity.entity.User;
import com.rightfull.identity.service.UserProvisioningService;
import com.rightfull.workspace.dto.UpdateMemberRoleRequest;
import com.rightfull.workspace.dto.WorkspaceMemberResponse;
import com.rightfull.workspace.service.WorkspaceMemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for workspace membership, role administration, and member removals.
 */
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/members")
@Tag(name = "Workspace Members", description = "Endpoints for managing workspace team members and roles")
public class WorkspaceMemberController {

    private final WorkspaceMemberService workspaceMemberService;
    private final UserProvisioningService userProvisioningService;

    public WorkspaceMemberController(
            WorkspaceMemberService workspaceMemberService,
            UserProvisioningService userProvisioningService) {
        this.workspaceMemberService = workspaceMemberService;
        this.userProvisioningService = userProvisioningService;
    }

    @GetMapping
    @PreAuthorize("@workspaceSecurity.hasRole(#workspaceId, 'VIEWER')")
    @Operation(
            summary = "List workspace members",
            description = "Returns all members of the specified workspace. Requires at least VIEWER role.",
            security = @SecurityRequirement(name = "SupabaseBearerAuth")
    )
    public ResponseEntity<ApiResponse<List<WorkspaceMemberResponse>>> getWorkspaceMembers(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID workspaceId) {
        User user = userProvisioningService.getOrCreateUser(jwt);
        List<WorkspaceMemberResponse> members = workspaceMemberService.getWorkspaceMembers(workspaceId, user);
        return ResponseEntity.ok(ApiResponse.ok(members, "Workspace members retrieved successfully"));
    }

    @GetMapping("/{memberId}")
    @PreAuthorize("@workspaceSecurity.hasRole(#workspaceId, 'VIEWER')")
    @Operation(
            summary = "Get workspace member details",
            description = "Returns details for a specific member in the workspace. Requires at least VIEWER role.",
            security = @SecurityRequirement(name = "SupabaseBearerAuth")
    )
    public ResponseEntity<ApiResponse<WorkspaceMemberResponse>> getMemberDetails(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID workspaceId,
            @PathVariable UUID memberId) {
        User user = userProvisioningService.getOrCreateUser(jwt);
        WorkspaceMemberResponse member = workspaceMemberService.getMemberDetails(workspaceId, memberId, user);
        return ResponseEntity.ok(ApiResponse.ok(member, "Member details retrieved successfully"));
    }

    @PatchMapping("/{memberId}")
    @PreAuthorize("@workspaceSecurity.hasRole(#workspaceId, 'ADMIN')")
    @Operation(
            summary = "Update workspace member role",
            description = "Modifies a member's role (ADMIN, MEMBER, VIEWER). Cannot set role to OWNER. Restricted to ADMIN / OWNER.",
            security = @SecurityRequirement(name = "SupabaseBearerAuth")
    )
    public ResponseEntity<ApiResponse<WorkspaceMemberResponse>> updateMemberRole(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID workspaceId,
            @PathVariable UUID memberId,
            @Valid @RequestBody UpdateMemberRoleRequest request) {
        User user = userProvisioningService.getOrCreateUser(jwt);
        WorkspaceMemberResponse member = workspaceMemberService.updateMemberRole(workspaceId, memberId, request, user);
        return ResponseEntity.ok(ApiResponse.ok(member, "Member role updated successfully"));
    }

    @DeleteMapping("/{memberId}")
    @Operation(
            summary = "Remove member or leave workspace",
            description = "Removes a member from the workspace or allows a member to leave. Last OWNER cannot be removed.",
            security = @SecurityRequirement(name = "SupabaseBearerAuth")
    )
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID workspaceId,
            @PathVariable UUID memberId) {
        User user = userProvisioningService.getOrCreateUser(jwt);
        workspaceMemberService.removeMember(workspaceId, memberId, user);
        return ResponseEntity.ok(ApiResponse.ok(null, "Member removed from workspace successfully"));
    }
}

