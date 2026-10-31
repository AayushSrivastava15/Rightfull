-- ==============================================================================
-- RIGHTFULL MIGRATION V2: WORKSPACE INVITATIONS SCHEMA
-- ==============================================================================
-- Module: Workspace Collaboration & Team Management
-- Target Database: PostgreSQL 17+ (Supabase)
-- Author: Rightfull Engineering
-- ==============================================================================

CREATE TABLE workspace_invitations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    email VARCHAR(255) NOT NULL,
    role VARCHAR(32) NOT NULL DEFAULT 'MEMBER',
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    invited_by UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    expires_at TIMESTAMPTZ NOT NULL,
    accepted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_workspace_invitations_email_non_empty CHECK (length(trim(email)) > 0),
    CONSTRAINT chk_workspace_invitations_role CHECK (role IN ('ADMIN', 'MEMBER', 'VIEWER')),
    CONSTRAINT chk_workspace_invitations_status CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'REVOKED'))
);

CREATE UNIQUE INDEX idx_workspace_invitations_token_hash ON workspace_invitations(token_hash);
CREATE INDEX idx_workspace_invitations_workspace_id ON workspace_invitations(workspace_id);
CREATE INDEX idx_workspace_invitations_lookup ON workspace_invitations(workspace_id, email);
CREATE INDEX idx_workspace_invitations_workspace_status ON workspace_invitations(workspace_id, status);

-- Default-deny Row Level Security (RLS)
ALTER TABLE workspace_invitations ENABLE ROW LEVEL SECURITY;

