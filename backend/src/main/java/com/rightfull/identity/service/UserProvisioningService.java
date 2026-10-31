package com.rightfull.identity.service;

import com.rightfull.identity.dto.UserProfileResponse;
import com.rightfull.identity.entity.User;
import com.rightfull.identity.entity.UserPreferences;
import com.rightfull.identity.repository.UserPreferencesRepository;
import com.rightfull.identity.repository.UserRepository;
import com.rightfull.workspace.entity.Workspace;
import com.rightfull.workspace.entity.WorkspaceMember;
import com.rightfull.workspace.entity.WorkspaceRole;
import com.rightfull.workspace.entity.WorkspaceTier;
import com.rightfull.workspace.repository.WorkspaceMemberRepository;
import com.rightfull.workspace.repository.WorkspaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Handles idempotent first-login user provisioning and profile synchronization.
 */
@Service
public class UserProvisioningService {

    private static final Logger log = LoggerFactory.getLogger(UserProvisioningService.class);

    private final UserRepository userRepository;
    private final UserPreferencesRepository userPreferencesRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    public UserProvisioningService(
            UserRepository userRepository,
            UserPreferencesRepository userPreferencesRepository,
            WorkspaceRepository workspaceRepository,
            WorkspaceMemberRepository workspaceMemberRepository) {
        this.userRepository = userRepository;
        this.userPreferencesRepository = userPreferencesRepository;
        this.workspaceRepository = workspaceRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
    }

    /**
     * Resolves the application user for the verified Supabase JWT.
     * Provisions the user, their personal workspace, and OWNER membership if accessing for the first time.
     */
    @Transactional
    public User getOrCreateUser(Jwt jwt) {
        UUID supabaseUserId = extractSupabaseUserId(jwt);
        Optional<User> existingUser = userRepository.findBySupabaseUserId(supabaseUserId);

        if (existingUser.isPresent()) {
            return existingUser.get();
        }

        return provisionNewUser(jwt, supabaseUserId);
    }

    /**
     * Retrieves the complete, verified user profile and workspace memberships.
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(Jwt jwt) {
        User user = getOrCreateUser(jwt);
        Optional<UserPreferences> preferencesOpt = userPreferencesRepository.findByUserIdWithWorkspace(user.getId());
        List<WorkspaceMember> memberships = workspaceMemberRepository.findByUserIdWithWorkspace(user.getId());

        List<UserProfileResponse.WorkspaceMembershipDto> membershipDtos = memberships.stream()
                .map(m -> new UserProfileResponse.WorkspaceMembershipDto(
                        m.getWorkspace().getId(),
                        m.getWorkspace().getName(),
                        m.getWorkspace().getSlug(),
                        m.getWorkspace().getTier(),
                        m.getRole(),
                        m.getJoinedAt()
                ))
                .toList();

        UserProfileResponse.WorkspaceSummaryDto defaultWorkspaceDto = null;
        if (preferencesOpt.isPresent() && preferencesOpt.get().getDefaultWorkspace() != null) {
            Workspace defaultWs = preferencesOpt.get().getDefaultWorkspace();
            WorkspaceRole role = memberships.stream()
                    .filter(m -> m.getWorkspace().getId().equals(defaultWs.getId()))
                    .map(WorkspaceMember::getRole)
                    .findFirst()
                    .orElse(WorkspaceRole.VIEWER);

            defaultWorkspaceDto = new UserProfileResponse.WorkspaceSummaryDto(
                    defaultWs.getId(),
                    defaultWs.getName(),
                    defaultWs.getSlug(),
                    defaultWs.getTier(),
                    role
            );
        } else if (!memberships.isEmpty()) {
            WorkspaceMember first = memberships.getFirst();
            defaultWorkspaceDto = new UserProfileResponse.WorkspaceSummaryDto(
                    first.getWorkspace().getId(),
                    first.getWorkspace().getName(),
                    first.getWorkspace().getSlug(),
                    first.getWorkspace().getTier(),
                    first.getRole()
            );
        }

        return new UserProfileResponse(
                user.getId(),
                user.getSupabaseUserId(),
                user.getEmail(),
                user.getFullName(),
                user.getAvatarUrl(),
                user.isActive(),
                user.getCreatedAt(),
                defaultWorkspaceDto,
                membershipDtos
        );
    }

    private User provisionNewUser(Jwt jwt, UUID supabaseUserId) {
        String email = extractEmail(jwt);
        String fullName = extractFullName(jwt);
        String avatarUrl = extractAvatarUrl(jwt);

        log.info("Provisioning first-login user: supabaseUserId={}, email={}", supabaseUserId, email);

        try {
            // 1. Create User
            User newUser = new User(supabaseUserId, email, fullName, avatarUrl);
            User savedUser = userRepository.saveAndFlush(newUser);

            // 2. Generate Personal Workspace
            String baseName = (fullName != null && !fullName.isBlank()) ? fullName.trim() : email.substring(0, email.indexOf('@'));
            String workspaceName = baseName + "'s Workspace";
            String slug = generateUniqueSlug(baseName, supabaseUserId);

            Workspace personalWorkspace = new Workspace(workspaceName, slug, savedUser);
            personalWorkspace.setTier(WorkspaceTier.COMMUNITY);
            Workspace savedWorkspace = workspaceRepository.saveAndFlush(personalWorkspace);

            // 3. Create OWNER Workspace Membership
            WorkspaceMember ownerMembership = new WorkspaceMember(savedWorkspace, savedUser, WorkspaceRole.OWNER, null);
            workspaceMemberRepository.saveAndFlush(ownerMembership);

            // 4. Create User Preferences
            UserPreferences preferences = new UserPreferences(savedUser, savedWorkspace);
            userPreferencesRepository.saveAndFlush(preferences);

            log.info("Successfully provisioned user id={} with personal workspace id={} (slug={})",
                    savedUser.getId(), savedWorkspace.getId(), savedWorkspace.getSlug());

            return savedUser;
        } catch (DataIntegrityViolationException ex) {
            log.warn("Concurrent provisioning detected for supabaseUserId={}, fetching existing user: {}",
                    supabaseUserId, ex.getMessage());
            return userRepository.findBySupabaseUserId(supabaseUserId)
                    .orElseThrow(() -> new IllegalStateException("Failed to retrieve concurrently provisioned user", ex));
        }
    }

    private String generateUniqueSlug(String baseName, UUID supabaseUserId) {
        String clean = baseName.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        if (clean.isBlank()) {
            clean = "workspace";
        }
        String candidate = clean + "-workspace";

        if (!workspaceRepository.existsBySlug(candidate)) {
            return candidate;
        }

        String suffix = supabaseUserId.toString().replace("-", "").substring(0, 6);
        return clean + "-" + suffix;
    }

    private UUID extractSupabaseUserId(Jwt jwt) {
        String sub = jwt.getSubject();
        if (sub == null || sub.isBlank()) {
            throw new IllegalArgumentException("JWT subject claim (sub) cannot be empty");
        }
        return UUID.fromString(sub);
    }

    private String extractEmail(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        if (email != null && !email.isBlank()) {
            return email.trim().toLowerCase();
        }

        Map<String, Object> userMetadata = jwt.getClaimAsMap("user_metadata");
        if (userMetadata != null && userMetadata.get("email") instanceof String metaEmail && !metaEmail.isBlank()) {
            return metaEmail.trim().toLowerCase();
        }

        throw new IllegalArgumentException("JWT does not contain a verified email claim");
    }

    private String extractFullName(Jwt jwt) {
        Map<String, Object> userMetadata = jwt.getClaimAsMap("user_metadata");
        if (userMetadata != null) {
            if (userMetadata.get("full_name") instanceof String fn && !fn.isBlank()) {
                return fn.trim();
            }
            if (userMetadata.get("name") instanceof String n && !n.isBlank()) {
                return n.trim();
            }
            if (userMetadata.get("preferred_username") instanceof String pu && !pu.isBlank()) {
                return pu.trim();
            }
        }
        return null;
    }

    private String extractAvatarUrl(Jwt jwt) {
        Map<String, Object> userMetadata = jwt.getClaimAsMap("user_metadata");
        if (userMetadata != null) {
            if (userMetadata.get("avatar_url") instanceof String url && !url.isBlank()) {
                return url.trim();
            }
            if (userMetadata.get("picture") instanceof String pic && !pic.isBlank()) {
                return pic.trim();
            }
        }
        return null;
    }
}

