package com.rightfull.workspace.controller;

import com.rightfull.common.response.ApiResponse;
import com.rightfull.identity.entity.User;
import com.rightfull.identity.service.UserProvisioningService;
import com.rightfull.workspace.dto.CreateInvitationRequest;
import com.rightfull.workspace.dto.WorkspaceInvitationResponse;
import com.rightfull.workspace.service.WorkspaceInvitationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for workspace member invitations and acceptance/rejection token redemption.
 */
@RestController
@Tag(name = "Workspace Invitations", description = "Endpoints for creating, listing, revoking, and redeeming workspace invitations")
public class WorkspaceInvitationController {

    private final WorkspaceInvitationService invitationService;
    private final UserProvisioningService userProvisioningService;

    public WorkspaceInvitationController(
            WorkspaceInvitationService invitationService,
            UserProvisioningService userProvisioningService) {
        this.invitationService = invitationService;
        this.userProvisioningService = userProvisioningService;
    }

    @PostMapping("/api/v1/workspaces/{workspaceId}/invitations")
    @PreAuthorize("@workspaceSecurity.hasRole(#workspaceId, 'ADMIN')")
    @Operation(
            summary = "Create workspace invitation",
            description = "Issues a single-use cryptographically hashed invitation to join the workspace. Restricted to ADMIN / OWNER.",
            security = @SecurityRequirement(name = "SupabaseBearerAuth")
    )
    public ResponseEntity<ApiResponse<WorkspaceInvitationResponse>> createInvitation(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID workspaceId,
            @Valid @RequestBody CreateInvitationRequest request) {
        User user = userProvisioningService.getOrCreateUser(jwt);
        WorkspaceInvitationResponse response = invitationService.createInvitation(workspaceId, request, user);
        return new ResponseEntity<>(ApiResponse.ok(response, "Invitation created successfully"), HttpStatus.CREATED);
    }

    @GetMapping("/api/v1/workspaces/{workspaceId}/invitations")
    @PreAuthorize("@workspaceSecurity.hasRole(#workspaceId, 'ADMIN')")
    @Operation(
            summary = "List pending invitations",
            description = "Returns all pending invitations for the workspace. Restricted to ADMIN / OWNER.",
            security = @SecurityRequirement(name = "SupabaseBearerAuth")
    )
    public ResponseEntity<ApiResponse<List<WorkspaceInvitationResponse>>> getPendingInvitations(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID workspaceId) {
        User user = userProvisioningService.getOrCreateUser(jwt);
        List<WorkspaceInvitationResponse> invitations = invitationService.getPendingInvitations(workspaceId, user);
        return ResponseEntity.ok(ApiResponse.ok(invitations, "Pending invitations retrieved successfully"));
    }

    @DeleteMapping("/api/v1/workspaces/{workspaceId}/invitations/{invitationId}")
    @PreAuthorize("@workspaceSecurity.hasRole(#workspaceId, 'ADMIN')")
    @Operation(
            summary = "Revoke workspace invitation",
            description = "Revokes a pending invitation before redemption. Restricted to ADMIN / OWNER.",
            security = @SecurityRequirement(name = "SupabaseBearerAuth")
    )
    public ResponseEntity<ApiResponse<Void>> revokeInvitation(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID workspaceId,
            @PathVariable UUID invitationId) {
        User user = userProvisioningService.getOrCreateUser(jwt);
        invitationService.revokeInvitation(workspaceId, invitationId, user);
        return ResponseEntity.ok(ApiResponse.ok(null, "Invitation revoked successfully"));
    }

    @PostMapping("/api/v1/invitations/{token}/accept")
    @Operation(
            summary = "Accept workspace invitation",
            description = "Redeems a single-use invitation token. The authenticated user's email must match the invitation recipient.",
            security = @SecurityRequirement(name = "SupabaseBearerAuth")
    )
    public ResponseEntity<ApiResponse<WorkspaceInvitationResponse>> acceptInvitation(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String token) {
        User user = userProvisioningService.getOrCreateUser(jwt);
        WorkspaceInvitationResponse response = invitationService.acceptInvitation(token, user);
        return ResponseEntity.ok(ApiResponse.ok(response, "Invitation accepted successfully"));
    }

    @PostMapping("/api/v1/invitations/{token}/reject")
    @Operation(
            summary = "Reject workspace invitation",
            description = "Rejects a pending invitation using the unhashed token.",
            security = @SecurityRequirement(name = "SupabaseBearerAuth")
    )
    public ResponseEntity<ApiResponse<WorkspaceInvitationResponse>> rejectInvitation(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String token) {
        User user = userProvisioningService.getOrCreateUser(jwt);
        WorkspaceInvitationResponse response = invitationService.rejectInvitation(token, user);
        return ResponseEntity.ok(ApiResponse.ok(response, "Invitation rejected successfully"));
    }
}

