-- ==============================================================================
-- RIGHTFULL MIGRATION V6: SUPABASE SECURITY ADVISOR REMEDIATIONS
-- ==============================================================================
-- 1. Fix Mutable search_path on V3 SECURITY DEFINER functions.
-- 2. Revoke PUBLIC EXECUTE permissions on all SECURITY DEFINER functions.
-- 3. Eliminate redundant duplicate indexes while preserving unique constraint backing indexes.
-- Target Database: PostgreSQL 17+ (Supabase)
-- ==============================================================================

-- ------------------------------------------------------------------------------
-- 1. Function search_path and execution privilege hardening
-- ------------------------------------------------------------------------------
ALTER FUNCTION public.app_current_user_id() SET search_path = public, pg_temp;
REVOKE ALL ON FUNCTION public.app_current_user_id() FROM PUBLIC;
DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'rightfull_app') THEN
        GRANT EXECUTE ON FUNCTION public.app_current_user_id() TO rightfull_app;
    END IF;
END $$;

ALTER FUNCTION public.app_current_workspace_id() SET search_path = public, pg_temp;
REVOKE ALL ON FUNCTION public.app_current_workspace_id() FROM PUBLIC;
DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'rightfull_app') THEN
        GRANT EXECUTE ON FUNCTION public.app_current_workspace_id() TO rightfull_app;
    END IF;
END $$;

ALTER FUNCTION public.app_current_supabase_user_id() SET search_path = public, pg_temp;
REVOKE ALL ON FUNCTION public.app_current_supabase_user_id() FROM PUBLIC;
DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'rightfull_app') THEN
        GRANT EXECUTE ON FUNCTION public.app_current_supabase_user_id() TO rightfull_app;
    END IF;
END $$;

REVOKE ALL ON FUNCTION public.app_current_user_email() FROM PUBLIC;
DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'rightfull_app') THEN
        GRANT EXECUTE ON FUNCTION public.app_current_user_email() TO rightfull_app;
    END IF;
END $$;

-- ------------------------------------------------------------------------------
-- 2. Duplicate Index Remediation
-- Drop strictly redundant explicit indexes that duplicate constraint-backing indexes
-- ------------------------------------------------------------------------------
DROP INDEX IF EXISTS public.idx_users_email;
DROP INDEX IF EXISTS public.idx_users_supabase_id;
DROP INDEX IF EXISTS public.idx_workspaces_slug;
DROP INDEX IF EXISTS public.idx_user_preferences_user_id;
DROP INDEX IF EXISTS public.idx_workspace_invitations_token_hash;
