-- ==============================================================================
-- RIGHTFULL MIGRATION V8: LICENSE ENGINE, ACTIVATIONS & VALIDATION
-- ==============================================================================
-- Module: Core Licensing Engine - Licenses, Activations & Tenant-Safe Resolution
-- Target Database: PostgreSQL 17+ (Supabase)
-- ==============================================================================

-- ------------------------------------------------------------------------------
-- 1. Table: licenses
-- Workspace-isolated issued software licenses with HMAC digests and immutable snapshots.
-- ------------------------------------------------------------------------------
CREATE TABLE licenses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    customer_id UUID REFERENCES customers(id) ON DELETE SET NULL,
    plan_id UUID REFERENCES license_plans(id) ON DELETE SET NULL,
    plan_version INTEGER,
    license_key_hash VARCHAR(64) NOT NULL,
    license_key_prefix VARCHAR(16) NOT NULL,
    license_key_last_four VARCHAR(8) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    starts_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ, -- NULL denotes perpetual/lifetime license
    max_activations INTEGER NOT NULL DEFAULT 1,
    current_activations INTEGER NOT NULL DEFAULT 0,
    seat_limit INTEGER NOT NULL DEFAULT 1,
    offline_grace_period_seconds INTEGER NOT NULL DEFAULT 25200, -- 7 hours default
    revalidation_interval_seconds INTEGER NOT NULL DEFAULT 300,  -- 5 minutes default
    entitlements JSONB NOT NULL DEFAULT '{}'::jsonb,
    signing_key_id VARCHAR(64) NOT NULL,
    external_reference VARCHAR(255),
    idempotency_key VARCHAR(128),
    suspended_at TIMESTAMPTZ,
    suspended_reason TEXT,
    revoked_at TIMESTAMPTZ,
    revoked_reason TEXT,
    renewed_at TIMESTAMPTZ,
    version INTEGER NOT NULL DEFAULT 0,
    created_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_licenses_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'REVOKED', 'EXPIRED')),
    CONSTRAINT chk_licenses_activations CHECK (max_activations >= 1),
    CONSTRAINT chk_licenses_cur_activations CHECK (current_activations >= 0 AND current_activations <= max_activations),
    CONSTRAINT chk_licenses_seats CHECK (seat_limit >= 1),
    CONSTRAINT chk_licenses_grace_period CHECK (offline_grace_period_seconds >= 300 AND offline_grace_period_seconds <= 2592000),
    CONSTRAINT chk_licenses_revalidation CHECK (revalidation_interval_seconds >= 60 AND revalidation_interval_seconds <= 86400),
    CONSTRAINT uq_licenses_key_hash UNIQUE (license_key_hash),
    CONSTRAINT uq_licenses_id_workspace UNIQUE (id, workspace_id),
    CONSTRAINT fk_licenses_product_workspace
        FOREIGN KEY (product_id, workspace_id)
        REFERENCES products(id, workspace_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_licenses_customer_workspace
        FOREIGN KEY (customer_id, workspace_id)
        REFERENCES customers(id, workspace_id)
        ON DELETE SET NULL
);

CREATE INDEX idx_licenses_workspace ON licenses(workspace_id);
CREATE INDEX idx_licenses_product ON licenses(product_id);
CREATE INDEX idx_licenses_customer ON licenses(customer_id);
CREATE INDEX idx_licenses_workspace_status ON licenses(workspace_id, status);
CREATE INDEX idx_licenses_key_prefix ON licenses(license_key_prefix);
CREATE INDEX idx_licenses_key_hash ON licenses(license_key_hash);
CREATE INDEX idx_licenses_expires_at ON licenses(expires_at) WHERE expires_at IS NOT NULL;
CREATE UNIQUE INDEX idx_licenses_workspace_idempotency ON licenses(workspace_id, idempotency_key) WHERE idempotency_key IS NOT NULL;
CREATE INDEX idx_licenses_entitlements ON licenses USING gin (entitlements);

ALTER TABLE licenses ENABLE ROW LEVEL SECURITY;

-- RLS Policies for licenses
CREATE POLICY licenses_select_policy ON licenses
FOR SELECT TO PUBLIC
USING (workspace_id = app_current_workspace_id());

CREATE POLICY licenses_insert_policy ON licenses
FOR INSERT TO PUBLIC
WITH CHECK (workspace_id = app_current_workspace_id());

CREATE POLICY licenses_update_policy ON licenses
FOR UPDATE TO PUBLIC
USING (workspace_id = app_current_workspace_id())
WITH CHECK (workspace_id = app_current_workspace_id());

CREATE POLICY licenses_delete_policy ON licenses
FOR DELETE TO PUBLIC
USING (FALSE);

-- ------------------------------------------------------------------------------
-- 2. Table: license_activations
-- Tracks registered client hardware/devices bound to an issued software license.
-- ------------------------------------------------------------------------------
CREATE TABLE license_activations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    license_id UUID NOT NULL REFERENCES licenses(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    device_identifier_hash VARCHAR(64) NOT NULL,
    device_name VARCHAR(255),
    device_platform VARCHAR(100),
    app_version VARCHAR(64),
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    activated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_validated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deactivated_at TIMESTAMPTZ,
    deactivated_by VARCHAR(32),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_activations_status CHECK (status IN ('ACTIVE', 'DEACTIVATED')),
    CONSTRAINT chk_activations_deactivated_by CHECK (deactivated_by IS NULL OR deactivated_by IN ('CLIENT', 'ADMIN')),
    CONSTRAINT uq_activations_license_device UNIQUE (license_id, device_identifier_hash),
    CONSTRAINT uq_activations_id_workspace UNIQUE (id, workspace_id),
    CONSTRAINT fk_activations_license_workspace
        FOREIGN KEY (license_id, workspace_id)
        REFERENCES licenses(id, workspace_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_activations_product_workspace
        FOREIGN KEY (product_id, workspace_id)
        REFERENCES products(id, workspace_id)
        ON DELETE CASCADE
);

CREATE INDEX idx_activations_license ON license_activations(license_id);
CREATE INDEX idx_activations_license_status ON license_activations(license_id, status);
CREATE INDEX idx_activations_workspace ON license_activations(workspace_id);
CREATE INDEX idx_activations_product ON license_activations(product_id);
CREATE INDEX idx_activations_device_hash ON license_activations(device_identifier_hash);

ALTER TABLE license_activations ENABLE ROW LEVEL SECURITY;

-- RLS Policies for license_activations
CREATE POLICY activations_select_policy ON license_activations
FOR SELECT TO PUBLIC
USING (workspace_id = app_current_workspace_id());

CREATE POLICY activations_insert_policy ON license_activations
FOR INSERT TO PUBLIC
WITH CHECK (workspace_id = app_current_workspace_id());

CREATE POLICY activations_update_policy ON license_activations
FOR UPDATE TO PUBLIC
USING (workspace_id = app_current_workspace_id())
WITH CHECK (workspace_id = app_current_workspace_id());

CREATE POLICY activations_delete_policy ON license_activations
FOR DELETE TO PUBLIC
USING (FALSE);

-- ------------------------------------------------------------------------------
-- 3. Security Definer Helper: fn_resolve_product_workspace
-- Securely resolves workspace_id from product_id for public validation endpoints.
-- ------------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_resolve_product_workspace(p_product_id UUID)
RETURNS UUID
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
DECLARE
    v_workspace_id UUID;
BEGIN
    SELECT workspace_id INTO v_workspace_id
    FROM products
    WHERE id = p_product_id AND status = 'ACTIVE';
    RETURN v_workspace_id;
END;
$$;

-- ------------------------------------------------------------------------------
-- 4. Grant Application Role Privileges (rightfull_app)
-- ------------------------------------------------------------------------------
DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'rightfull_app') THEN
        GRANT SELECT, INSERT, UPDATE ON licenses TO rightfull_app;
        GRANT SELECT, INSERT, UPDATE ON license_activations TO rightfull_app;
        REVOKE ALL ON FUNCTION fn_resolve_product_workspace(UUID) FROM PUBLIC;
        GRANT EXECUTE ON FUNCTION fn_resolve_product_workspace(UUID) TO rightfull_app;
    END IF;
END $$;

