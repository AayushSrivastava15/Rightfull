package com.rightfull.security;

import com.rightfull.identity.dto.UserProfileResponse;
import com.rightfull.identity.entity.User;
import com.rightfull.identity.service.UserProvisioningService;
import com.rightfull.workspace.entity.Workspace;
import com.rightfull.workspace.entity.WorkspaceRole;
import com.rightfull.workspace.entity.WorkspaceTier;
import com.rightfull.workspace.repository.WorkspaceRepository;
import com.rightfull.workspace.security.WorkspaceSecurityEvaluator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class AuthenticationIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private UserProvisioningService userProvisioningService;

    @MockitoBean
    private WorkspaceSecurityEvaluator workspaceSecurityEvaluator;

    @MockitoBean
    private WorkspaceRepository workspaceRepository;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    private Jwt createMockJwt(UUID supabaseUserId, String email) {
        return Jwt.withTokenValue("valid.test.token")
                .header("alg", "ES256")
                .subject(supabaseUserId.toString())
                .claim("email", email)
                .claim("aud", "authenticated")
                .issuer("https://test.supabase.co/auth/v1")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/users/me without Bearer token returns HTTP 401 Unauthorized with standard error envelope")
    void unauthenticatedRequestReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("UNAUTHORIZED")))
                .andExpect(jsonPath("$.error.message", is("Invalid, expired or missing authentication token")));
    }

    @Test
    @DisplayName("GET /api/v1/users/me with malformed / invalid JWT returns HTTP 401 Unauthorized")
    void invalidJwtReturns401() throws Exception {
        when(jwtDecoder.decode("bad.token")).thenThrow(new BadJwtException("Invalid token signature"));

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer bad.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("UNAUTHORIZED")));
    }

    @Test
    @DisplayName("GET /api/v1/users/me with valid Bearer token returns HTTP 200 with verified user profile")
    void authenticatedRequestReturns200WithProfile() throws Exception {
        UUID supabaseUserId = UUID.randomUUID();
        UUID appUserId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        String email = "sarah@rightfull.com";

        Jwt jwt = createMockJwt(supabaseUserId, email);
        when(jwtDecoder.decode("valid.test.token")).thenReturn(jwt);

        UserProfileResponse.WorkspaceSummaryDto wsSummary = new UserProfileResponse.WorkspaceSummaryDto(
                workspaceId, "Sarah's Workspace", "sarah-workspace", WorkspaceTier.COMMUNITY, WorkspaceRole.OWNER
        );
        UserProfileResponse.WorkspaceMembershipDto membership = new UserProfileResponse.WorkspaceMembershipDto(
                workspaceId, "Sarah's Workspace", "sarah-workspace", WorkspaceTier.COMMUNITY, WorkspaceRole.OWNER, Instant.now()
        );
        UserProfileResponse profileResponse = new UserProfileResponse(
                appUserId, supabaseUserId, email, "Sarah Connor", null, true, Instant.now(), wsSummary, List.of(membership)
        );

        when(userProvisioningService.getUserProfile(any(Jwt.class))).thenReturn(profileResponse);

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer valid.test.token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(appUserId.toString())))
                .andExpect(jsonPath("$.data.supabaseUserId", is(supabaseUserId.toString())))
                .andExpect(jsonPath("$.data.email", is(email)))
                .andExpect(jsonPath("$.data.fullName", is("Sarah Connor")))
                .andExpect(jsonPath("$.data.defaultWorkspace.slug", is("sarah-workspace")))
                .andExpect(jsonPath("$.data.defaultWorkspace.role", is("OWNER")));
    }

    @Test
    @DisplayName("Workspace endpoint without proper role returns HTTP 403 Forbidden with standard error envelope")
    void unauthorizedWorkspaceAccessReturns403() throws Exception {
        UUID supabaseUserId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();

        Jwt jwt = createMockJwt(supabaseUserId, "dev@rightfull.com");
        when(jwtDecoder.decode("valid.test.token")).thenReturn(jwt);

        // Security evaluator returns false (insufficient role / not member)
        when(workspaceSecurityEvaluator.hasRole(eq(workspaceId), eq("VIEWER"))).thenReturn(false);

        mockMvc.perform(get("/api/v1/workspaces/" + workspaceId)
                        .header("Authorization", "Bearer valid.test.token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("FORBIDDEN")))
                .andExpect(jsonPath("$.error.message", is("Access denied: insufficient workspace privileges")));
    }

    @Test
    @DisplayName("Member attempting ADMIN workspace operation is rejected with HTTP 403 (role escalation prevention)")
    void memberCannotPerformAdminAction() throws Exception {
        UUID supabaseUserId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();

        Jwt jwt = createMockJwt(supabaseUserId, "member@rightfull.com");
        when(jwtDecoder.decode("valid.test.token")).thenReturn(jwt);

        // Evaluator denies ADMIN role
        when(workspaceSecurityEvaluator.hasRole(eq(workspaceId), eq("ADMIN"))).thenReturn(false);

        mockMvc.perform(put("/api/v1/workspaces/" + workspaceId)
                        .header("Authorization", "Bearer valid.test.token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Hacked Workspace\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code", is("FORBIDDEN")));
    }

    @Test
    @DisplayName("Non-owner attempting OWNER workspace deletion is rejected with HTTP 403")
    void nonOwnerCannotDeleteWorkspace() throws Exception {
        UUID supabaseUserId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();

        Jwt jwt = createMockJwt(supabaseUserId, "admin@rightfull.com");
        when(jwtDecoder.decode("valid.test.token")).thenReturn(jwt);

        // Evaluator denies OWNER role
        when(workspaceSecurityEvaluator.hasRole(eq(workspaceId), eq("OWNER"))).thenReturn(false);

        mockMvc.perform(delete("/api/v1/workspaces/" + workspaceId)
                        .header("Authorization", "Bearer valid.test.token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code", is("FORBIDDEN")));
    }

    @Test
    @DisplayName("Workspace OWNER can successfully delete workspace")
    void ownerCanDeleteWorkspace() throws Exception {
        UUID supabaseUserId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();

        Jwt jwt = createMockJwt(supabaseUserId, "owner@rightfull.com");
        when(jwtDecoder.decode("valid.test.token")).thenReturn(jwt);

        when(workspaceSecurityEvaluator.hasRole(eq(workspaceId), eq("OWNER"))).thenReturn(true);

        User owner = new User(supabaseUserId, "owner@rightfull.com", "Owner", null);
        Workspace ws = new Workspace("Owner Workspace", "owner-ws", owner);
        ws.setId(workspaceId);
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(ws));

        mockMvc.perform(delete("/api/v1/workspaces/" + workspaceId)
                        .header("Authorization", "Bearer valid.test.token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Workspace deleted successfully")));
    }
}
