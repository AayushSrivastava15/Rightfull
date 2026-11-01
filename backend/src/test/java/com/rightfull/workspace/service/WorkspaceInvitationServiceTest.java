package com.rightfull.workspace.service;

import com.rightfull.audit.WorkspaceAuditService;
import com.rightfull.common.exception.BusinessException;
import com.rightfull.common.exception.ResourceNotFoundException;
import com.rightfull.common.util.CryptoUtils;
import com.rightfull.identity.entity.User;
import com.rightfull.identity.repository.UserPreferencesRepository;
import com.rightfull.identity.repository.UserRepository;
import com.rightfull.notification.EmailService;
import com.rightfull.workspace.dto.CreateInvitationRequest;
import com.rightfull.workspace.dto.WorkspaceInvitationResponse;
import com.rightfull.workspace.entity.Workspace;
import com.rightfull.workspace.entity.WorkspaceInvitation;
import com.rightfull.workspace.entity.WorkspaceInvitationStatus;
import com.rightfull.workspace.entity.WorkspaceMember;
import com.rightfull.workspace.entity.WorkspaceRole;
import com.rightfull.workspace.repository.WorkspaceInvitationRepository;
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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkspaceInvitationServiceTest {

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Mock
    private WorkspaceInvitationRepository workspaceInvitationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserPreferencesRepository userPreferencesRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private WorkspaceAuditService auditService;

    @Mock
    private com.rightfull.security.tenant.TenantContextManager tenantContextManager;

    private WorkspaceInvitationService invitationService;

    private Workspace workspace;
    private UUID workspaceId;
    private User actorAdmin;

    @BeforeEach
    void setUp() {
        invitationService = new WorkspaceInvitationService(
                workspaceRepository,
                workspaceMemberRepository,
                workspaceInvitationRepository,
                userRepository,
                userPreferencesRepository,
                emailService,
                auditService,
                tenantContextManager
        );

        workspaceId = UUID.randomUUID();
        actorAdmin = new User(UUID.randomUUID(), "admin@rightfull.com", "Admin Lead", null);
        actorAdmin.setId(UUID.randomUUID());

        workspace = new Workspace("Rightful Platform", "rightful-platform", actorAdmin);
        workspace.setId(workspaceId);
    }

    @Test
    @DisplayName("Admin can issue invitation with secure token hash and email dispatch")
    void testCreateInvitationSuccess() {
        WorkspaceMember adminMember = new WorkspaceMember(workspace, actorAdmin, WorkspaceRole.ADMIN, null);
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, actorAdmin.getId()))
                .thenReturn(Optional.of(adminMember));
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));
        when(userRepository.findByEmail("invitee@company.com")).thenReturn(Optional.empty());
        when(workspaceInvitationRepository.findByWorkspaceIdAndEmailAndStatus(workspaceId, "invitee@company.com", WorkspaceInvitationStatus.PENDING))
                .thenReturn(Optional.empty());

        when(workspaceInvitationRepository.save(any(WorkspaceInvitation.class))).thenAnswer(i -> {
            WorkspaceInvitation inv = i.getArgument(0);
            inv.setId(UUID.randomUUID());
            return inv;
        });

        CreateInvitationRequest req = new CreateInvitationRequest("invitee@company.com", WorkspaceRole.MEMBER);
        WorkspaceInvitationResponse response = invitationService.createInvitation(workspaceId, req, actorAdmin);

        assertThat(response).isNotNull();
        assertThat(response.email()).isEqualTo("invitee@company.com");
        assertThat(response.role()).isEqualTo(WorkspaceRole.MEMBER);
        assertThat(response.invitationToken()).isNotNull();
        assertThat(response.invitationToken()).hasSize(64); // 32 bytes hex

        // Verify token_hash stored is SHA-256 of the raw token
        ArgumentCaptor<WorkspaceInvitation> captor = ArgumentCaptor.forClass(WorkspaceInvitation.class);
        verify(workspaceInvitationRepository).save(captor.capture());
        String expectedHash = CryptoUtils.sha256Hex(response.invitationToken());
        assertThat(captor.getValue().getTokenHash()).isEqualTo(expectedHash);

        verify(emailService).sendInvitationEmail(eq("invitee@company.com"), eq("Rightful Platform"), eq("MEMBER"), any(), any());
        verify(auditService).recordEvent(any());
    }

    @Test
    @DisplayName("Invitation cannot grant OWNER privileges")
    void testCannotInviteAsOwner() {
        WorkspaceMember adminMember = new WorkspaceMember(workspace, actorAdmin, WorkspaceRole.ADMIN, null);
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, actorAdmin.getId()))
                .thenReturn(Optional.of(adminMember));

        CreateInvitationRequest req = new CreateInvitationRequest("invitee@company.com", WorkspaceRole.OWNER);

        assertThatThrownBy(() -> invitationService.createInvitation(workspaceId, req, actorAdmin))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invitations cannot grant OWNER role");
    }

    @Test
    @DisplayName("Cannot invite user who is already an active member")
    void testCannotInviteExistingMember() {
        WorkspaceMember adminMember = new WorkspaceMember(workspace, actorAdmin, WorkspaceRole.ADMIN, null);
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, actorAdmin.getId()))
                .thenReturn(Optional.of(adminMember));
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));

        User existingUser = new User(UUID.randomUUID(), "existing@rightfull.com", "Existing", null);
        existingUser.setId(UUID.randomUUID());
        when(userRepository.findByEmail("existing@rightfull.com")).thenReturn(Optional.of(existingUser));
        when(workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, existingUser.getId())).thenReturn(true);

        CreateInvitationRequest req = new CreateInvitationRequest("existing@rightfull.com", WorkspaceRole.MEMBER);

        assertThatThrownBy(() -> invitationService.createInvitation(workspaceId, req, actorAdmin))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("User is already a member");
    }

    @Test
    @DisplayName("Accept invitation succeeds and creates membership when email matches")
    void testAcceptInvitationSuccess() {
        String rawToken = CryptoUtils.generateSecureToken(32);
        String tokenHash = CryptoUtils.sha256Hex(rawToken);

        User inviteeUser = new User(UUID.randomUUID(), "jane@developer.com", "Jane Doe", null);
        inviteeUser.setId(UUID.randomUUID());

        Instant future = Instant.now().plus(5, ChronoUnit.DAYS);
        WorkspaceInvitation invitation = new WorkspaceInvitation(
                workspace,
                "jane@developer.com",
                WorkspaceRole.MEMBER,
                tokenHash,
                actorAdmin,
                future
        );
        invitation.setId(UUID.randomUUID());

        when(workspaceInvitationRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(invitation));
        when(workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, inviteeUser.getId())).thenReturn(false);
        when(workspaceInvitationRepository.save(any(WorkspaceInvitation.class))).thenAnswer(i -> i.getArgument(0));
        when(userPreferencesRepository.findByUserId(inviteeUser.getId())).thenReturn(Optional.empty());

        WorkspaceInvitationResponse response = invitationService.acceptInvitation(rawToken, inviteeUser);

        assertThat(response).isNotNull();
        assertThat(invitation.getStatus()).isEqualTo(WorkspaceInvitationStatus.ACCEPTED);
        assertThat(invitation.getAcceptedAt()).isNotNull();

        verify(workspaceMemberRepository).save(any(WorkspaceMember.class));
        verify(auditService).recordEvent(any());
    }

    @Test
    @DisplayName("Accept invitation rejected when authenticated email does not match invitation recipient")
    void testAcceptInvitationEmailMismatch() {
        String rawToken = CryptoUtils.generateSecureToken(32);
        String tokenHash = CryptoUtils.sha256Hex(rawToken);

        User imposterUser = new User(UUID.randomUUID(), "imposter@attacker.com", "Attacker", null);
        imposterUser.setId(UUID.randomUUID());

        Instant future = Instant.now().plus(5, ChronoUnit.DAYS);
        WorkspaceInvitation invitation = new WorkspaceInvitation(
                workspace,
                "legitimate@developer.com",
                WorkspaceRole.MEMBER,
                tokenHash,
                actorAdmin,
                future
        );

        when(workspaceInvitationRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(invitation));

        assertThatThrownBy(() -> invitationService.acceptInvitation(rawToken, imposterUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invitation was issued to a different email address");
    }

    @Test
    @DisplayName("Accept invitation rejected when invitation has expired")
    void testAcceptInvitationExpired() {
        String rawToken = CryptoUtils.generateSecureToken(32);
        String tokenHash = CryptoUtils.sha256Hex(rawToken);

        User inviteeUser = new User(UUID.randomUUID(), "jane@developer.com", "Jane Doe", null);
        inviteeUser.setId(UUID.randomUUID());

        Instant past = Instant.now().minus(2, ChronoUnit.DAYS);
        WorkspaceInvitation invitation = new WorkspaceInvitation(
                workspace,
                "jane@developer.com",
                WorkspaceRole.MEMBER,
                tokenHash,
                actorAdmin,
                past
        );

        when(workspaceInvitationRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(invitation));

        assertThatThrownBy(() -> invitationService.acceptInvitation(rawToken, inviteeUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invitation has expired");
    }
}

