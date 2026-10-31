package com.rightfull.identity.service;

import com.rightfull.audit.WorkspaceAuditEvent;
import com.rightfull.audit.WorkspaceAuditEventType;
import com.rightfull.audit.WorkspaceAuditService;
import com.rightfull.common.exception.BusinessException;
import com.rightfull.common.exception.ResourceNotFoundException;
import com.rightfull.identity.entity.User;
import com.rightfull.identity.entity.UserPreferences;
import com.rightfull.identity.repository.UserPreferencesRepository;
import com.rightfull.workspace.dto.WorkspaceSummaryResponse;
import com.rightfull.workspace.entity.Workspace;
import com.rightfull.workspace.entity.WorkspaceMember;
import com.rightfull.workspace.repository.WorkspaceMemberRepository;
import com.rightfull.workspace.repository.WorkspaceRepository;
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
 * Service managing user settings, theme, notifications, and default workspace selection.
 */
@Service
public class UserPreferencesService {

    private static final Logger log = LoggerFactory.getLogger(UserPreferencesService.class);

    private final UserPreferencesRepository userPreferencesRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final WorkspaceAuditService auditService;

    public UserPreferencesService(
            UserPreferencesRepository userPreferencesRepository,
            WorkspaceRepository workspaceRepository,
            WorkspaceMemberRepository workspaceMemberRepository,
            WorkspaceAuditService auditService) {
        this.userPreferencesRepository = userPreferencesRepository;
        this.workspaceRepository = workspaceRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.auditService = auditService;
    }

    /**
     * Retrieves the default workspace for the authenticated user, validating continued membership.
     */
    @Transactional
    public WorkspaceSummaryResponse getDefaultWorkspace(User user) {
        Optional<UserPreferences> prefsOpt = userPreferencesRepository.findByUserIdWithWorkspace(user.getId());

        if (prefsOpt.isPresent() && prefsOpt.get().getDefaultWorkspace() != null) {
            Workspace defaultWs = prefsOpt.get().getDefaultWorkspace();
            Optional<WorkspaceMember> memberOpt = workspaceMemberRepository.findByWorkspaceIdAndUserId(defaultWs.getId(), user.getId());

            if (memberOpt.isPresent()) {
                return new WorkspaceSummaryResponse(
                        defaultWs.getId(),
                        defaultWs.getName(),
                        defaultWs.getSlug(),
                        defaultWs.getTier().name(),
                        memberOpt.get().getRole().name(),
                        memberOpt.get().getJoinedAt()
                );
            } else {
                // User is no longer a member of this default workspace; clear stale reference
                log.warn("User id={} is no longer a member of default workspace id={}. Clearing reference.",
                        user.getId(), defaultWs.getId());
                UserPreferences prefs = prefsOpt.get();
                prefs.setDefaultWorkspace(null);
                userPreferencesRepository.save(prefs);
            }
        }

        // Fallback: Auto-assign first available workspace membership
        List<WorkspaceMember> memberships = workspaceMemberRepository.findByUserIdWithWorkspace(user.getId());
        if (!memberships.isEmpty()) {
            WorkspaceMember first = memberships.get(0);
            UserPreferences prefs = prefsOpt.orElseGet(() -> new UserPreferences(user));
            prefs.setDefaultWorkspace(first.getWorkspace());
            userPreferencesRepository.save(prefs);

            return new WorkspaceSummaryResponse(
                    first.getWorkspace().getId(),
                    first.getWorkspace().getName(),
                    first.getWorkspace().getSlug(),
                    first.getWorkspace().getTier().name(),
                    first.getRole().name(),
                    first.getJoinedAt()
            );
        }

        return null;
    }

    /**
     * Updates the user's default active workspace.
     * Validates that the target workspace exists and the user has active membership.
     */
    @Transactional
    public WorkspaceSummaryResponse setDefaultWorkspace(User user, UUID workspaceId) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace", "id", workspaceId));

        WorkspaceMember membership = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, user.getId())
                .orElseThrow(() -> new AccessDeniedException("User is not a member of the selected workspace"));

        UserPreferences prefs = userPreferencesRepository.findByUserId(user.getId())
                .orElseGet(() -> new UserPreferences(user));

        UUID previousDefaultId = prefs.getDefaultWorkspace() != null ? prefs.getDefaultWorkspace().getId() : null;
        prefs.setDefaultWorkspace(workspace);
        userPreferencesRepository.save(prefs);

        auditService.recordEvent(WorkspaceAuditEvent.of(
                WorkspaceAuditEventType.DEFAULT_WORKSPACE_CHANGED,
                workspaceId,
                user.getId(),
                null,
                Map.of("previousDefaultId", previousDefaultId != null ? previousDefaultId : "N/A", "newDefaultId", workspaceId)
        ));

        log.info("Updated default workspace for user id={} to workspace id={}", user.getId(), workspaceId);

        return new WorkspaceSummaryResponse(
                workspace.getId(),
                workspace.getName(),
                workspace.getSlug(),
                workspace.getTier().name(),
                membership.getRole().name(),
                membership.getJoinedAt()
        );
    }
}

