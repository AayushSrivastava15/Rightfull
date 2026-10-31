package com.rightfull.workspace.security;

import com.rightfull.identity.entity.User;
import com.rightfull.identity.repository.UserRepository;
import com.rightfull.workspace.entity.Workspace;
import com.rightfull.workspace.entity.WorkspaceMember;
import com.rightfull.workspace.entity.WorkspaceRole;
import com.rightfull.workspace.repository.WorkspaceMemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkspaceSecurityEvaluatorTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private WorkspaceMemberRepository workspaceMemberRepository;

    private WorkspaceSecurityEvaluator evaluator;

    private UUID userId;
    private UUID supabaseUserId;
    private UUID workspaceId;
    private User testUser;
    private Workspace testWorkspace;

    @BeforeEach
    void setUp() {
        evaluator = new WorkspaceSecurityEvaluator(userRepository, workspaceMemberRepository);

        userId = UUID.randomUUID();
        supabaseUserId = UUID.randomUUID();
        workspaceId = UUID.randomUUID();

        testUser = new User(supabaseUserId, "dev@rightfull.com", "Dev User", null);
        testUser.setId(userId);

        testWorkspace = new Workspace("Dev Workspace", "dev-workspace", testUser);
        testWorkspace.setId(workspaceId);

        // Setup SecurityContext with authenticated Jwt
        Jwt jwt = Jwt.withTokenValue("mock.token")
                .header("alg", "ES256")
                .subject(supabaseUserId.toString())
                .claim("email", "dev@rightfull.com")
                .claim("aud", "authenticated")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("OWNER role satisfies OWNER, ADMIN, MEMBER, and VIEWER requirements")
    void ownerSatisfiesAllRoles() {
        WorkspaceMember ownerMember = new WorkspaceMember(testWorkspace, testUser, WorkspaceRole.OWNER, null);

        when(userRepository.findBySupabaseUserId(supabaseUserId)).thenReturn(Optional.of(testUser));
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)).thenReturn(Optional.of(ownerMember));

        assertThat(evaluator.hasRole(workspaceId, "OWNER")).isTrue();
        assertThat(evaluator.hasRole(workspaceId, "ADMIN")).isTrue();
        assertThat(evaluator.hasRole(workspaceId, "MEMBER")).isTrue();
        assertThat(evaluator.hasRole(workspaceId, "VIEWER")).isTrue();
    }

    @Test
    @DisplayName("ADMIN role satisfies ADMIN, MEMBER, and VIEWER, but NOT OWNER")
    void adminCannotActAsOwner() {
        WorkspaceMember adminMember = new WorkspaceMember(testWorkspace, testUser, WorkspaceRole.ADMIN, null);

        when(userRepository.findBySupabaseUserId(supabaseUserId)).thenReturn(Optional.of(testUser));
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)).thenReturn(Optional.of(adminMember));

        assertThat(evaluator.hasRole(workspaceId, "OWNER")).isFalse();
        assertThat(evaluator.hasRole(workspaceId, "ADMIN")).isTrue();
        assertThat(evaluator.hasRole(workspaceId, "MEMBER")).isTrue();
        assertThat(evaluator.hasRole(workspaceId, "VIEWER")).isTrue();
    }

    @Test
    @DisplayName("MEMBER role cannot escalate to ADMIN or OWNER")
    void memberCannotEscalateToAdmin() {
        WorkspaceMember member = new WorkspaceMember(testWorkspace, testUser, WorkspaceRole.MEMBER, null);

        when(userRepository.findBySupabaseUserId(supabaseUserId)).thenReturn(Optional.of(testUser));
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)).thenReturn(Optional.of(member));

        assertThat(evaluator.hasRole(workspaceId, "OWNER")).isFalse();
        assertThat(evaluator.hasRole(workspaceId, "ADMIN")).isFalse();
        assertThat(evaluator.hasRole(workspaceId, "MEMBER")).isTrue();
        assertThat(evaluator.hasRole(workspaceId, "VIEWER")).isTrue();
    }

    @Test
    @DisplayName("VIEWER role cannot perform write or administrative operations")
    void viewerIsReadOnly() {
        WorkspaceMember viewer = new WorkspaceMember(testWorkspace, testUser, WorkspaceRole.VIEWER, null);

        when(userRepository.findBySupabaseUserId(supabaseUserId)).thenReturn(Optional.of(testUser));
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)).thenReturn(Optional.of(viewer));

        assertThat(evaluator.hasRole(workspaceId, "OWNER")).isFalse();
        assertThat(evaluator.hasRole(workspaceId, "ADMIN")).isFalse();
        assertThat(evaluator.hasRole(workspaceId, "MEMBER")).isFalse();
        assertThat(evaluator.hasRole(workspaceId, "VIEWER")).isTrue();
    }

    @Test
    @DisplayName("Cross-workspace isolation: access denied to workspace where user holds no membership")
    void crossWorkspaceAccessDenied() {
        UUID otherWorkspaceId = UUID.randomUUID();

        when(userRepository.findBySupabaseUserId(supabaseUserId)).thenReturn(Optional.of(testUser));
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(otherWorkspaceId, userId)).thenReturn(Optional.empty());

        assertThat(evaluator.hasRole(otherWorkspaceId, "VIEWER")).isFalse();
    }

    @Test
    @DisplayName("Unauthenticated request or missing SecurityContext returns false")
    void unauthenticatedReturnsFalse() {
        SecurityContextHolder.clearContext();

        assertThat(evaluator.hasRole(workspaceId, "VIEWER")).isFalse();
    }
}

