-- ==============================================================================
-- RIGHTFUL MIGRATION V1: SCHEMA BASELINE
-- ==============================================================================
-- Milestone 1: Initial Technical Foundation & Scaffolding.
-- Domain business tables (users, workspaces, products, plans, licenses, etc.)
-- are formally introduced in Milestone Step 3.3 (Entity Mapping & Security Design).
-- ==============================================================================

-- Create schema extension for UUID generation if supported
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Baseline system metadata table
CREATE TABLE IF NOT EXISTS system_metadata (
    key VARCHAR(64) PRIMARY KEY,
    value VARCHAR(255) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO system_metadata (key, value)
VALUES ('schema_version', '1.0.0-baseline')
ON CONFLICT (key) DO NOTHING;
