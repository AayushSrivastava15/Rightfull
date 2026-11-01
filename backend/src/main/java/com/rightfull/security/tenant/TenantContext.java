package com.rightfull.security.tenant;

import com.rightfull.workspace.entity.WorkspaceRole;

import java.util.Optional;
import java.util.UUID;

/**
 * Thread-local tenant context holding verified workspace and user credentials for the active request.
 */
public final class TenantContext {

    public record TenantContextData(
            UUID workspaceId,
            UUID userId,
            UUID supabaseUserId,
            WorkspaceRole role,
            String email
    ) {}

    private static final ThreadLocal<TenantContextData> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(UUID workspaceId, UUID userId, UUID supabaseUserId, WorkspaceRole role) {
        set(workspaceId, userId, supabaseUserId, role, null);
    }

    public static void set(UUID workspaceId, UUID userId, UUID supabaseUserId, WorkspaceRole role, String email) {
        CURRENT.set(new TenantContextData(workspaceId, userId, supabaseUserId, role, email));
    }

    public static Optional<TenantContextData> get() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static Optional<UUID> getWorkspaceId() {
        return get().map(TenantContextData::workspaceId);
    }

    public static Optional<UUID> getUserId() {
        return get().map(TenantContextData::userId);
    }

    public static Optional<String> getEmail() {
        return get().map(TenantContextData::email);
    }

    public static void clear() {
        CURRENT.remove();
    }
}

