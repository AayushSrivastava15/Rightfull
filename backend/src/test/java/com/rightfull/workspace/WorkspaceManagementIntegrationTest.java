package com.rightfull.workspace;

import com.rightfull.identity.dto.UserProfileResponse;
import com.rightfull.identity.entity.User;
import com.rightfull.identity.service.UserPreferencesService;
import com.rightfull.identity.service.UserProvisioningService;
import com.rightfull.workspace.dto.CreateInvitationRequest;
import com.rightfull.workspace.dto.CreateWorkspaceRequest;
import com.rightfull.workspace.dto.SetDefaultWorkspaceRequest;
import com.rightfull.workspace.dto.TransferOwnershipRequest;
import com.rightfull.workspace.dto.UpdateMemberRoleRequest;
import com.rightfull.workspace.dto.UpdateWorkspaceRequest;
import com.rightfull.workspace.dto.WorkspaceInvitationResponse;
import com.rightfull.workspace.dto.WorkspaceMemberResponse;
import com.rightfull.workspace.dto.WorkspaceResponse;
import com.rightfull.workspace.dto.WorkspaceSummaryResponse;
import com.rightfull.workspace.entity.WorkspaceInvitationStatus;
import com.rightfull.workspace.entity.WorkspaceRole;
import com.rightfull.workspace.security.WorkspaceSecurityEvaluator;
import com.rightfull.workspace.service.WorkspaceInvitationService;
import com.rightfull.workspace.service.WorkspaceMemberService;
import com.rightfull.workspace.service.WorkspaceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class WorkspaceManagementIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private UserProvisioningService userProvisioningService;

    @MockitoBean
    private WorkspaceService workspaceService;

    @MockitoBean
    private WorkspaceMemberService workspaceMemberService;

    @MockitoBean
    private WorkspaceInvitationService workspaceInvitationService;

    @MockitoBean
    private UserPreferencesService userPreferencesService;

    @MockitoBean
    private WorkspaceSecurityEvaluator workspaceSecurityEvaluator;

    private UUID userId;
    private User testUser;
    private UUID workspaceId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        userId = UUID.randomUUID();
        testUser = new User(UUID.randomUUID(), "architect@rightfull.com", "System Architect", null);
        testUser.setId(userId);
        workspaceId = UUID.randomUUID();

        when(userProvisioningService.getOrCreateUser(any())).thenReturn(testUser);
    }

    private Jwt mockJwt() {
        return Jwt.withTokenValue("mock.jwt.token")
                .header("alg", "RS256")
                .claim("sub", testUser.getSupabaseUserId().toString())
                .claim("email", testUser.getEmail())
                .claim("aud", "authenticated")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }

    @Test
    @DisplayName("Unauthenticated request to /api/v1/workspaces returns HTTP 401 Unauthorized")
    void testUnauthenticatedWorkspaceAccessRejected() throws Exception {
        mockMvc.perform(get("/api/v1/workspaces"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("UNAUTHORIZED")));
    }

    @Test
    @DisplayName("GET /api/v1/workspaces returns authenticated user's workspaces")
    void testGetUserWorkspaces() throws Exception {
        when(jwtDecoder.decode("token")).thenReturn(mockJwt());

        WorkspaceSummaryResponse ws = new WorkspaceSummaryResponse(
                workspaceId, "Rightful Enterprise", "rightful-enterprise", "ENTERPRISE", "OWNER", Instant.now()
        );
        when(workspaceService.getUserWorkspaces(any())).thenReturn(List.of(ws));

        mockMvc.perform(get("/api/v1/workspaces")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data[0].name", is("Rightful Enterprise")))
                .andExpect(jsonPath("$.data[0].role", is("OWNER")));
    }

    @Test
    @DisplayName("POST /api/v1/workspaces provisions new workspace with OWNER role")
    void testCreateWorkspace() throws Exception {
        when(jwtDecoder.decode("token")).thenReturn(mockJwt());

        WorkspaceResponse response = new WorkspaceResponse(
                workspaceId, "New Corp", "new-corp", "COMMUNITY", "ACTIVE", userId, "OWNER", 1, Instant.now()
        );
        when(workspaceService.createWorkspace(any(), any(CreateWorkspaceRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/workspaces")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"New Corp\", \"slug\": \"new-corp\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.slug", is("new-corp")))
                .andExpect(jsonPath("$.data.currentUserRole", is("OWNER")));
    }

    @Test
    @DisplayName("GET /api/v1/workspaces/{id} succeeds for member with VIEWER role")
    void testGetWorkspaceDetails() throws Exception {
        when(jwtDecoder.decode("token")).thenReturn(mockJwt());
        when(workspaceSecurityEvaluator.hasRole(eq(workspaceId), eq("VIEWER"))).thenReturn(true);

        WorkspaceResponse response = new WorkspaceResponse(
                workspaceId, "New Corp", "new-corp", "COMMUNITY", "ACTIVE", userId, "VIEWER", 3, Instant.now()
        );
        when(workspaceService.getWorkspaceDetails(eq(workspaceId), any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/workspaces/" + workspaceId)
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name", is("New Corp")));
    }

    @Test
    @DisplayName("GET /api/v1/workspaces/{id} is rejected with HTTP 403 when not a member")
    void testGetWorkspaceDetailsCrossTenantRejected() throws Exception {
        when(jwtDecoder.decode("token")).thenReturn(mockJwt());
        when(workspaceSecurityEvaluator.hasRole(eq(workspaceId), eq("VIEWER"))).thenReturn(false);

        mockMvc.perform(get("/api/v1/workspaces/" + workspaceId)
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code", is("FORBIDDEN")));
    }

    @Test
    @DisplayName("PATCH /api/v1/workspaces/{id} updates workspace settings")
    void testUpdateWorkspace() throws Exception {
        when(jwtDecoder.decode("token")).thenReturn(mockJwt());
        when(workspaceSecurityEvaluator.hasRole(eq(workspaceId), eq("ADMIN"))).thenReturn(true);

        WorkspaceResponse response = new WorkspaceResponse(
                workspaceId, "Renamed Corp", "new-corp", "COMMUNITY", "ACTIVE", userId, "ADMIN", 3, Instant.now()
        );
        when(workspaceService.updateWorkspace(eq(workspaceId), any(UpdateWorkspaceRequest.class), any())).thenReturn(response);

        mockMvc.perform(patch("/api/v1/workspaces/" + workspaceId)
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Renamed Corp\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name", is("Renamed Corp")));
    }

    @Test
    @DisplayName("POST /api/v1/workspaces/{id}/archive archives workspace for OWNER")
    void testArchiveWorkspace() throws Exception {
        when(jwtDecoder.decode("token")).thenReturn(mockJwt());
        when(workspaceSecurityEvaluator.hasRole(eq(workspaceId), eq("OWNER"))).thenReturn(true);

        WorkspaceResponse response = new WorkspaceResponse(
                workspaceId, "Renamed Corp", "new-corp", "COMMUNITY", "ARCHIVED", userId, "OWNER", 3, Instant.now()
        );
        when(workspaceService.archiveWorkspace(eq(workspaceId), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/workspaces/" + workspaceId + "/archive")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("ARCHIVED")));
    }

    @Test
    @DisplayName("POST /api/v1/workspaces/{id}/invitations issues single-use invitation")
    void testCreateInvitation() throws Exception {
        when(jwtDecoder.decode("token")).thenReturn(mockJwt());
        when(workspaceSecurityEvaluator.hasRole(eq(workspaceId), eq("ADMIN"))).thenReturn(true);

        WorkspaceInvitationResponse response = new WorkspaceInvitationResponse(
                UUID.randomUUID(), workspaceId, "New Corp", "colleague@rightfull.com",
                WorkspaceRole.MEMBER, WorkspaceInvitationStatus.PENDING, "admin@rightfull.com",
                Instant.now().plusSeconds(604800), Instant.now(), "secure_raw_token_xyz"
        );
        when(workspaceInvitationService.createInvitation(eq(workspaceId), any(CreateInvitationRequest.class), any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/workspaces/" + workspaceId + "/invitations")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"colleague@rightfull.com\", \"role\": \"MEMBER\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.email", is("colleague@rightfull.com")))
                .andExpect(jsonPath("$.data.invitationToken", is("secure_raw_token_xyz")));
    }

    @Test
    @DisplayName("POST /api/v1/invitations/{token}/accept redeems token")
    void testAcceptInvitation() throws Exception {
        when(jwtDecoder.decode("token")).thenReturn(mockJwt());

        WorkspaceInvitationResponse response = new WorkspaceInvitationResponse(
                UUID.randomUUID(), workspaceId, "New Corp", "architect@rightfull.com",
                WorkspaceRole.MEMBER, WorkspaceInvitationStatus.ACCEPTED, "admin@rightfull.com",
                Instant.now().plusSeconds(604800), Instant.now(), null
        );
        when(workspaceInvitationService.acceptInvitation(eq("test_token_123"), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/invitations/test_token_123/accept")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("ACCEPTED")));
    }

    @Test
    @DisplayName("POST /api/v1/users/me/default-workspace updates active default workspace")
    void testSetDefaultWorkspace() throws Exception {
        when(jwtDecoder.decode("token")).thenReturn(mockJwt());

        WorkspaceSummaryResponse response = new WorkspaceSummaryResponse(
                workspaceId, "Rightful Enterprise", "rightful-enterprise", "COMMUNITY", "OWNER", Instant.now()
        );
        when(userPreferencesService.setDefaultWorkspace(any(), eq(workspaceId))).thenReturn(response);

        mockMvc.perform(post("/api/v1/users/me/default-workspace")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workspaceId\": \"" + workspaceId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name", is("Rightful Enterprise")));
    }
}

