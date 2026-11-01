package com.rightfull.workspace.security;

import com.rightfull.identity.entity.User;
import com.rightfull.identity.repository.UserRepository;
import com.rightfull.security.tenant.TenantContext;
import com.rightfull.workspace.entity.WorkspaceMember;
import com.rightfull.workspace.entity.WorkspaceRole;
import com.rightfull.workspace.repository.WorkspaceMemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Centralized evaluator for workspace-level role-based authorization.
 * Used in method-level annotations, e.g. {@code @PreAuthorize("@workspaceSecurity.hasRole(#workspaceId, 'ADMIN')")}
 */
@Component("workspaceSecurity")
public class WorkspaceSecurityEvaluator {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceSecurityEvaluator.class);

    private final UserRepository userRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    public WorkspaceSecurityEvaluator(UserRepository userRepository, WorkspaceMemberRepository workspaceMemberRepository) {
        this.userRepository = userRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
    }

    /**
     * Verifies that the authenticated user has at least the specified role in the target workspace.
     */
    @Transactional(readOnly = true)
    public boolean hasRole(UUID workspaceId, String requiredRoleName) {
        if (workspaceId == null || requiredRoleName == null) {
            return false;
        }

        WorkspaceRole requiredRole;
        try {
            requiredRole = WorkspaceRole.valueOf(requiredRoleName.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            log.error("Invalid role name supplied to security evaluator: {}", requiredRoleName);
            return false;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            log.warn("Unauthorized access attempt: No verified JWT found in SecurityContext");
            return false;
        }

        String sub = jwt.getSubject();
        if (sub == null || sub.isBlank()) {
            return false;
        }

        UUID supabaseUserId;
        try {
            supabaseUserId = UUID.fromString(sub);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid UUID in JWT sub claim: {}", sub);
            return false;
        }

        Optional<User> userOpt = userRepository.findBySupabaseUserId(supabaseUserId);
        if (userOpt.isEmpty()) {
            log.warn("No application user found for authenticated supabase_user_id={}", supabaseUserId);
            return false;
        }

        UUID userId = userOpt.get().getId();
        Optional<WorkspaceMember> memberOpt = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId);
        if (memberOpt.isEmpty()) {
            log.warn("Access denied: User id={} has no membership in workspace id={}", userId, workspaceId);
            TenantContext.clear();
            return false;
        }

        WorkspaceRole actualRole = memberOpt.get().getRole();
        boolean authorized = actualRole.hasAtLeast(requiredRole);
        if (!authorized) {
            log.warn("Access denied: User id={} has role={} but required={}", userId, actualRole, requiredRole);
            TenantContext.clear();
        } else {
            TenantContext.set(workspaceId, userId, supabaseUserId, actualRole, userOpt.get().getEmail());
        }

        return authorized;
    }

    public boolean isOwner(UUID workspaceId) {
        return hasRole(workspaceId, "OWNER");
    }

    public boolean isAdmin(UUID workspaceId) {
        return hasRole(workspaceId, "ADMIN");
    }

    public boolean isMember(UUID workspaceId) {
        return hasRole(workspaceId, "MEMBER");
    }

    public boolean isViewer(UUID workspaceId) {
        return hasRole(workspaceId, "VIEWER");
    }
}

