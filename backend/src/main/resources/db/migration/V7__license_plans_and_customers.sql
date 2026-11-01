-- ==============================================================================
-- RIGHTFULL MIGRATION V7: LICENSE PLANS, ENTITLEMENTS & CUSTOMER MANAGEMENT
-- ==============================================================================
-- Module: Core Licensing Foundation - License Plans & Customers
-- Target Database: PostgreSQL 17+ (Supabase)
-- ==============================================================================

-- ------------------------------------------------------------------------------
-- 1. Table: license_plans
-- Product-specific licensing plans with duration, seats, grace period, and entitlements.
-- Composite foreign key guarantees plan and product tenant consistency.
-- ------------------------------------------------------------------------------
CREATE TABLE license_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    duration_days INTEGER DEFAULT 365, -- Nullable where appropriate (null = perpetual/lifetime license)
    maximum_activations INTEGER NOT NULL DEFAULT 1,
    seat_limit INTEGER NOT NULL DEFAULT 1,
    offline_grace_period_seconds INTEGER NOT NULL DEFAULT 25200, -- 7 hours default
    revalidation_interval_seconds INTEGER NOT NULL DEFAULT 300,  -- 5 minutes default
    entitlements JSONB NOT NULL DEFAULT '{}'::jsonb,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    version INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    archived_at TIMESTAMPTZ,
    CONSTRAINT chk_plans_status CHECK (status IN ('ACTIVE', 'ARCHIVED')),
    CONSTRAINT chk_plans_duration CHECK (duration_days IS NULL OR duration_days >= 0),
    CONSTRAINT chk_plans_activations CHECK (maximum_activations >= 1),
    CONSTRAINT chk_plans_seats CHECK (seat_limit >= 1),
    CONSTRAINT chk_plans_grace_period CHECK (offline_grace_period_seconds >= 300 AND offline_grace_period_seconds <= 2592000),
    CONSTRAINT chk_plans_revalidation CHECK (revalidation_interval_seconds >= 60 AND revalidation_interval_seconds <= 86400),
    CONSTRAINT uq_plans_product_name UNIQUE (product_id, name),
    CONSTRAINT uq_plans_id_workspace UNIQUE (id, workspace_id),
    CONSTRAINT fk_plans_product_workspace
        FOREIGN KEY (product_id, workspace_id)
        REFERENCES products(id, workspace_id)
        ON DELETE CASCADE
);

CREATE INDEX idx_plans_product ON license_plans(product_id);
CREATE INDEX idx_plans_workspace ON license_plans(workspace_id);
CREATE INDEX idx_plans_workspace_status ON license_plans(workspace_id, status);
CREATE INDEX idx_plans_entitlements ON license_plans USING gin (entitlements);

ALTER TABLE license_plans ENABLE ROW LEVEL SECURITY;

-- RLS Policies for license_plans
CREATE POLICY license_plans_select_policy ON license_plans
FOR SELECT TO PUBLIC
USING (workspace_id = app_current_workspace_id());

CREATE POLICY license_plans_insert_policy ON license_plans
FOR INSERT TO PUBLIC
WITH CHECK (workspace_id = app_current_workspace_id());

CREATE POLICY license_plans_update_policy ON license_plans
FOR UPDATE TO PUBLIC
USING (workspace_id = app_current_workspace_id())
WITH CHECK (workspace_id = app_current_workspace_id());

CREATE POLICY license_plans_delete_policy ON license_plans
FOR DELETE TO PUBLIC
USING (FALSE);

-- ------------------------------------------------------------------------------
-- 2. Table: customers
-- Workspace-isolated customer directory for software license assignment.
-- ------------------------------------------------------------------------------
CREATE TABLE customers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    company VARCHAR(255),
    external_reference VARCHAR(255),
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    archived_at TIMESTAMPTZ,
    CONSTRAINT chk_customers_status CHECK (status IN ('ACTIVE', 'ARCHIVED')),
    CONSTRAINT chk_customers_email_non_empty CHECK (length(trim(email)) > 0),
    CONSTRAINT uq_customers_workspace_email UNIQUE (workspace_id, email),
    CONSTRAINT uq_customers_id_workspace UNIQUE (id, workspace_id)
);

CREATE INDEX idx_customers_workspace ON customers(workspace_id);
CREATE INDEX idx_customers_workspace_status ON customers(workspace_id, status);
CREATE INDEX idx_customers_workspace_email ON customers(workspace_id, email);
CREATE UNIQUE INDEX idx_customers_workspace_ext_ref ON customers(workspace_id, external_reference) WHERE external_reference IS NOT NULL;
CREATE INDEX idx_customers_metadata ON customers USING gin (metadata);

ALTER TABLE customers ENABLE ROW LEVEL SECURITY;

-- RLS Policies for customers
CREATE POLICY customers_select_policy ON customers
FOR SELECT TO PUBLIC
USING (workspace_id = app_current_workspace_id());

CREATE POLICY customers_insert_policy ON customers
FOR INSERT TO PUBLIC
WITH CHECK (workspace_id = app_current_workspace_id());

CREATE POLICY customers_update_policy ON customers
FOR UPDATE TO PUBLIC
USING (workspace_id = app_current_workspace_id())
WITH CHECK (workspace_id = app_current_workspace_id());

CREATE POLICY customers_delete_policy ON customers
FOR DELETE TO PUBLIC
USING (FALSE);

-- ------------------------------------------------------------------------------
-- 3. Table: license_plan_versions
-- Immutable historical version records for license configurations.
-- Preserves past entitlement definitions and ensures previous configurations remain recoverable.
-- ------------------------------------------------------------------------------
CREATE TABLE license_plan_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_id UUID NOT NULL REFERENCES license_plans(id) ON DELETE CASCADE,
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    version INTEGER NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    duration_days INTEGER,
    maximum_activations INTEGER NOT NULL,
    seat_limit INTEGER NOT NULL,
    offline_grace_period_seconds INTEGER NOT NULL,
    revalidation_interval_seconds INTEGER NOT NULL,
    entitlements JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT uq_plan_versions_plan_ver UNIQUE (plan_id, version),
    CONSTRAINT fk_plan_versions_product_workspace
        FOREIGN KEY (product_id, workspace_id)
        REFERENCES products(id, workspace_id)
        ON DELETE CASCADE
);

CREATE INDEX idx_plan_versions_plan ON license_plan_versions(plan_id);
CREATE INDEX idx_plan_versions_workspace ON license_plan_versions(workspace_id);
CREATE INDEX idx_plan_versions_entitlements ON license_plan_versions USING gin (entitlements);

ALTER TABLE license_plan_versions ENABLE ROW LEVEL SECURITY;

CREATE POLICY plan_versions_select_policy ON license_plan_versions
FOR SELECT TO PUBLIC
USING (workspace_id = app_current_workspace_id());

CREATE POLICY plan_versions_insert_policy ON license_plan_versions
FOR INSERT TO PUBLIC
WITH CHECK (workspace_id = app_current_workspace_id());

-- Historical versions are strictly append-only: updates and deletes are permanently denied
CREATE POLICY plan_versions_update_policy ON license_plan_versions
FOR UPDATE TO PUBLIC
USING (FALSE);

CREATE POLICY plan_versions_delete_policy ON license_plan_versions
FOR DELETE TO PUBLIC
USING (FALSE);

-- ------------------------------------------------------------------------------
-- 4. Compatibility Views: license_configurations and license_configuration_versions
-- Provides clean aliases for reusable license restriction configuration presets.
-- ------------------------------------------------------------------------------
CREATE OR REPLACE VIEW license_configurations AS
    SELECT * FROM license_plans;

CREATE OR REPLACE VIEW license_configuration_versions AS
    SELECT * FROM license_plan_versions;

-- ------------------------------------------------------------------------------
-- 5. Grant Application Role Privileges (rightfull_app)
-- ------------------------------------------------------------------------------
DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'rightfull_app') THEN
        GRANT SELECT, INSERT, UPDATE ON license_plans TO rightfull_app;
        GRANT SELECT, INSERT, UPDATE ON customers TO rightfull_app;
        GRANT SELECT, INSERT ON license_plan_versions TO rightfull_app;
        GRANT SELECT ON license_configurations TO rightfull_app;
        GRANT SELECT ON license_configuration_versions TO rightfull_app;
    END IF;
END $$;

