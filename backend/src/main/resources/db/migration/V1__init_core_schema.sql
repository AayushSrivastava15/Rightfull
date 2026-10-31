-- ==============================================================================
-- RIGHTFULL MIGRATION V1: CORE IDENTITY & MULTI-TENANT WORKSPACE SCHEMA
-- ==============================================================================
-- Module: Identity & Multi-Tenancy Foundation
-- Target Database: PostgreSQL 17+ (Supabase)
-- Author: Rightfull Engineering
-- ==============================================================================

-- ------------------------------------------------------------------------------
-- 1. Table: users
-- Represents the global application user profile mapped to Supabase Auth.
-- Zero passwords or authentication credentials stored.
-- ------------------------------------------------------------------------------
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    supabase_user_id UUID NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    full_name VARCHAR(255),
    avatar_url TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_users_email_non_empty CHECK (length(trim(email)) > 0)
);

CREATE UNIQUE INDEX idx_users_supabase_id ON users(supabase_user_id);
CREATE UNIQUE INDEX idx_users_email ON users(email);

-- ------------------------------------------------------------------------------
-- 2. Table: workspaces
-- Root multi-tenant boundary representing a software vendor or development team.
-- ------------------------------------------------------------------------------
CREATE TABLE workspaces (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(128) NOT NULL,
    slug VARCHAR(64) NOT NULL UNIQUE,
    owner_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    tier VARCHAR(32) NOT NULL DEFAULT 'COMMUNITY',
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_workspaces_tier CHECK (tier IN ('COMMUNITY', 'PRO', 'ENTERPRISE')),
    CONSTRAINT chk_workspaces_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'ARCHIVED')),
    CONSTRAINT chk_workspaces_slug_format CHECK (slug ~ '^[a-z0-9-]+$')
);

CREATE UNIQUE INDEX idx_workspaces_slug ON workspaces(slug);
CREATE INDEX idx_workspaces_owner_id ON workspaces(owner_id);

-- ------------------------------------------------------------------------------
-- 3. Table: user_preferences
-- 1:1 user settings (theme, notifications, default active workspace).
-- ------------------------------------------------------------------------------
CREATE TABLE user_preferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    default_workspace_id UUID REFERENCES workspaces(id) ON DELETE SET NULL,
    theme VARCHAR(32) NOT NULL DEFAULT 'dark',
    email_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    ui_preferences JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_user_preferences_theme CHECK (theme IN ('light', 'dark', 'system'))
);

CREATE UNIQUE INDEX idx_user_preferences_user_id ON user_preferences(user_id);
CREATE INDEX idx_user_preferences_default_workspace ON user_preferences(default_workspace_id);

-- ------------------------------------------------------------------------------
-- 4. Table: workspace_members
-- Maps users to workspaces with role-based access control (RBAC).
-- ------------------------------------------------------------------------------
CREATE TABLE workspace_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(32) NOT NULL DEFAULT 'MEMBER',
    invited_by UUID REFERENCES users(id) ON DELETE SET NULL,
    joined_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_workspace_user UNIQUE (workspace_id, user_id),
    CONSTRAINT chk_workspace_members_role CHECK (role IN ('OWNER', 'ADMIN', 'MEMBER', 'VIEWER'))
);

CREATE INDEX idx_workspace_members_workspace_id ON workspace_members(workspace_id);
CREATE INDEX idx_workspace_members_user_id ON workspace_members(user_id);
CREATE INDEX idx_workspace_members_role ON workspace_members(workspace_id, role);

-- ------------------------------------------------------------------------------
-- 5. Row Level Security (RLS) Configuration
-- Default-deny posture: RLS is enabled; zero permissive public policies exist.
-- Direct PostgREST / Supabase Client access is blocked by default.
-- ------------------------------------------------------------------------------
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE workspaces ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_preferences ENABLE ROW LEVEL SECURITY;
ALTER TABLE workspace_members ENABLE ROW LEVEL SECURITY;

