package com.rightfull.workspace.service;

import com.rightfull.audit.WorkspaceAuditService;
import com.rightfull.common.exception.BusinessException;
import com.rightfull.common.exception.ResourceNotFoundException;
import com.rightfull.identity.entity.User;
import com.rightfull.identity.entity.UserPreferences;
import com.rightfull.identity.repository.UserPreferencesRepository;
import com.rightfull.workspace.dto.CreateWorkspaceRequest;
import com.rightfull.workspace.dto.TransferOwnershipRequest;
import com.rightfull.workspace.dto.UpdateWorkspaceRequest;
import com.rightfull.workspace.dto.WorkspaceResponse;
import com.rightfull.workspace.dto.WorkspaceSummaryResponse;
import com.rightfull.workspace.entity.Workspace;
import com.rightfull.workspace.entity.WorkspaceMember;
import com.rightfull.workspace.entity.WorkspaceRole;
import com.rightfull.workspace.entity.WorkspaceStatus;
import com.rightfull.workspace.entity.WorkspaceTier;
import com.rightfull.workspace.repository.WorkspaceMemberRepository;
import com.rightfull.workspace.repository.WorkspaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkspaceServiceTest {

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Mock
    private UserPreferencesRepository userPreferencesRepository;

    @Mock
    private WorkspaceAuditService auditService;

    private WorkspaceService workspaceService;

    private User testUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        workspaceService = new WorkspaceService(
                workspaceRepository,
                workspaceMemberRepository,
                userPreferencesRepository,
                auditService
        );

        userId = UUID.randomUUID();
        testUser = new User(UUID.randomUUID(), "dev@rightfull.com", "Dev Lead", null);
        testUser.setId(userId);
    }

    @Test
    @DisplayName("Create workspace automatically assigns creator as OWNER and sets default workspace if null")
    void testCreateWorkspace() {
        CreateWorkspaceRequest request = new CreateWorkspaceRequest("Acme SaaS", "acme-saas");
        when(workspaceRepository.existsBySlug("acme-saas")).thenReturn(false);

        when(workspaceRepository.save(any(Workspace.class))).thenAnswer(invocation -> {
            Workspace ws = invocation.getArgument(0);
            ws.setId(UUID.randomUUID());
            return ws;
        });

        UserPreferences prefs = new UserPreferences(testUser);
        when(userPreferencesRepository.findByUserId(userId)).thenReturn(Optional.of(prefs));

        WorkspaceResponse response = workspaceService.createWorkspace(testUser, request);

        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("Acme SaaS");
        assertThat(response.slug()).isEqualTo("acme-saas");
        assertThat(response.currentUserRole()).isEqualTo("OWNER");
        assertThat(response.memberCount()).isEqualTo(1);

        // Verify OWNER member was created
        ArgumentCaptor<WorkspaceMember> memberCaptor = ArgumentCaptor.forClass(WorkspaceMember.class);
        verify(workspaceMemberRepository).save(memberCaptor.capture());
        assertThat(memberCaptor.getValue().getRole()).isEqualTo(WorkspaceRole.OWNER);
        assertThat(memberCaptor.getValue().getUser()).isEqualTo(testUser);

        // Verify preferences updated
        verify(userPreferencesRepository).save(prefs);
        assertThat(prefs.getDefaultWorkspace()).isNotNull();

        verify(auditService).recordEvent(any());
    }

    @Test
    @DisplayName("Create workspace auto-resolves slug collision")
    void testCreateWorkspaceDuplicateSlugCollision() {
        CreateWorkspaceRequest request = new CreateWorkspaceRequest("Acme Tools", "acme-tools");
        when(workspaceRepository.existsBySlug("acme-tools")).thenReturn(true);
        when(workspaceRepository.existsBySlug(argThat(s -> s.startsWith("acme-tools-")))).thenReturn(false);

        when(workspaceRepository.save(any(Workspace.class))).thenAnswer(invocation -> {
            Workspace ws = invocation.getArgument(0);
            ws.setId(UUID.randomUUID());
            return ws;
        });

        when(userPreferencesRepository.findByUserId(userId)).thenReturn(Optional.empty());

        WorkspaceResponse response = workspaceService.createWorkspace(testUser, request);

        assertThat(response).isNotNull();
        assertThat(response.slug()).startsWith("acme-tools-");
    }

    @Test
    @DisplayName("Get user workspaces returns all active memberships")
    void testGetUserWorkspaces() {
        Workspace ws1 = new Workspace("Ws 1", "ws-1", testUser);
        ws1.setId(UUID.randomUUID());
        WorkspaceMember m1 = new WorkspaceMember(ws1, testUser, WorkspaceRole.OWNER, null);

        Workspace ws2 = new Workspace("Ws 2", "ws-2", testUser);
        ws2.setId(UUID.randomUUID());
        WorkspaceMember m2 = new WorkspaceMember(ws2, testUser, WorkspaceRole.MEMBER, null);

        when(workspaceMemberRepository.findByUserIdWithWorkspace(userId)).thenReturn(List.of(m1, m2));

        List<WorkspaceSummaryResponse> result = workspaceService.getUserWorkspaces(testUser);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).role()).isEqualTo("OWNER");
        assertThat(result.get(1).role()).isEqualTo("MEMBER");
    }

    @Test
    @DisplayName("Get workspace details fails if user is not a member")
    void testGetWorkspaceDetailsNotMember() {
        UUID wsId = UUID.randomUUID();
        Workspace ws = new Workspace("Target Ws", "target-ws", testUser);
        ws.setId(wsId);
        when(workspaceRepository.findById(wsId)).thenReturn(Optional.of(ws));
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(wsId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> workspaceService.getWorkspaceDetails(wsId, testUser))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("not a member");
    }

    @Test
    @DisplayName("Update workspace name succeeds for ADMIN or OWNER")
    void testUpdateWorkspaceAdmin() {
        UUID wsId = UUID.randomUUID();
        Workspace ws = new Workspace("Old Name", "old-name", testUser);
        ws.setId(wsId);
        WorkspaceMember member = new WorkspaceMember(ws, testUser, WorkspaceRole.ADMIN, null);

        when(workspaceRepository.findById(wsId)).thenReturn(Optional.of(ws));
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(wsId, userId)).thenReturn(Optional.of(member));
        when(workspaceRepository.save(any(Workspace.class))).thenAnswer(i -> i.getArgument(0));
        when(workspaceMemberRepository.countByWorkspaceId(wsId)).thenReturn(3L);

        WorkspaceResponse response = workspaceService.updateWorkspace(wsId, new UpdateWorkspaceRequest("New Name"), testUser);

        assertThat(response.name()).isEqualTo("New Name");
        verify(auditService).recordEvent(any());
    }

    @Test
    @DisplayName("Update workspace name is rejected for ordinary MEMBER")
    void testUpdateWorkspaceMemberRejected() {
        UUID wsId = UUID.randomUUID();
        Workspace ws = new Workspace("Old Name", "old-name", testUser);
        ws.setId(wsId);
        WorkspaceMember member = new WorkspaceMember(ws, testUser, WorkspaceRole.MEMBER, null);

        when(workspaceRepository.findById(wsId)).thenReturn(Optional.of(ws));
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(wsId, userId)).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> workspaceService.updateWorkspace(wsId, new UpdateWorkspaceRequest("New Name"), testUser))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only ADMIN or OWNER");
    }

    @Test
    @DisplayName("Archive workspace succeeds for OWNER")
    void testArchiveWorkspaceOwner() {
        UUID wsId = UUID.randomUUID();
        Workspace ws = new Workspace("Target Ws", "target-ws", testUser);
        ws.setId(wsId);
        WorkspaceMember ownerMember = new WorkspaceMember(ws, testUser, WorkspaceRole.OWNER, null);

        when(workspaceRepository.findById(wsId)).thenReturn(Optional.of(ws));
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(wsId, userId)).thenReturn(Optional.of(ownerMember));
        when(workspaceRepository.save(any(Workspace.class))).thenAnswer(i -> i.getArgument(0));

        WorkspaceResponse response = workspaceService.archiveWorkspace(wsId, testUser);

        assertThat(response.status()).isEqualTo("ARCHIVED");
        verify(auditService).recordEvent(any());
    }

    @Test
    @DisplayName("Archive workspace is rejected for non-OWNER")
    void testArchiveWorkspaceNonOwnerRejected() {
        UUID wsId = UUID.randomUUID();
        Workspace ws = new Workspace("Target Ws", "target-ws", testUser);
        ws.setId(wsId);
        WorkspaceMember adminMember = new WorkspaceMember(ws, testUser, WorkspaceRole.ADMIN, null);

        when(workspaceRepository.findById(wsId)).thenReturn(Optional.of(ws));
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(wsId, userId)).thenReturn(Optional.of(adminMember));

        assertThatThrownBy(() -> workspaceService.archiveWorkspace(wsId, testUser))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only the workspace OWNER");
    }

    @Test
    @DisplayName("Transfer ownership swaps OWNER and ADMIN roles")
    void testTransferOwnership() {
        UUID wsId = UUID.randomUUID();
        Workspace ws = new Workspace("Target Ws", "target-ws", testUser);
        ws.setId(wsId);

        User newOwner = new User(UUID.randomUUID(), "newowner@rightfull.com", "New Owner", null);
        UUID newOwnerId = UUID.randomUUID();
        newOwner.setId(newOwnerId);

        WorkspaceMember currentOwnerMember = new WorkspaceMember(ws, testUser, WorkspaceRole.OWNER, null);
        WorkspaceMember newOwnerMember = new WorkspaceMember(ws, newOwner, WorkspaceRole.ADMIN, null);

        when(workspaceRepository.findById(wsId)).thenReturn(Optional.of(ws));
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(wsId, userId)).thenReturn(Optional.of(currentOwnerMember));
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(wsId, newOwnerId)).thenReturn(Optional.of(newOwnerMember));
        when(workspaceRepository.save(any(Workspace.class))).thenAnswer(i -> i.getArgument(0));

        WorkspaceResponse response = workspaceService.transferOwnership(wsId, new TransferOwnershipRequest(newOwnerId), testUser);

        assertThat(currentOwnerMember.getRole()).isEqualTo(WorkspaceRole.ADMIN);
        assertThat(newOwnerMember.getRole()).isEqualTo(WorkspaceRole.OWNER);
        assertThat(ws.getOwner()).isEqualTo(newOwner);
        verify(auditService).recordEvent(any());
    }
}

