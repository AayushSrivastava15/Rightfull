package com.rightfull.workspace.service;

import com.rightfull.audit.WorkspaceAuditEvent;
import com.rightfull.audit.WorkspaceAuditEventType;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service managing workspace lifecycles, tenant boundaries, and ownership transfers.
 */
@Service
public class WorkspaceService {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceService.class);

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserPreferencesRepository userPreferencesRepository;
    private final WorkspaceAuditService auditService;

    public WorkspaceService(
            WorkspaceRepository workspaceRepository,
            WorkspaceMemberRepository workspaceMemberRepository,
            UserPreferencesRepository userPreferencesRepository,
            WorkspaceAuditService auditService) {
        this.workspaceRepository = workspaceRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.userPreferencesRepository = userPreferencesRepository;
        this.auditService = auditService;
    }

    /**
     * Creates a new organization or project workspace and assigns the creator as OWNER.
     */
    @Transactional
    public WorkspaceResponse createWorkspace(User owner, CreateWorkspaceRequest request) {
        String slug = resolveUniqueSlug(request.slug(), request.name());

        Workspace workspace = new Workspace();
        workspace.setName(request.name().trim());
        workspace.setSlug(slug);
        workspace.setOwner(owner);
        workspace.setTier(WorkspaceTier.COMMUNITY);
        workspace.setStatus(WorkspaceStatus.ACTIVE);

        Workspace savedWorkspace = workspaceRepository.save(workspace);

        WorkspaceMember ownerMember = new WorkspaceMember(
                savedWorkspace,
                owner,
                WorkspaceRole.OWNER,
                null
        );
        workspaceMemberRepository.save(ownerMember);

        // Ensure user has a default workspace
        Optional<UserPreferences> prefsOpt = userPreferencesRepository.findByUserId(owner.getId());
        if (prefsOpt.isPresent()) {
            UserPreferences prefs = prefsOpt.get();
            if (prefs.getDefaultWorkspace() == null) {
                prefs.setDefaultWorkspace(savedWorkspace);
                userPreferencesRepository.save(prefs);
            }
        }

        auditService.recordEvent(WorkspaceAuditEvent.of(
                WorkspaceAuditEventType.WORKSPACE_CREATED,
                savedWorkspace.getId(),
                owner.getId(),
                null,
                Map.of("workspaceName", savedWorkspace.getName(), "slug", savedWorkspace.getSlug())
        ));

        log.info("Created workspace id={} slug='{}' for owner={}", savedWorkspace.getId(), slug, owner.getId());

        return toWorkspaceResponse(savedWorkspace, WorkspaceRole.OWNER, 1);
    }

    /**
     * Retrieves all workspace summaries where the authenticated user is an active member.
     */
    @Transactional(readOnly = true)
    public List<WorkspaceSummaryResponse> getUserWorkspaces(User user) {
        List<WorkspaceMember> memberships = workspaceMemberRepository.findByUserIdWithWorkspace(user.getId());
        return memberships.stream()
                .map(m -> new WorkspaceSummaryResponse(
                        m.getWorkspace().getId(),
                        m.getWorkspace().getName(),
                        m.getWorkspace().getSlug(),
                        m.getWorkspace().getTier().name(),
                        m.getRole().name(),
                        m.getJoinedAt()
                ))
                .toList();
    }

    /**
     * Retrieves workspace details for an authorized member.
     */
    @Transactional(readOnly = true)
    public WorkspaceResponse getWorkspaceDetails(UUID workspaceId, User user) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace", "id", workspaceId));

        WorkspaceMember member = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, user.getId())
                .orElseThrow(() -> new AccessDeniedException("User is not a member of this workspace"));

        long memberCount = workspaceMemberRepository.countByWorkspaceId(workspaceId);
        return toWorkspaceResponse(workspace, member.getRole(), memberCount);
    }

    /**
     * Updates permitted workspace settings (name) for an ADMIN or OWNER.
     */
    @Transactional
    public WorkspaceResponse updateWorkspace(UUID workspaceId, UpdateWorkspaceRequest request, User user) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace", "id", workspaceId));

        WorkspaceMember member = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, user.getId())
                .orElseThrow(() -> new AccessDeniedException("User is not a member of this workspace"));

        if (!member.getRole().hasAtLeast(WorkspaceRole.ADMIN)) {
            throw new AccessDeniedException("Only ADMIN or OWNER can update workspace settings");
        }

        String oldName = workspace.getName();
        workspace.setName(request.name().trim());
        Workspace updated = workspaceRepository.save(workspace);

        auditService.recordEvent(WorkspaceAuditEvent.of(
                WorkspaceAuditEventType.WORKSPACE_UPDATED,
                workspaceId,
                user.getId(),
                null,
                Map.of("oldName", oldName, "newName", updated.getName())
        ));

        long memberCount = workspaceMemberRepository.countByWorkspaceId(workspaceId);
        return toWorkspaceResponse(updated, member.getRole(), memberCount);
    }

    /**
     * Archives a workspace. Exclusively restricted to the workspace OWNER.
     */
    @Transactional
    public WorkspaceResponse archiveWorkspace(UUID workspaceId, User user) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace", "id", workspaceId));

        WorkspaceMember member = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, user.getId())
                .orElseThrow(() -> new AccessDeniedException("User is not a member of this workspace"));

        if (member.getRole() != WorkspaceRole.OWNER) {
            throw new AccessDeniedException("Only the workspace OWNER can archive the workspace");
        }

        if (workspace.getStatus() == WorkspaceStatus.ARCHIVED) {
            throw new BusinessException("Workspace is already archived", "WORKSPACE_ALREADY_ARCHIVED", HttpStatus.CONFLICT);
        }

        workspace.setStatus(WorkspaceStatus.ARCHIVED);
        Workspace archived = workspaceRepository.save(workspace);

        auditService.recordEvent(WorkspaceAuditEvent.of(
                WorkspaceAuditEventType.WORKSPACE_ARCHIVED,
                workspaceId,
                user.getId(),
                null,
                Map.of("status", "ARCHIVED")
        ));

        log.info("Archived workspace id={} by owner={}", workspaceId, user.getId());

        long memberCount = workspaceMemberRepository.countByWorkspaceId(workspaceId);
        return toWorkspaceResponse(archived, WorkspaceRole.OWNER, memberCount);
    }

    /**
     * Deletes a workspace permanently. Exclusively restricted to the workspace OWNER.
     */
    @Transactional
    public void deleteWorkspace(UUID workspaceId) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace", "id", workspaceId));

        workspaceRepository.delete(workspace);
        log.info("Deleted workspace id={}", workspaceId);
    }

    /**
     * Transfers workspace ownership to an existing active member.
     */
    @Transactional
    public WorkspaceResponse transferOwnership(UUID workspaceId, TransferOwnershipRequest request, User currentOwner) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace", "id", workspaceId));

        WorkspaceMember currentOwnerMember = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, currentOwner.getId())
                .orElseThrow(() -> new AccessDeniedException("User is not a member of this workspace"));

        if (currentOwnerMember.getRole() != WorkspaceRole.OWNER) {
            throw new AccessDeniedException("Only the current workspace OWNER can transfer ownership");
        }

        if (currentOwner.getId().equals(request.newOwnerUserId())) {
            throw new BusinessException("User is already the workspace owner", "ALREADY_OWNER", HttpStatus.BAD_REQUEST);
        }

        WorkspaceMember newOwnerMember = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, request.newOwnerUserId())
                .orElseThrow(() -> new BusinessException("Target user is not a member of this workspace", "MEMBER_NOT_FOUND", HttpStatus.NOT_FOUND));

        // Transfer roles
        currentOwnerMember.setRole(WorkspaceRole.ADMIN);
        newOwnerMember.setRole(WorkspaceRole.OWNER);
        workspace.setOwner(newOwnerMember.getUser());

        workspaceMemberRepository.save(currentOwnerMember);
        workspaceMemberRepository.save(newOwnerMember);
        Workspace updated = workspaceRepository.save(workspace);

        auditService.recordEvent(WorkspaceAuditEvent.of(
                WorkspaceAuditEventType.WORKSPACE_OWNERSHIP_TRANSFERRED,
                workspaceId,
                currentOwner.getId(),
                newOwnerMember.getUser().getId(),
                Map.of("previousOwnerId", currentOwner.getId(), "newOwnerId", newOwnerMember.getUser().getId())
        ));

        log.info("Transferred ownership of workspace id={} from user={} to user={}",
                workspaceId, currentOwner.getId(), newOwnerMember.getUser().getId());

        long memberCount = workspaceMemberRepository.countByWorkspaceId(workspaceId);
        return toWorkspaceResponse(updated, WorkspaceRole.ADMIN, memberCount);
    }

    private String resolveUniqueSlug(String customSlug, String name) {
        String baseSlug;
        if (customSlug != null && !customSlug.isBlank()) {
            baseSlug = customSlug.toLowerCase(Locale.ROOT).trim();
        } else {
            baseSlug = name.toLowerCase(Locale.ROOT)
                    .replaceAll("[^a-z0-9]+", "-")
                    .replaceAll("^-+|-+$", "");
            if (baseSlug.isBlank()) {
                baseSlug = "workspace";
            }
        }

        String candidateSlug = baseSlug;
        int suffix = 1;
        while (workspaceRepository.existsBySlug(candidateSlug)) {
            candidateSlug = baseSlug + "-" + (suffix == 1 ? UUID.randomUUID().toString().substring(0, 4) : suffix);
            suffix++;
        }
        return candidateSlug;
    }

    private WorkspaceResponse toWorkspaceResponse(Workspace ws, WorkspaceRole userRole, long memberCount) {
        return new WorkspaceResponse(
                ws.getId(),
                ws.getName(),
                ws.getSlug(),
                ws.getTier().name(),
                ws.getStatus().name(),
                ws.getOwner().getId(),
                userRole.name(),
                memberCount,
                ws.getCreatedAt()
        );
    }
}
