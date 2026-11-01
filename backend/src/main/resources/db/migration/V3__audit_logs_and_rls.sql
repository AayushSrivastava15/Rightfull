-- ==============================================================================
-- RIGHTFULL MIGRATION V3: MULTI-TENANT ROW-LEVEL SECURITY & AUDIT LOGS
-- ==============================================================================
-- Module: Multi-Tenant Database Isolation & Persistent Compliance Auditing
-- Target Database: PostgreSQL 17+ (Supabase)
-- Author: Rightfull Engineering
-- ==============================================================================

-- ------------------------------------------------------------------------------
-- 1. Table: audit_logs
-- Immutable compliance and security audit logs tracking administrative actions.
-- ------------------------------------------------------------------------------
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID REFERENCES workspaces(id) ON DELETE CASCADE,
    actor_id UUID REFERENCES users(id) ON DELETE SET NULL,
    event_type VARCHAR(64) NOT NULL,
    target_type VARCHAR(64),
    target_id UUID,
    ip_address VARCHAR(45),
    details JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_logs_workspace_time ON audit_logs(workspace_id, created_at DESC);
CREATE INDEX idx_audit_logs_target ON audit_logs(workspace_id, target_type, target_id);
CREATE INDEX idx_audit_logs_actor ON audit_logs(actor_id, created_at DESC);

-- Trigger to guarantee engine-level audit log immutability (blocks UPDATE and DELETE)
CREATE OR REPLACE FUNCTION trg_audit_logs_immutable()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'Audit logs are immutable and cannot be updated or deleted';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_audit_logs_prevent_mutation
BEFORE UPDATE OR DELETE ON audit_logs
FOR EACH ROW EXECUTE FUNCTION trg_audit_logs_immutable();

ALTER TABLE audit_logs ENABLE ROW LEVEL SECURITY;

-- ------------------------------------------------------------------------------
-- 2. Transaction-Scoped Context Helper Functions
-- Reads local transaction settings set via set_config(..., true)
-- ------------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION app_current_workspace_id() RETURNS UUID AS $$
BEGIN
    RETURN NULLIF(current_setting('app.current_workspace_id', true), '')::uuid;
EXCEPTION WHEN OTHERS THEN
    RETURN NULL;
END;
$$ LANGUAGE plpgsql STABLE SECURITY DEFINER;

CREATE OR REPLACE FUNCTION app_current_user_id() RETURNS UUID AS $$
BEGIN
    RETURN NULLIF(current_setting('app.current_user_id', true), '')::uuid;
EXCEPTION WHEN OTHERS THEN
    RETURN NULL;
END;
$$ LANGUAGE plpgsql STABLE SECURITY DEFINER;

CREATE OR REPLACE FUNCTION app_current_supabase_user_id() RETURNS UUID AS $$
BEGIN
    RETURN NULLIF(current_setting('app.current_supabase_user_id', true), '')::uuid;
EXCEPTION WHEN OTHERS THEN
    RETURN NULL;
END;
$$ LANGUAGE plpgsql STABLE SECURITY DEFINER;

-- ------------------------------------------------------------------------------
-- 3. Row-Level Security (RLS) Policies
-- Enforced on non-BYPASSRLS application roles (e.g. rightfull_app)
-- ------------------------------------------------------------------------------

-- Users table policies
DROP POLICY IF EXISTS users_select_policy ON users;
CREATE POLICY users_select_policy ON users
FOR SELECT TO PUBLIC
USING (
    id = app_current_user_id()
    OR supabase_user_id = app_current_supabase_user_id()
    OR id IN (
        SELECT wm.user_id FROM workspace_members wm 
        WHERE wm.workspace_id = app_current_workspace_id()
    )
);

DROP POLICY IF EXISTS users_insert_policy ON users;
CREATE POLICY users_insert_policy ON users
FOR INSERT TO PUBLIC
WITH CHECK (
    supabase_user_id = app_current_supabase_user_id()
    OR id = app_current_user_id()
    OR app_current_user_id() IS NULL
);

DROP POLICY IF EXISTS users_update_policy ON users;
CREATE POLICY users_update_policy ON users
FOR UPDATE TO PUBLIC
USING (id = app_current_user_id())
WITH CHECK (id = app_current_user_id());

-- User Preferences table policies
DROP POLICY IF EXISTS user_preferences_all_policy ON user_preferences;
CREATE POLICY user_preferences_all_policy ON user_preferences
FOR ALL TO PUBLIC
USING (user_id = app_current_user_id())
WITH CHECK (user_id = app_current_user_id());

-- Workspaces table policies
DROP POLICY IF EXISTS workspaces_select_policy ON workspaces;
CREATE POLICY workspaces_select_policy ON workspaces
FOR SELECT TO PUBLIC
USING (
    id = app_current_workspace_id()
    OR owner_id = app_current_user_id()
    OR id IN (
        SELECT wm.workspace_id FROM workspace_members wm
        WHERE wm.user_id = app_current_user_id()
    )
);

DROP POLICY IF EXISTS workspaces_insert_policy ON workspaces;
CREATE POLICY workspaces_insert_policy ON workspaces
FOR INSERT TO PUBLIC
WITH CHECK (owner_id = app_current_user_id());

DROP POLICY IF EXISTS workspaces_update_policy ON workspaces;
CREATE POLICY workspaces_update_policy ON workspaces
FOR UPDATE TO PUBLIC
USING (
    id = app_current_workspace_id()
    AND (
        owner_id = app_current_user_id()
        OR id IN (
            SELECT wm.workspace_id FROM workspace_members wm
            WHERE wm.user_id = app_current_user_id() AND wm.role IN ('OWNER', 'ADMIN')
        )
    )
)
WITH CHECK (
    id = app_current_workspace_id()
    AND (
        owner_id = app_current_user_id()
        OR id IN (
            SELECT wm.workspace_id FROM workspace_members wm
            WHERE wm.user_id = app_current_user_id() AND wm.role IN ('OWNER', 'ADMIN')
        )
    )
);

DROP POLICY IF EXISTS workspaces_delete_policy ON workspaces;
CREATE POLICY workspaces_delete_policy ON workspaces
FOR DELETE TO PUBLIC
USING (
    owner_id = app_current_user_id()
    AND id = app_current_workspace_id()
);

-- Workspace Members table policies
DROP POLICY IF EXISTS workspace_members_select_policy ON workspace_members;
CREATE POLICY workspace_members_select_policy ON workspace_members
FOR SELECT TO PUBLIC
USING (
    workspace_id = app_current_workspace_id()
    OR user_id = app_current_user_id()
);

DROP POLICY IF EXISTS workspace_members_insert_policy ON workspace_members;
CREATE POLICY workspace_members_insert_policy ON workspace_members
FOR INSERT TO PUBLIC
WITH CHECK (
    workspace_id = app_current_workspace_id()
    OR user_id = app_current_user_id()
);

DROP POLICY IF EXISTS workspace_members_update_policy ON workspace_members;
CREATE POLICY workspace_members_update_policy ON workspace_members
FOR UPDATE TO PUBLIC
USING (workspace_id = app_current_workspace_id())
WITH CHECK (workspace_id = app_current_workspace_id());

DROP POLICY IF EXISTS workspace_members_delete_policy ON workspace_members;
CREATE POLICY workspace_members_delete_policy ON workspace_members
FOR DELETE TO PUBLIC
USING (
    workspace_id = app_current_workspace_id()
    OR user_id = app_current_user_id()
);

-- Workspace Invitations table policies
DROP POLICY IF EXISTS workspace_invitations_select_policy ON workspace_invitations;
CREATE POLICY workspace_invitations_select_policy ON workspace_invitations
FOR SELECT TO PUBLIC
USING (
    workspace_id = app_current_workspace_id()
    OR status = 'PENDING'
);

DROP POLICY IF EXISTS workspace_invitations_insert_policy ON workspace_invitations;
CREATE POLICY workspace_invitations_insert_policy ON workspace_invitations
FOR INSERT TO PUBLIC
WITH CHECK (workspace_id = app_current_workspace_id());

DROP POLICY IF EXISTS workspace_invitations_update_policy ON workspace_invitations;
CREATE POLICY workspace_invitations_update_policy ON workspace_invitations
FOR UPDATE TO PUBLIC
USING (
    workspace_id = app_current_workspace_id()
    OR status = 'PENDING'
)
WITH CHECK (
    workspace_id = app_current_workspace_id()
    OR status IN ('ACCEPTED', 'REJECTED')
);

DROP POLICY IF EXISTS workspace_invitations_delete_policy ON workspace_invitations;
CREATE POLICY workspace_invitations_delete_policy ON workspace_invitations
FOR DELETE TO PUBLIC
USING (workspace_id = app_current_workspace_id());

-- Audit Logs table policies
DROP POLICY IF EXISTS audit_logs_select_policy ON audit_logs;
CREATE POLICY audit_logs_select_policy ON audit_logs
FOR SELECT TO PUBLIC
USING (workspace_id = app_current_workspace_id());

DROP POLICY IF EXISTS audit_logs_insert_policy ON audit_logs;
CREATE POLICY audit_logs_insert_policy ON audit_logs
FOR INSERT TO PUBLIC
WITH CHECK (
    workspace_id = app_current_workspace_id()
    OR workspace_id IS NULL
);

-- ------------------------------------------------------------------------------
-- 4. Application Role Privileges
-- Grant necessary schema, table, and sequence access to rightfull_app if it exists
-- ------------------------------------------------------------------------------
DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'rightfull_app') THEN
        GRANT USAGE ON SCHEMA public TO rightfull_app;
        GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO rightfull_app;
        GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO rightfull_app;
    END IF;
END $$;

