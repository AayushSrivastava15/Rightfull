package com.rightfull.workspace.service;

import com.rightfull.audit.WorkspaceAuditEvent;
import com.rightfull.audit.WorkspaceAuditEventType;
import com.rightfull.audit.WorkspaceAuditService;
import com.rightfull.common.exception.BusinessException;
import com.rightfull.common.exception.ResourceNotFoundException;
import com.rightfull.common.util.CryptoUtils;
import com.rightfull.identity.entity.User;
import com.rightfull.identity.entity.UserPreferences;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service managing secure workspace invitations, cryptographically hashed tokens, and accept/reject flows.
 */
@Service
public class WorkspaceInvitationService {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceInvitationService.class);
    private static final int INVITATION_EXPIRY_DAYS = 7;

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final WorkspaceInvitationRepository workspaceInvitationRepository;
    private final UserRepository userRepository;
    private final UserPreferencesRepository userPreferencesRepository;
    private final EmailService emailService;
    private final WorkspaceAuditService auditService;

    public WorkspaceInvitationService(
            WorkspaceRepository workspaceRepository,
            WorkspaceMemberRepository workspaceMemberRepository,
            WorkspaceInvitationRepository workspaceInvitationRepository,
            UserRepository userRepository,
            UserPreferencesRepository userPreferencesRepository,
            EmailService emailService,
            WorkspaceAuditService auditService) {
        this.workspaceRepository = workspaceRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.workspaceInvitationRepository = workspaceInvitationRepository;
        this.userRepository = userRepository;
        this.userPreferencesRepository = userPreferencesRepository;
        this.emailService = emailService;
        this.auditService = auditService;
    }

    /**
     * Issues a cryptographically secure invitation to join a workspace.
     */
    @Transactional
    public WorkspaceInvitationResponse createInvitation(UUID workspaceId, CreateInvitationRequest request, User actor) {
        WorkspaceMember actorMember = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, actor.getId())
                .orElseThrow(() -> new AccessDeniedException("User is not a member of this workspace"));

        if (!actorMember.getRole().hasAtLeast(WorkspaceRole.ADMIN)) {
            throw new AccessDeniedException("Only ADMIN or OWNER can invite new members");
        }

        if (request.role() == WorkspaceRole.OWNER) {
            throw new BusinessException(
                    "Invitations cannot grant OWNER role. Workspace must be transferred by current owner",
                    "INVALID_INVITATION_ROLE",
                    HttpStatus.BAD_REQUEST
            );
        }

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace", "id", workspaceId));

        String email = request.email().toLowerCase().trim();

        // Prevent inviting existing members
        Optional<User> existingUserOpt = userRepository.findByEmail(email);
        if (existingUserOpt.isPresent()) {
            boolean alreadyMember = workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, existingUserOpt.get().getId());
            if (alreadyMember) {
                throw new BusinessException("User is already a member of this workspace", "ALREADY_MEMBER", HttpStatus.CONFLICT);
            }
        }

        // Prevent duplicate pending invitations
        Optional<WorkspaceInvitation> existingPendingOpt = workspaceInvitationRepository
                .findByWorkspaceIdAndEmailAndStatus(workspaceId, email, WorkspaceInvitationStatus.PENDING);

        if (existingPendingOpt.isPresent()) {
            WorkspaceInvitation pending = existingPendingOpt.get();
            if (!pending.isExpired()) {
                throw new BusinessException(
                        "An active pending invitation already exists for this email",
                        "DUPLICATE_INVITATION",
                        HttpStatus.CONFLICT
                );
            } else {
                pending.setStatus(WorkspaceInvitationStatus.REVOKED);
                workspaceInvitationRepository.save(pending);
            }
        }

        // Cryptographically secure token (32 bytes = 64 hex characters)
        String rawToken = CryptoUtils.generateSecureToken(32);
        String tokenHash = CryptoUtils.sha256Hex(rawToken);
        Instant expiresAt = Instant.now().plus(INVITATION_EXPIRY_DAYS, ChronoUnit.DAYS);

        WorkspaceInvitation invitation = new WorkspaceInvitation(
                workspace,
                email,
                request.role(),
                tokenHash,
                actor,
                expiresAt
        );

        WorkspaceInvitation saved = workspaceInvitationRepository.save(invitation);

        emailService.sendInvitationEmail(email, workspace.getName(), request.role().name(), rawToken, expiresAt);

        auditService.recordEvent(WorkspaceAuditEvent.of(
                WorkspaceAuditEventType.MEMBER_INVITED,
                workspaceId,
                actor.getId(),
                null,
                Map.of("inviteeEmail", email, "role", request.role().name(), "invitationId", saved.getId())
        ));

        log.info("Invitation id={} created for email='{}' in workspace id={} by actor={}",
                saved.getId(), email, workspaceId, actor.getId());

        return toInvitationResponse(saved, rawToken);
    }

    /**
     * Lists active pending invitations for a workspace. Restricted to ADMIN / OWNER.
     */
    @Transactional(readOnly = true)
    public List<WorkspaceInvitationResponse> getPendingInvitations(UUID workspaceId, User actor) {
        WorkspaceMember actorMember = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, actor.getId())
                .orElseThrow(() -> new AccessDeniedException("User is not a member of this workspace"));

        if (!actorMember.getRole().hasAtLeast(WorkspaceRole.ADMIN)) {
            throw new AccessDeniedException("Only ADMIN or OWNER can view workspace invitations");
        }

        List<WorkspaceInvitation> invitations = workspaceInvitationRepository
                .findByWorkspaceIdAndStatus(workspaceId, WorkspaceInvitationStatus.PENDING);

        return invitations.stream()
                .map(inv -> toInvitationResponse(inv, null))
                .toList();
    }

    /**
     * Revokes a pending workspace invitation. Restricted to ADMIN / OWNER.
     */
    @Transactional
    public void revokeInvitation(UUID workspaceId, UUID invitationId, User actor) {
        WorkspaceMember actorMember = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, actor.getId())
                .orElseThrow(() -> new AccessDeniedException("User is not a member of this workspace"));

        if (!actorMember.getRole().hasAtLeast(WorkspaceRole.ADMIN)) {
            throw new AccessDeniedException("Only ADMIN or OWNER can revoke invitations");
        }

        WorkspaceInvitation invitation = workspaceInvitationRepository.findByWorkspaceIdAndId(workspaceId, invitationId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkspaceInvitation", "id", invitationId));

        if (invitation.getStatus() != WorkspaceInvitationStatus.PENDING) {
            throw new BusinessException("Only pending invitations can be revoked", "INVITATION_NOT_PENDING", HttpStatus.BAD_REQUEST);
        }

        invitation.setStatus(WorkspaceInvitationStatus.REVOKED);
        workspaceInvitationRepository.save(invitation);

        auditService.recordEvent(WorkspaceAuditEvent.of(
                WorkspaceAuditEventType.INVITATION_REVOKED,
                workspaceId,
                actor.getId(),
                null,
                Map.of("invitationId", invitationId, "inviteeEmail", invitation.getEmail())
        ));

        log.info("Invitation id={} revoked in workspace id={} by actor={}", invitationId, workspaceId, actor.getId());
    }

    /**
     * Accepts a workspace invitation using the unhashed token.
     * Enforces single-use, non-expired status, and email binding with the authenticated user.
     */
    @Transactional
    public WorkspaceInvitationResponse acceptInvitation(String rawToken, User authenticatedUser) {
        String tokenHash = CryptoUtils.sha256Hex(rawToken);

        WorkspaceInvitation invitation = workspaceInvitationRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid or non-existent invitation token"));

        if (invitation.getStatus() != WorkspaceInvitationStatus.PENDING) {
            throw new BusinessException(
                    "Invitation is no longer pending (" + invitation.getStatus() + ")",
                    "INVITATION_INACTIVE",
                    HttpStatus.GONE
            );
        }

        if (invitation.isExpired()) {
            invitation.setStatus(WorkspaceInvitationStatus.REVOKED);
            workspaceInvitationRepository.save(invitation);
            throw new BusinessException("Invitation has expired", "INVITATION_EXPIRED", HttpStatus.GONE);
        }

        // Email binding validation
        if (!authenticatedUser.getEmail().equalsIgnoreCase(invitation.getEmail())) {
            log.warn("Invitation acceptance rejected: Authenticated user email='{}' does not match invitation email='{}'",
                    authenticatedUser.getEmail(), invitation.getEmail());
            throw new BusinessException(
                    "Invitation was issued to a different email address (" + invitation.getEmail() + ")",
                    "EMAIL_MISMATCH",
                    HttpStatus.FORBIDDEN
            );
        }

        Workspace workspace = invitation.getWorkspace();

        // Add member if not already joined
        if (!workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspace.getId(), authenticatedUser.getId())) {
            WorkspaceMember member = new WorkspaceMember(
                    workspace,
                    authenticatedUser,
                    invitation.getRole(),
                    invitation.getInvitedBy()
            );
            workspaceMemberRepository.save(member);
        }

        invitation.setStatus(WorkspaceInvitationStatus.ACCEPTED);
        invitation.setAcceptedAt(Instant.now());
        WorkspaceInvitation updated = workspaceInvitationRepository.save(invitation);

        // Set default workspace if user doesn't have one
        Optional<UserPreferences> prefsOpt = userPreferencesRepository.findByUserId(authenticatedUser.getId());
        if (prefsOpt.isPresent() && prefsOpt.get().getDefaultWorkspace() == null) {
            UserPreferences prefs = prefsOpt.get();
            prefs.setDefaultWorkspace(workspace);
            userPreferencesRepository.save(prefs);
        }

        auditService.recordEvent(WorkspaceAuditEvent.of(
                WorkspaceAuditEventType.INVITATION_ACCEPTED,
                workspace.getId(),
                authenticatedUser.getId(),
                invitation.getInvitedBy().getId(),
                Map.of("role", invitation.getRole().name(), "invitationId", invitation.getId())
        ));

        log.info("Invitation id={} accepted by user id={} ({}) in workspace id={}",
                invitation.getId(), authenticatedUser.getId(), authenticatedUser.getEmail(), workspace.getId());

        return toInvitationResponse(updated, null);
    }

    /**
     * Rejects an invitation using the unhashed token.
     */
    @Transactional
    public WorkspaceInvitationResponse rejectInvitation(String rawToken, User authenticatedUser) {
        String tokenHash = CryptoUtils.sha256Hex(rawToken);

        WorkspaceInvitation invitation = workspaceInvitationRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid or non-existent invitation token"));

        if (invitation.getStatus() != WorkspaceInvitationStatus.PENDING) {
            throw new BusinessException(
                    "Invitation is no longer pending (" + invitation.getStatus() + ")",
                    "INVITATION_INACTIVE",
                    HttpStatus.GONE
            );
        }

        invitation.setStatus(WorkspaceInvitationStatus.REJECTED);
        WorkspaceInvitation updated = workspaceInvitationRepository.save(invitation);

        auditService.recordEvent(WorkspaceAuditEvent.of(
                WorkspaceAuditEventType.INVITATION_REJECTED,
                invitation.getWorkspace().getId(),
                authenticatedUser != null ? authenticatedUser.getId() : null,
                invitation.getInvitedBy().getId(),
                Map.of("invitationId", invitation.getId())
        ));

        log.info("Invitation id={} rejected", invitation.getId());

        return toInvitationResponse(updated, null);
    }

    private WorkspaceInvitationResponse toInvitationResponse(WorkspaceInvitation inv, String rawToken) {
        return new WorkspaceInvitationResponse(
                inv.getId(),
                inv.getWorkspace().getId(),
                inv.getWorkspace().getName(),
                inv.getEmail(),
                inv.getRole(),
                inv.getStatus(),
                inv.getInvitedBy().getEmail(),
                inv.getExpiresAt(),
                inv.getCreatedAt(),
                rawToken
        );
    }
}

