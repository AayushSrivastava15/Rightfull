-- ==============================================================================
-- RIGHTFULL MIGRATION V5: PRODUCT CATALOG & ED25519 SIGNING KEYS
-- ==============================================================================
-- Module: Core Licensing Foundation - Product Catalog & Key Lifecycle
-- Target Database: PostgreSQL 17+ (Supabase)
-- ==============================================================================

-- ------------------------------------------------------------------------------
-- 1. Table: products
-- Workspace-isolated software products and metadata.
-- ------------------------------------------------------------------------------
CREATE TABLE products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(100) NOT NULL,
    description TEXT,
    version VARCHAR(50),
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    archived_at TIMESTAMPTZ,
    CONSTRAINT chk_products_status CHECK (status IN ('ACTIVE', 'ARCHIVED')),
    CONSTRAINT uq_products_workspace_slug UNIQUE (workspace_id, slug),
    CONSTRAINT uq_products_id_workspace UNIQUE (id, workspace_id)
);

CREATE INDEX idx_products_workspace ON products(workspace_id);
CREATE INDEX idx_products_workspace_status ON products(workspace_id, status);
CREATE INDEX idx_products_created_at ON products(workspace_id, created_at DESC);

ALTER TABLE products ENABLE ROW LEVEL SECURITY;

-- ------------------------------------------------------------------------------
-- 2. Table: product_signing_keys
-- Product-specific Ed25519 signing keys with AES-256-GCM encrypted private keys at rest.
-- Composite foreign key guarantees product and key tenant consistency.
-- ------------------------------------------------------------------------------
CREATE TABLE product_signing_keys (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    key_id VARCHAR(64) NOT NULL,
    algorithm VARCHAR(32) NOT NULL DEFAULT 'ED25519',
    public_key TEXT NOT NULL,
    encrypted_private_key BYTEA NOT NULL,
    encryption_nonce BYTEA NOT NULL,
    encryption_version VARCHAR(16) NOT NULL DEFAULT 'v1',
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    rotated_at TIMESTAMPTZ,
    retired_at TIMESTAMPTZ,
    CONSTRAINT chk_signing_keys_status CHECK (status IN ('ACTIVE', 'RETIRED', 'REVOKED')),
    CONSTRAINT uq_product_key_id UNIQUE (product_id, key_id),
    CONSTRAINT fk_signing_keys_product_workspace
        FOREIGN KEY (product_id, workspace_id)
        REFERENCES products(id, workspace_id)
        ON DELETE CASCADE
);

CREATE INDEX idx_signing_keys_product_status ON product_signing_keys(product_id, status);
CREATE INDEX idx_signing_keys_workspace ON product_signing_keys(workspace_id);

-- Enforce exactly one ACTIVE signing key per product at the database level
CREATE UNIQUE INDEX uq_product_signing_keys_one_active
ON product_signing_keys (product_id)
WHERE status = 'ACTIVE';

ALTER TABLE product_signing_keys ENABLE ROW LEVEL SECURITY;

-- ------------------------------------------------------------------------------
-- 3. Row-Level Security (RLS) Policies for products
-- Enforces archive-only lifecycle (DELETE denied at database level).
-- ------------------------------------------------------------------------------
DROP POLICY IF EXISTS products_select_policy ON products;
CREATE POLICY products_select_policy ON products
FOR SELECT TO PUBLIC
USING (workspace_id = app_current_workspace_id());

DROP POLICY IF EXISTS products_insert_policy ON products;
CREATE POLICY products_insert_policy ON products
FOR INSERT TO PUBLIC
WITH CHECK (workspace_id = app_current_workspace_id());

DROP POLICY IF EXISTS products_update_policy ON products;
CREATE POLICY products_update_policy ON products
FOR UPDATE TO PUBLIC
USING (workspace_id = app_current_workspace_id())
WITH CHECK (workspace_id = app_current_workspace_id());

DROP POLICY IF EXISTS products_delete_policy ON products;
CREATE POLICY products_delete_policy ON products
FOR DELETE TO PUBLIC
USING (FALSE);

-- ------------------------------------------------------------------------------
-- 4. Row-Level Security (RLS) Policies for product_signing_keys
-- Cryptographic keys are immutable; DELETE is permanently denied.
-- ------------------------------------------------------------------------------
DROP POLICY IF EXISTS product_signing_keys_select_policy ON product_signing_keys;
CREATE POLICY product_signing_keys_select_policy ON product_signing_keys
FOR SELECT TO PUBLIC
USING (workspace_id = app_current_workspace_id());

DROP POLICY IF EXISTS product_signing_keys_insert_policy ON product_signing_keys;
CREATE POLICY product_signing_keys_insert_policy ON product_signing_keys
FOR INSERT TO PUBLIC
WITH CHECK (workspace_id = app_current_workspace_id());

DROP POLICY IF EXISTS product_signing_keys_update_policy ON product_signing_keys;
CREATE POLICY product_signing_keys_update_policy ON product_signing_keys
FOR UPDATE TO PUBLIC
USING (workspace_id = app_current_workspace_id())
WITH CHECK (workspace_id = app_current_workspace_id());

DROP POLICY IF EXISTS product_signing_keys_delete_policy ON product_signing_keys;
CREATE POLICY product_signing_keys_delete_policy ON product_signing_keys
FOR DELETE TO PUBLIC
USING (FALSE);

-- ------------------------------------------------------------------------------
-- 5. Application Role Privileges (rightfull_app)
-- ------------------------------------------------------------------------------
DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'rightfull_app') THEN
        GRANT SELECT, INSERT, UPDATE ON products TO rightfull_app;
        GRANT SELECT, INSERT, UPDATE ON product_signing_keys TO rightfull_app;
    END IF;
END $$;
