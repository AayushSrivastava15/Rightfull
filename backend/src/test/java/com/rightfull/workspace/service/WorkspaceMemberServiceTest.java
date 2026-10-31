package com.rightfull.workspace.service;

import com.rightfull.audit.WorkspaceAuditService;
import com.rightfull.common.exception.BusinessException;
import com.rightfull.common.exception.ResourceNotFoundException;
import com.rightfull.identity.entity.User;
import com.rightfull.identity.entity.UserPreferences;
import com.rightfull.identity.repository.UserPreferencesRepository;
import com.rightfull.workspace.dto.UpdateMemberRoleRequest;
import com.rightfull.workspace.dto.WorkspaceMemberResponse;
import com.rightfull.workspace.entity.Workspace;
import com.rightfull.workspace.entity.WorkspaceMember;
import com.rightfull.workspace.entity.WorkspaceRole;
import com.rightfull.workspace.repository.WorkspaceMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class WorkspaceMemberServiceTest {

    @Mock
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Mock
    private UserPreferencesRepository userPreferencesRepository;

    @Mock
    private WorkspaceAuditService auditService;

    private WorkspaceMemberService memberService;

    private Workspace workspace;
    private UUID workspaceId;
    private User actorOwner;
    private User actorAdmin;
    private User targetMemberUser;
    private UUID memberId;

    @BeforeEach
    void setUp() {
        memberService = new WorkspaceMemberService(
                workspaceMemberRepository,
                userPreferencesRepository,
                auditService
        );

        workspaceId = UUID.randomUUID();
        actorOwner = new User(UUID.randomUUID(), "owner@rightfull.com", "Owner", null);
        actorOwner.setId(UUID.randomUUID());

        workspace = new Workspace("Test Workspace", "test-ws", actorOwner);
        workspace.setId(workspaceId);

        actorAdmin = new User(UUID.randomUUID(), "admin@rightfull.com", "Admin", null);
        actorAdmin.setId(UUID.randomUUID());

        targetMemberUser = new User(UUID.randomUUID(), "member@rightfull.com", "Member", null);
        targetMemberUser.setId(UUID.randomUUID());

        memberId = UUID.randomUUID();
    }

    @Test
    @DisplayName("List workspace members succeeds for VIEWER role")
    void testGetWorkspaceMembers() {
        WorkspaceMember m1 = new WorkspaceMember(workspace, actorOwner, WorkspaceRole.OWNER, null);
        WorkspaceMember m2 = new WorkspaceMember(workspace, targetMemberUser, WorkspaceRole.MEMBER, null);

        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, actorOwner.getId()))
                .thenReturn(Optional.of(m1));
        when(workspaceMemberRepository.findByWorkspaceIdWithUser(workspaceId))
                .thenReturn(List.of(m1, m2));

        List<WorkspaceMemberResponse> result = memberService.getWorkspaceMembers(workspaceId, actorOwner);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).email()).isEqualTo("owner@rightfull.com");
    }

    @Test
    @DisplayName("Role change to OWNER is rejected (must use transfer-ownership)")
    void testCannotSetRoleToOwner() {
        WorkspaceMember adminMember = new WorkspaceMember(workspace, actorAdmin, WorkspaceRole.ADMIN, null);
        WorkspaceMember targetMember = new WorkspaceMember(workspace, targetMemberUser, WorkspaceRole.MEMBER, null);
        targetMember.setId(memberId);

        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, actorAdmin.getId()))
                .thenReturn(Optional.of(adminMember));
        when(workspaceMemberRepository.findByWorkspaceIdAndId(workspaceId, memberId))
                .thenReturn(Optional.of(targetMember));

        assertThatThrownBy(() -> memberService.updateMemberRole(workspaceId, memberId, new UpdateMemberRoleRequest(WorkspaceRole.OWNER), actorAdmin))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Role cannot be set to OWNER directly");
    }

    @Test
    @DisplayName("Role change of an existing OWNER is rejected")
    void testCannotModifyOwnerRole() {
        WorkspaceMember adminMember = new WorkspaceMember(workspace, actorAdmin, WorkspaceRole.ADMIN, null);
        WorkspaceMember ownerMember = new WorkspaceMember(workspace, actorOwner, WorkspaceRole.OWNER, null);
        ownerMember.setId(memberId);

        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, actorAdmin.getId()))
                .thenReturn(Optional.of(adminMember));
        when(workspaceMemberRepository.findByWorkspaceIdAndId(workspaceId, memberId))
                .thenReturn(Optional.of(ownerMember));

        assertThatThrownBy(() -> memberService.updateMemberRole(workspaceId, memberId, new UpdateMemberRoleRequest(WorkspaceRole.MEMBER), actorAdmin))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Cannot modify the role of the workspace OWNER");
    }

    @Test
    @DisplayName("ADMIN modifying peer ADMIN is rejected (anti-escalation)")
    void testAdminCannotModifyPeerAdmin() {
        WorkspaceMember admin1 = new WorkspaceMember(workspace, actorAdmin, WorkspaceRole.ADMIN, null);
        User admin2User = new User(UUID.randomUUID(), "admin2@rightfull.com", "Admin 2", null);
        admin2User.setId(UUID.randomUUID());
        WorkspaceMember admin2 = new WorkspaceMember(workspace, admin2User, WorkspaceRole.ADMIN, null);
        admin2.setId(memberId);

        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, actorAdmin.getId()))
                .thenReturn(Optional.of(admin1));
        when(workspaceMemberRepository.findByWorkspaceIdAndId(workspaceId, memberId))
                .thenReturn(Optional.of(admin2));

        assertThatThrownBy(() -> memberService.updateMemberRole(workspaceId, memberId, new UpdateMemberRoleRequest(WorkspaceRole.VIEWER), actorAdmin))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("ADMIN cannot modify the role of another ADMIN");
    }

    @Test
    @DisplayName("ADMIN promoting member to ADMIN is rejected (only OWNER can promote to ADMIN)")
    void testAdminCannotPromoteToAdmin() {
        WorkspaceMember admin = new WorkspaceMember(workspace, actorAdmin, WorkspaceRole.ADMIN, null);
        WorkspaceMember member = new WorkspaceMember(workspace, targetMemberUser, WorkspaceRole.MEMBER, null);
        member.setId(memberId);

        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, actorAdmin.getId()))
                .thenReturn(Optional.of(admin));
        when(workspaceMemberRepository.findByWorkspaceIdAndId(workspaceId, memberId))
                .thenReturn(Optional.of(member));

        assertThatThrownBy(() -> memberService.updateMemberRole(workspaceId, memberId, new UpdateMemberRoleRequest(WorkspaceRole.ADMIN), actorAdmin))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only workspace OWNER can promote members to ADMIN");
    }

    @Test
    @DisplayName("OWNER can promote member to ADMIN")
    void testOwnerCanPromoteToAdmin() {
        WorkspaceMember owner = new WorkspaceMember(workspace, actorOwner, WorkspaceRole.OWNER, null);
        WorkspaceMember member = new WorkspaceMember(workspace, targetMemberUser, WorkspaceRole.MEMBER, null);
        member.setId(memberId);

        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, actorOwner.getId()))
                .thenReturn(Optional.of(owner));
        when(workspaceMemberRepository.findByWorkspaceIdAndId(workspaceId, memberId))
                .thenReturn(Optional.of(member));
        when(workspaceMemberRepository.save(any(WorkspaceMember.class))).thenAnswer(i -> i.getArgument(0));

        WorkspaceMemberResponse response = memberService.updateMemberRole(workspaceId, memberId, new UpdateMemberRoleRequest(WorkspaceRole.ADMIN), actorOwner);

        assertThat(response.role()).isEqualTo(WorkspaceRole.ADMIN);
        verify(auditService).recordEvent(any());
    }

    @Test
    @DisplayName("Removal of the last OWNER is rejected (last OWNER protection)")
    void testCannotRemoveLastOwner() {
        WorkspaceMember owner = new WorkspaceMember(workspace, actorOwner, WorkspaceRole.OWNER, null);
        owner.setId(memberId);

        when(workspaceMemberRepository.findByWorkspaceIdAndId(workspaceId, memberId))
                .thenReturn(Optional.of(owner));
        when(workspaceMemberRepository.countByWorkspaceIdAndRole(workspaceId, WorkspaceRole.OWNER))
                .thenReturn(1L);

        assertThatThrownBy(() -> memberService.removeMember(workspaceId, memberId, actorOwner))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Cannot remove the last workspace OWNER");
    }

    @Test
    @DisplayName("OWNER can remove an ADMIN member")
    void testOwnerCanRemoveAdmin() {
        WorkspaceMember owner = new WorkspaceMember(workspace, actorOwner, WorkspaceRole.OWNER, null);
        WorkspaceMember admin = new WorkspaceMember(workspace, actorAdmin, WorkspaceRole.ADMIN, null);
        admin.setId(memberId);

        when(workspaceMemberRepository.findByWorkspaceIdAndId(workspaceId, memberId))
                .thenReturn(Optional.of(admin));
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, actorOwner.getId()))
                .thenReturn(Optional.of(owner));
        when(userPreferencesRepository.findByUserId(actorAdmin.getId()))
                .thenReturn(Optional.empty());

        memberService.removeMember(workspaceId, memberId, actorOwner);

        verify(workspaceMemberRepository).delete(admin);
        verify(auditService).recordEvent(any());
    }

    @Test
    @DisplayName("ADMIN cannot remove another ADMIN or OWNER")
    void testAdminCannotRemoveAdminOrOwner() {
        WorkspaceMember actorAdminMember = new WorkspaceMember(workspace, actorAdmin, WorkspaceRole.ADMIN, null);
        User peerAdminUser = new User(UUID.randomUUID(), "peer@rightfull.com", "Peer Admin", null);
        peerAdminUser.setId(UUID.randomUUID());
        WorkspaceMember peerAdmin = new WorkspaceMember(workspace, peerAdminUser, WorkspaceRole.ADMIN, null);
        peerAdmin.setId(memberId);

        when(workspaceMemberRepository.findByWorkspaceIdAndId(workspaceId, memberId))
                .thenReturn(Optional.of(peerAdmin));
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, actorAdmin.getId()))
                .thenReturn(Optional.of(actorAdminMember));

        assertThatThrownBy(() -> memberService.removeMember(workspaceId, memberId, actorAdmin))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("ADMIN cannot remove another ADMIN or an OWNER");
    }
}

