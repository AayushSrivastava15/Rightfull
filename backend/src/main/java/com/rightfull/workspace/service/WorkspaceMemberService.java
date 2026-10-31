package com.rightfull.workspace.service;

import com.rightfull.audit.WorkspaceAuditEvent;
import com.rightfull.audit.WorkspaceAuditEventType;
import com.rightfull.audit.WorkspaceAuditService;
import com.rightfull.common.exception.BusinessException;
import com.rightfull.common.exception.ResourceNotFoundException;
import com.rightfull.identity.entity.User;
import com.rightfull.identity.entity.UserPreferences;
import com.rightfull.identity.repository.UserPreferencesRepository;
import com.rightfull.workspace.dto.UpdateMemberRoleRequest;
import com.rightfull.workspace.dto.WorkspaceMemberResponse;
import com.rightfull.workspace.entity.WorkspaceMember;
import com.rightfull.workspace.entity.WorkspaceRole;
import com.rightfull.workspace.repository.WorkspaceMemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service managing workspace memberships, role changes, and member removals.
 */
@Service
public class WorkspaceMemberService {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceMemberService.class);

    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserPreferencesRepository userPreferencesRepository;
    private final WorkspaceAuditService auditService;

    public WorkspaceMemberService(
            WorkspaceMemberRepository workspaceMemberRepository,
            UserPreferencesRepository userPreferencesRepository,
            WorkspaceAuditService auditService) {
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.userPreferencesRepository = userPreferencesRepository;
        this.auditService = auditService;
    }

    /**
     * Lists all members of a workspace. Accessible to members with at least VIEWER role.
     */
    @Transactional(readOnly = true)
    public List<WorkspaceMemberResponse> getWorkspaceMembers(UUID workspaceId, User actor) {
        verifyMembership(workspaceId, actor.getId(), WorkspaceRole.VIEWER);

        List<WorkspaceMember> members = workspaceMemberRepository.findByWorkspaceIdWithUser(workspaceId);
        return members.stream()
                .map(this::toMemberResponse)
                .toList();
    }

    /**
     * Retrieves details of a specific workspace member.
     */
    @Transactional(readOnly = true)
    public WorkspaceMemberResponse getMemberDetails(UUID workspaceId, UUID memberId, User actor) {
        verifyMembership(workspaceId, actor.getId(), WorkspaceRole.VIEWER);

        WorkspaceMember member = workspaceMemberRepository.findByWorkspaceIdAndId(workspaceId, memberId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkspaceMember", "id", memberId));

        return toMemberResponse(member);
    }

    /**
     * Updates a member's role with strict hierarchy and anti-escalation enforcement.
     */
    @Transactional
    public WorkspaceMemberResponse updateMemberRole(
            UUID workspaceId,
            UUID memberId,
            UpdateMemberRoleRequest request,
            User actor) {
        WorkspaceMember actorMember = verifyMembership(workspaceId, actor.getId(), WorkspaceRole.ADMIN);

        WorkspaceMember targetMember = workspaceMemberRepository.findByWorkspaceIdAndId(workspaceId, memberId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkspaceMember", "id", memberId));

        WorkspaceRole newRole = request.role();

        if (newRole == WorkspaceRole.OWNER) {
            throw new BusinessException(
                    "Role cannot be set to OWNER directly. Use the transfer-ownership endpoint",
                    "INVALID_ROLE_CHANGE",
                    HttpStatus.BAD_REQUEST
            );
        }

        if (targetMember.getRole() == WorkspaceRole.OWNER) {
            throw new BusinessException(
                    "Cannot modify the role of the workspace OWNER. Use transfer-ownership instead",
                    "CANNOT_MODIFY_OWNER",
                    HttpStatus.BAD_REQUEST
            );
        }

        // Anti-escalation & peer modification restrictions for ADMINs
        if (actorMember.getRole() == WorkspaceRole.ADMIN) {
            if (targetMember.getRole() == WorkspaceRole.ADMIN) {
                throw new AccessDeniedException("ADMIN cannot modify the role of another ADMIN");
            }
            if (newRole == WorkspaceRole.ADMIN) {
                throw new AccessDeniedException("Only workspace OWNER can promote members to ADMIN");
            }
        }

        WorkspaceRole previousRole = targetMember.getRole();
        targetMember.setRole(newRole);
        WorkspaceMember saved = workspaceMemberRepository.save(targetMember);

        auditService.recordEvent(WorkspaceAuditEvent.of(
                WorkspaceAuditEventType.MEMBER_ROLE_CHANGED,
                workspaceId,
                actor.getId(),
                targetMember.getUser().getId(),
                Map.of("previousRole", previousRole.name(), "newRole", newRole.name())
        ));

        log.info("Member id={} role updated from {} to {} in workspace={} by actor={}",
                memberId, previousRole, newRole, workspaceId, actor.getId());

        return toMemberResponse(saved);
    }

    /**
     * Removes a member from a workspace or allows a member to leave.
     * Prevents removal of the last workspace OWNER.
     */
    @Transactional
    public void removeMember(UUID workspaceId, UUID memberId, User actor) {
        WorkspaceMember targetMember = workspaceMemberRepository.findByWorkspaceIdAndId(workspaceId, memberId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkspaceMember", "id", memberId));

        boolean isSelf = targetMember.getUser().getId().equals(actor.getId());

        // Last OWNER protection
        if (targetMember.getRole() == WorkspaceRole.OWNER) {
            long ownerCount = workspaceMemberRepository.countByWorkspaceIdAndRole(workspaceId, WorkspaceRole.OWNER);
            if (ownerCount <= 1) {
                throw new BusinessException(
                        "Cannot remove the last workspace OWNER. You must transfer ownership or archive the workspace",
                        "LAST_OWNER_PROTECTION",
                        HttpStatus.BAD_REQUEST
                );
            }
        }

        if (isSelf) {
            // Self-leave: Non-owners or non-last owners can leave freely
            log.info("User id={} is voluntarily leaving workspace id={}", actor.getId(), workspaceId);
        } else {
            // Actor removing another member
            WorkspaceMember actorMember = verifyMembership(workspaceId, actor.getId(), WorkspaceRole.ADMIN);

            if (actorMember.getRole() == WorkspaceRole.ADMIN) {
                if (targetMember.getRole() == WorkspaceRole.ADMIN || targetMember.getRole() == WorkspaceRole.OWNER) {
                    throw new AccessDeniedException("ADMIN cannot remove another ADMIN or an OWNER from the workspace");
                }
            }
        }

        // Clean up user preferences if this was their default workspace
        Optional<UserPreferences> prefsOpt = userPreferencesRepository.findByUserId(targetMember.getUser().getId());
        if (prefsOpt.isPresent()) {
            UserPreferences prefs = prefsOpt.get();
            if (prefs.getDefaultWorkspace() != null && prefs.getDefaultWorkspace().getId().equals(workspaceId)) {
                prefs.setDefaultWorkspace(null);
                userPreferencesRepository.save(prefs);
            }
        }

        workspaceMemberRepository.delete(targetMember);

        auditService.recordEvent(WorkspaceAuditEvent.of(
                WorkspaceAuditEventType.MEMBER_REMOVED,
                workspaceId,
                actor.getId(),
                targetMember.getUser().getId(),
                Map.of("removedUserId", targetMember.getUser().getId(), "role", targetMember.getRole().name())
        ));

        log.info("Member id={} (user={}) removed from workspace id={} by actor={}",
                memberId, targetMember.getUser().getId(), workspaceId, actor.getId());
    }

    private WorkspaceMember verifyMembership(UUID workspaceId, UUID userId, WorkspaceRole minRequiredRole) {
        WorkspaceMember member = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new AccessDeniedException("User is not a member of this workspace"));

        if (!member.getRole().hasAtLeast(minRequiredRole)) {
            throw new AccessDeniedException(String.format(
                    "Insufficient privileges: required %s but user has %s", minRequiredRole, member.getRole()));
        }

        return member;
    }

    private WorkspaceMemberResponse toMemberResponse(WorkspaceMember m) {
        return new WorkspaceMemberResponse(
                m.getId(),
                m.getUser().getId(),
                m.getUser().getEmail(),
                m.getUser().getFullName(),
                m.getUser().getAvatarUrl(),
                m.getRole(),
                m.getJoinedAt()
        );
    }
}

