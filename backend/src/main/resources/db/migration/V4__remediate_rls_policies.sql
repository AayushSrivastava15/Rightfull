-- ==============================================================================
-- RIGHTFULL MIGRATION V4: REMEDIATE WORKSPACE RLS POLICIES & SECURE INVITATIONS
-- ==============================================================================
-- Fixes:
-- 1. Eliminates cross-workspace invitation visibility (removes OR status = 'PENDING').
-- 2. Provides secure invitation redemption lookup via SECURITY DEFINER function.
-- 3. Restricts workspace membership self-insertion and prevents RLS recursion.
-- Target Database: PostgreSQL 17+ (Supabase)
-- ==============================================================================

-- ------------------------------------------------------------------------------
-- 1. Helper function for current user email claim
-- ------------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION app_current_user_email() RETURNS VARCHAR AS $$
BEGIN
    RETURN NULLIF(current_setting('app.current_user_email', true), '');
EXCEPTION WHEN OTHERS THEN
    RETURN NULL;
END;
$$ LANGUAGE plpgsql STABLE SECURITY DEFINER
SET search_path = public, pg_temp;

-- ------------------------------------------------------------------------------
-- 2. Non-recursive helper for workspace admin check
-- ------------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION app_is_workspace_admin(p_workspace_id UUID, p_user_id UUID)
RETURNS BOOLEAN AS $$
BEGIN
    IF p_workspace_id IS NULL OR p_user_id IS NULL THEN
        RETURN FALSE;
    END IF;
    RETURN EXISTS (
        SELECT 1 FROM workspace_members
        WHERE workspace_id = p_workspace_id
          AND user_id = p_user_id
          AND role IN ('OWNER', 'ADMIN')
    );
END;
$$ LANGUAGE plpgsql STABLE SECURITY DEFINER
SET search_path = public, pg_temp;

REVOKE ALL ON FUNCTION app_is_workspace_admin(UUID, UUID) FROM PUBLIC;
DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'rightfull_app') THEN
        GRANT EXECUTE ON FUNCTION app_is_workspace_admin(UUID, UUID) TO rightfull_app;
    END IF;
END $$;

-- ------------------------------------------------------------------------------
-- 3. Secure invitation lookup function for redemption
-- Prevents table-level enumeration while allowing single-use redemption by SHA-256 hash.
-- ------------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION lookup_invitation_for_redemption(p_token_hash VARCHAR(64))
RETURNS SETOF workspace_invitations AS $$
BEGIN
    IF p_token_hash IS NULL OR length(trim(p_token_hash)) = 0 THEN
        RETURN;
    END IF;
    RETURN QUERY
    SELECT * FROM workspace_invitations
    WHERE token_hash = p_token_hash
      AND status = 'PENDING'
      AND expires_at > CURRENT_TIMESTAMP;
END;
$$ LANGUAGE plpgsql STABLE SECURITY DEFINER
SET search_path = public, pg_temp;

REVOKE ALL ON FUNCTION lookup_invitation_for_redemption(VARCHAR) FROM PUBLIC;
DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'rightfull_app') THEN
        GRANT EXECUTE ON FUNCTION lookup_invitation_for_redemption(VARCHAR) TO rightfull_app;
    END IF;
END $$;

-- ------------------------------------------------------------------------------
-- 4. Remediate Workspace Invitations Policies
-- ------------------------------------------------------------------------------
DROP POLICY IF EXISTS workspace_invitations_select_policy ON workspace_invitations;
CREATE POLICY workspace_invitations_select_policy ON workspace_invitations
FOR SELECT TO PUBLIC
USING (
    workspace_id = app_current_workspace_id()
    OR email = app_current_user_email()
);

DROP POLICY IF EXISTS workspace_invitations_update_policy ON workspace_invitations;
CREATE POLICY workspace_invitations_update_policy ON workspace_invitations
FOR UPDATE TO PUBLIC
USING (
    workspace_id = app_current_workspace_id()
    OR email = app_current_user_email()
)
WITH CHECK (
    workspace_id = app_current_workspace_id()
    OR status IN ('ACCEPTED', 'REJECTED')
);

-- ------------------------------------------------------------------------------
-- 5. Remediate Workspace Members Insertion Policy
-- Prevents arbitrary self-insertion as OWNER and uses SECURITY DEFINER admin check
-- ------------------------------------------------------------------------------
DROP POLICY IF EXISTS workspace_members_insert_policy ON workspace_members;
CREATE POLICY workspace_members_insert_policy ON workspace_members
FOR INSERT TO PUBLIC
WITH CHECK (
    -- Case 1: Initial workspace owner provisioning
    (EXISTS (
        SELECT 1 FROM workspaces w 
        WHERE w.id = workspace_members.workspace_id 
          AND w.owner_id = app_current_user_id() 
          AND workspace_members.user_id = app_current_user_id()
    ))
    -- Case 2: Administrator adding a non-owner member within active tenant boundary
    OR (
        workspace_id = app_current_workspace_id() 
        AND workspace_members.role != 'OWNER' 
        AND app_is_workspace_admin(app_current_workspace_id(), app_current_user_id())
    )
    -- Case 3: Legitimate invitation acceptance by invited recipient
    OR (
        workspace_members.role != 'OWNER' 
        AND EXISTS (
            SELECT 1 FROM workspace_invitations wi 
            WHERE wi.workspace_id = workspace_members.workspace_id 
              AND wi.email = app_current_user_email() 
              AND wi.status = 'PENDING' 
              AND wi.role = workspace_members.role
        )
    )
);
