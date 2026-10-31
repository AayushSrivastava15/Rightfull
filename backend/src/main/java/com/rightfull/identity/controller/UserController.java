package com.rightfull.identity.controller;

import com.rightfull.common.response.ApiResponse;
import com.rightfull.identity.dto.UserProfileResponse;
import com.rightfull.identity.entity.User;
import com.rightfull.identity.service.UserPreferencesService;
import com.rightfull.identity.service.UserProvisioningService;
import com.rightfull.workspace.dto.SetDefaultWorkspaceRequest;
import com.rightfull.workspace.dto.WorkspaceSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for authenticated user profile and preference operations.
 */
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User Identity", description = "Endpoints for user profile, preferences, and default workspace selection")
public class UserController {

    private final UserProvisioningService userProvisioningService;
    private final UserPreferencesService userPreferencesService;

    public UserController(
            UserProvisioningService userProvisioningService,
            UserPreferencesService userPreferencesService) {
        this.userProvisioningService = userProvisioningService;
        this.userPreferencesService = userPreferencesService;
    }

    @GetMapping("/me")
    @Operation(
            summary = "Get current authenticated user profile",
            description = "Returns the verified application profile, default workspace, and workspace memberships for the authenticated Supabase user.",
            security = @SecurityRequirement(name = "SupabaseBearerAuth")
    )
    public ResponseEntity<ApiResponse<UserProfileResponse>> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        UserProfileResponse profile = userProvisioningService.getUserProfile(jwt);
        return ResponseEntity.ok(ApiResponse.ok(profile, "User profile retrieved successfully"));
    }

    @GetMapping("/me/default-workspace")
    @Operation(
            summary = "Get current default workspace",
            description = "Returns the user's active default workspace, verifying active membership.",
            security = @SecurityRequirement(name = "SupabaseBearerAuth")
    )
    public ResponseEntity<ApiResponse<WorkspaceSummaryResponse>> getDefaultWorkspace(@AuthenticationPrincipal Jwt jwt) {
        User user = userProvisioningService.getOrCreateUser(jwt);
        WorkspaceSummaryResponse defaultWs = userPreferencesService.getDefaultWorkspace(user);
        return ResponseEntity.ok(ApiResponse.ok(defaultWs, "Default workspace retrieved successfully"));
    }

    @PostMapping("/me/default-workspace")
    @Operation(
            summary = "Set default active workspace",
            description = "Switches the user's default active workspace. Validates that the user is an active member of the target workspace.",
            security = @SecurityRequirement(name = "SupabaseBearerAuth")
    )
    public ResponseEntity<ApiResponse<WorkspaceSummaryResponse>> setDefaultWorkspace(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody SetDefaultWorkspaceRequest request) {
        User user = userProvisioningService.getOrCreateUser(jwt);
        WorkspaceSummaryResponse defaultWs = userPreferencesService.setDefaultWorkspace(user, request.workspaceId());
        return ResponseEntity.ok(ApiResponse.ok(defaultWs, "Default workspace updated successfully"));
    }
}
