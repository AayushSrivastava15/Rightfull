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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProvisioningServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserPreferencesRepository userPreferencesRepository;

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private WorkspaceMemberRepository workspaceMemberRepository;

    private UserProvisioningService service;

    @BeforeEach
    void setUp() {
        service = new UserProvisioningService(
                userRepository,
                userPreferencesRepository,
                workspaceRepository,
                workspaceMemberRepository
        );
    }

    private Jwt createMockJwt(UUID supabaseUserId, String email, String fullName, String avatarUrl) {
        Jwt.Builder builder = Jwt.withTokenValue("mock.jwt.token")
                .header("alg", "ES256")
                .subject(supabaseUserId.toString())
                .claim("email", email)
                .claim("aud", "authenticated")
                .issuer("https://test.supabase.co/auth/v1")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600));

        if (fullName != null || avatarUrl != null) {
            builder.claim("user_metadata", Map.of(
                    "full_name", fullName != null ? fullName : "",
                    "avatar_url", avatarUrl != null ? avatarUrl : ""
            ));
        }

        return builder.build();
    }

    @Test
    @DisplayName("First-time user provisioning: creates User, Personal Workspace, OWNER membership, and Preferences")
    void provisionNewUserSuccessfully() {
        UUID supabaseUserId = UUID.randomUUID();
        String email = "alice@example.com";
        String fullName = "Alice Smith";
        String avatarUrl = "https://example.com/avatar.png";

        Jwt jwt = createMockJwt(supabaseUserId, email, fullName, avatarUrl);

        when(userRepository.findBySupabaseUserId(supabaseUserId)).thenReturn(Optional.empty());
        when(workspaceRepository.existsBySlug(any())).thenReturn(false);

        User mockSavedUser = new User(supabaseUserId, email, fullName, avatarUrl);
        UUID userId = UUID.randomUUID();
        mockSavedUser.setId(userId);
        when(userRepository.saveAndFlush(any(User.class))).thenReturn(mockSavedUser);

        Workspace mockSavedWorkspace = new Workspace("Alice Smith's Workspace", "alice-smith-workspace", mockSavedUser);
        UUID workspaceId = UUID.randomUUID();
        mockSavedWorkspace.setId(workspaceId);
        when(workspaceRepository.saveAndFlush(any(Workspace.class))).thenReturn(mockSavedWorkspace);

        User result = service.getOrCreateUser(jwt);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(userId);
        assertThat(result.getEmail()).isEqualTo(email);
        assertThat(result.getSupabaseUserId()).isEqualTo(supabaseUserId);

        // Verify Workspace Member creation with OWNER role
        ArgumentCaptor<WorkspaceMember> memberCaptor = ArgumentCaptor.forClass(WorkspaceMember.class);
        verify(workspaceMemberRepository).saveAndFlush(memberCaptor.capture());
        WorkspaceMember member = memberCaptor.getValue();
        assertThat(member.getRole()).isEqualTo(WorkspaceRole.OWNER);
        assertThat(member.getUser()).isEqualTo(mockSavedUser);
        assertThat(member.getWorkspace()).isEqualTo(mockSavedWorkspace);

        // Verify User Preferences creation
        ArgumentCaptor<UserPreferences> prefCaptor = ArgumentCaptor.forClass(UserPreferences.class);
        verify(userPreferencesRepository).saveAndFlush(prefCaptor.capture());
        UserPreferences pref = prefCaptor.getValue();
        assertThat(pref.getUser()).isEqualTo(mockSavedUser);
        assertThat(pref.getDefaultWorkspace()).isEqualTo(mockSavedWorkspace);
    }

    @Test
    @DisplayName("Returning user login: idempotent lookup returns existing user without provisioning")
    void returningUserReturnsExistingWithoutProvisioning() {
        UUID supabaseUserId = UUID.randomUUID();
        String email = "bob@example.com";
        Jwt jwt = createMockJwt(supabaseUserId, email, "Bob Jones", null);

        User existingUser = new User(supabaseUserId, email, "Bob Jones", null);
        existingUser.setId(UUID.randomUUID());

        when(userRepository.findBySupabaseUserId(supabaseUserId)).thenReturn(Optional.of(existingUser));

        User result = service.getOrCreateUser(jwt);

        assertThat(result).isEqualTo(existingUser);
        verify(userRepository, never()).saveAndFlush(any());
        verify(workspaceRepository, never()).saveAndFlush(any());
        verify(workspaceMemberRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Concurrent provisioning race condition: gracefully falls back to existing record upon unique constraint violation")
    void concurrentProvisioningHandledSafely() {
        UUID supabaseUserId = UUID.randomUUID();
        String email = "carol@example.com";
        Jwt jwt = createMockJwt(supabaseUserId, email, "Carol Danvers", null);

        when(userRepository.findBySupabaseUserId(supabaseUserId))
                .thenReturn(Optional.empty()) // Initial check
                .thenReturn(Optional.of(new User(supabaseUserId, email, "Carol Danvers", null))); // Check after race

        when(userRepository.saveAndFlush(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint 'users_supabase_user_id_key'"));

        User result = service.getOrCreateUser(jwt);

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo(email);
    }

    @Test
    @DisplayName("Get user profile returns verified application and workspace details")
    void getUserProfileReturnsCompleteProfile() {
        UUID supabaseUserId = UUID.randomUUID();
        String email = "dave@example.com";
        Jwt jwt = createMockJwt(supabaseUserId, email, "Dave Miller", "https://avatar.png");

        User user = new User(supabaseUserId, email, "Dave Miller", "https://avatar.png");
        UUID userId = UUID.randomUUID();
        user.setId(userId);

        Workspace workspace = new Workspace("Dave Miller's Workspace", "dave-miller-workspace", user);
        UUID workspaceId = UUID.randomUUID();
        workspace.setId(workspaceId);
        workspace.setTier(WorkspaceTier.COMMUNITY);

        UserPreferences preferences = new UserPreferences(user, workspace);

        WorkspaceMember member = new WorkspaceMember(workspace, user, WorkspaceRole.OWNER, null);

        when(userRepository.findBySupabaseUserId(supabaseUserId)).thenReturn(Optional.of(user));
        when(userPreferencesRepository.findByUserIdWithWorkspace(userId)).thenReturn(Optional.of(preferences));
        when(workspaceMemberRepository.findByUserIdWithWorkspace(userId)).thenReturn(List.of(member));

        UserProfileResponse response = service.getUserProfile(jwt);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(userId);
        assertThat(response.supabaseUserId()).isEqualTo(supabaseUserId);
        assertThat(response.email()).isEqualTo(email);
        assertThat(response.fullName()).isEqualTo("Dave Miller");
        assertThat(response.defaultWorkspace()).isNotNull();
        assertThat(response.defaultWorkspace().id()).isEqualTo(workspaceId);
        assertThat(response.defaultWorkspace().role()).isEqualTo(WorkspaceRole.OWNER);
        assertThat(response.memberships()).hasSize(1);
        assertThat(response.memberships().getFirst().role()).isEqualTo(WorkspaceRole.OWNER);
    }
}

