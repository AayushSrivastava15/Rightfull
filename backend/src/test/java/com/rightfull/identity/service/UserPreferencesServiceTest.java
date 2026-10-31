package com.rightfull.identity.service;

import com.rightfull.audit.WorkspaceAuditService;
import com.rightfull.identity.entity.User;
import com.rightfull.identity.entity.UserPreferences;
import com.rightfull.identity.repository.UserPreferencesRepository;
import com.rightfull.workspace.dto.WorkspaceSummaryResponse;
import com.rightfull.workspace.entity.Workspace;
import com.rightfull.workspace.entity.WorkspaceMember;
import com.rightfull.workspace.entity.WorkspaceRole;
import com.rightfull.workspace.repository.WorkspaceMemberRepository;
import com.rightfull.workspace.repository.WorkspaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserPreferencesServiceTest {

    @Mock
    private UserPreferencesRepository userPreferencesRepository;

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Mock
    private WorkspaceAuditService auditService;

    private UserPreferencesService preferencesService;

    private User user;
    private UUID userId;

    @BeforeEach
    void setUp() {
        preferencesService = new UserPreferencesService(
                userPreferencesRepository,
                workspaceRepository,
                workspaceMemberRepository,
                auditService
        );

        userId = UUID.randomUUID();
        user = new User(UUID.randomUUID(), "test@rightfull.com", "Tester", null);
        user.setId(userId);
    }

    @Test
    @DisplayName("Get default workspace returns current workspace when membership is active")
    void testGetDefaultWorkspaceActive() {
        Workspace ws = new Workspace("My SaaS", "my-saas", user);
        ws.setId(UUID.randomUUID());

        UserPreferences prefs = new UserPreferences(user, ws);
        WorkspaceMember member = new WorkspaceMember(ws, user, WorkspaceRole.OWNER, null);

        when(userPreferencesRepository.findByUserIdWithWorkspace(userId)).thenReturn(Optional.of(prefs));
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(ws.getId(), userId)).thenReturn(Optional.of(member));

        WorkspaceSummaryResponse result = preferencesService.getDefaultWorkspace(user);

        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("My SaaS");
        assertThat(result.role()).isEqualTo("OWNER");
    }

    @Test
    @DisplayName("Get default workspace clears reference if user is no longer a member")
    void testGetDefaultWorkspaceStaleCleared() {
        Workspace ws = new Workspace("Old SaaS", "old-saas", user);
        ws.setId(UUID.randomUUID());

        UserPreferences prefs = new UserPreferences(user, ws);

        when(userPreferencesRepository.findByUserIdWithWorkspace(userId)).thenReturn(Optional.of(prefs));
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(ws.getId(), userId)).thenReturn(Optional.empty());
        when(workspaceMemberRepository.findByUserIdWithWorkspace(userId)).thenReturn(java.util.List.of());

        WorkspaceSummaryResponse result = preferencesService.getDefaultWorkspace(user);

        assertThat(result).isNull();
        assertThat(prefs.getDefaultWorkspace()).isNull();
        verify(userPreferencesRepository).save(prefs);
    }

    @Test
    @DisplayName("Set default workspace succeeds when user is a verified member")
    void testSetDefaultWorkspaceSuccess() {
        UUID wsId = UUID.randomUUID();
        Workspace ws = new Workspace("New Ws", "new-ws", user);
        ws.setId(wsId);

        WorkspaceMember member = new WorkspaceMember(ws, user, WorkspaceRole.ADMIN, null);
        UserPreferences prefs = new UserPreferences(user);

        when(workspaceRepository.findById(wsId)).thenReturn(Optional.of(ws));
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(wsId, userId)).thenReturn(Optional.of(member));
        when(userPreferencesRepository.findByUserId(userId)).thenReturn(Optional.of(prefs));

        WorkspaceSummaryResponse result = preferencesService.setDefaultWorkspace(user, wsId);

        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("New Ws");
        assertThat(result.role()).isEqualTo("ADMIN");
        assertThat(prefs.getDefaultWorkspace()).isEqualTo(ws);
        verify(userPreferencesRepository).save(prefs);
        verify(auditService).recordEvent(any());
    }

    @Test
    @DisplayName("Set default workspace rejected if user is not a member (cross-tenant isolation)")
    void testSetDefaultWorkspaceNotMemberRejected() {
        UUID wsId = UUID.randomUUID();
        Workspace ws = new Workspace("Foreign Ws", "foreign-ws", user);
        ws.setId(wsId);

        when(workspaceRepository.findById(wsId)).thenReturn(Optional.of(ws));
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(wsId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> preferencesService.setDefaultWorkspace(user, wsId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("User is not a member of the selected workspace");
    }
}

