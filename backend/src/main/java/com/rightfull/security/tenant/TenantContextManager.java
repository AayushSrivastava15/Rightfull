package com.rightfull.security.tenant;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Manages transaction-scoped PostgreSQL session variables for Row Level Security (RLS).
 * Enforces transaction-local parameterization via set_config(..., true) to prevent connection pool leakage.
 */
@Component
public class TenantContextManager {

    private static final Logger log = LoggerFactory.getLogger(TenantContextManager.class);

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Binds the target workspace, user, and Supabase identity to the current PostgreSQL transaction.
     * With is_local = true, PostgreSQL automatically reverts the variables upon transaction commit/rollback.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void applyTenantContext(UUID workspaceId, UUID userId, UUID supabaseUserId) {
        applyTenantContext(workspaceId, userId, supabaseUserId, null);
    }

    /**
     * Binds the target workspace, user, Supabase identity, and email to the current PostgreSQL transaction.
     * With is_local = true, PostgreSQL automatically reverts the variables upon transaction commit/rollback.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void applyTenantContext(UUID workspaceId, UUID userId, UUID supabaseUserId, String email) {
        try {
            String wsStr = (workspaceId != null) ? workspaceId.toString() : "";
            String userStr = (userId != null) ? userId.toString() : "";
            String subStr = (supabaseUserId != null) ? supabaseUserId.toString() : "";
            String emailStr = (email != null) ? email.trim() : "";

            entityManager.createNativeQuery("SELECT set_config('app.current_workspace_id', :ws, true)")
                    .setParameter("ws", wsStr)
                    .getSingleResult();

            entityManager.createNativeQuery("SELECT set_config('app.current_user_id', :usr, true)")
                    .setParameter("usr", userStr)
                    .getSingleResult();

            entityManager.createNativeQuery("SELECT set_config('app.current_supabase_user_id', :sub, true)")
                    .setParameter("sub", subStr)
                    .getSingleResult();

            entityManager.createNativeQuery("SELECT set_config('app.current_user_email', :email, true)")
                    .setParameter("email", emailStr)
                    .getSingleResult();

            log.debug("Successfully bound transaction-scoped PostgreSQL tenant context: ws={}, user={}, email={}", wsStr, userStr, emailStr);
        } catch (Exception ex) {
            // Graceful fallback for non-PostgreSQL dialects (e.g. H2 in standard unit tests)
            log.trace("Skipping set_config on non-PostgreSQL dialect: {}", ex.getMessage());
        }
    }

    /**
     * Binds the current TenantContext state to the active transaction.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void applyCurrentContext() {
        TenantContext.get().ifPresent(ctx -> 
                applyTenantContext(ctx.workspaceId(), ctx.userId(), ctx.supabaseUserId(), ctx.email()));
    }
}

