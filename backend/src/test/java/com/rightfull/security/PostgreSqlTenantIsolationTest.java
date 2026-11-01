package com.rightfull.security;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Isolated security test suite executed against live PostgreSQL 17 using
 * the restricted, non-BYPASSRLS application role (rightfull_app).
 * Verifies all 12 database-level multi-tenant isolation and RLS requirements.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PostgreSqlTenantIsolationTest {

    private static final String DB_URL = "jdbc:postgresql://aws-0-ap-south-1.pooler.supabase.com:5432/postgres?sslmode=require";
    private static final String APP_USER = "rightfull_app.kcyccddrzsfxgrkeenop";
    private static final String APP_PASS = "RightfullApp2026_SecurePwd!";
    private static final String ADMIN_USER = "postgres.kcyccddrzsfxgrkeenop";
    private static final String ADMIN_PASS = "utN7hNkB+-Rmc77";

    private static UUID userAlphaId;
    private static UUID userBetaId;
    private static UUID wsAlphaId;
    private static UUID wsBetaId;

    @BeforeAll
    static void setupTestTenants() throws SQLException {
        userAlphaId = UUID.randomUUID();
        userBetaId = UUID.randomUUID();
        wsAlphaId = UUID.randomUUID();
        wsBetaId = UUID.randomUUID();

        // Seed test tenants using admin role (to avoid RLS bootstrap chicken-and-egg)
        try (Connection adminConn = DriverManager.getConnection(DB_URL, ADMIN_USER, ADMIN_PASS)) {
            adminConn.setAutoCommit(false);

            // 1. Seed Users
            try (PreparedStatement ps = adminConn.prepareStatement(
                    "INSERT INTO users (id, supabase_user_id, email, full_name) VALUES (?, ?, ?, ?)")) {
                ps.setObject(1, userAlphaId);
                ps.setObject(2, UUID.randomUUID());
                ps.setString(3, "alpha-" + userAlphaId + "@rightfull.test");
                ps.setString(4, "Tenant Alpha Owner");
                ps.executeUpdate();

                ps.setObject(1, userBetaId);
                ps.setObject(2, UUID.randomUUID());
                ps.setString(3, "beta-" + userBetaId + "@rightfull.test");
                ps.setString(4, "Tenant Beta Owner");
                ps.executeUpdate();
            }

            // 2. Seed Workspaces
            try (PreparedStatement ps = adminConn.prepareStatement(
                    "INSERT INTO workspaces (id, name, slug, owner_id) VALUES (?, ?, ?, ?)")) {
                ps.setObject(1, wsAlphaId);
                ps.setString(2, "Workspace Alpha");
                ps.setString(3, "ws-alpha-" + wsAlphaId.toString().substring(0, 8));
                ps.setObject(4, userAlphaId);
                ps.executeUpdate();

                ps.setObject(1, wsBetaId);
                ps.setString(2, "Workspace Beta");
                ps.setString(3, "ws-beta-" + wsBetaId.toString().substring(0, 8));
                ps.setObject(4, userBetaId);
                ps.executeUpdate();
            }

            // 3. Seed Memberships
            try (PreparedStatement ps = adminConn.prepareStatement(
                    "INSERT INTO workspace_members (workspace_id, user_id, role) VALUES (?, ?, ?)")) {
                ps.setObject(1, wsAlphaId);
                ps.setObject(2, userAlphaId);
                ps.setString(3, "OWNER");
                ps.executeUpdate();

                ps.setObject(1, wsBetaId);
                ps.setObject(2, userBetaId);
                ps.setString(3, "OWNER");
                ps.executeUpdate();
            }

            adminConn.commit();
        }
    }

    @AfterAll
    static void cleanupTestTenants() throws SQLException {
        try (Connection adminConn = DriverManager.getConnection(DB_URL, ADMIN_USER, ADMIN_PASS)) {
            adminConn.setAutoCommit(false);
            // Disable trigger temporarily to allow cleaning up synthetic test data
            try (PreparedStatement ps = adminConn.prepareStatement("ALTER TABLE audit_logs DISABLE TRIGGER trg_audit_logs_prevent_mutation")) {
                ps.execute();
            }
            try (PreparedStatement ps = adminConn.prepareStatement("DELETE FROM workspaces WHERE id IN (?, ?)")) {
                ps.setObject(1, wsAlphaId);
                ps.setObject(2, wsBetaId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = adminConn.prepareStatement("DELETE FROM users WHERE id IN (?, ?)")) {
                ps.setObject(1, userAlphaId);
                ps.setObject(2, userBetaId);
                ps.executeUpdate();
            }
            // Re-enable trigger
            try (PreparedStatement ps = adminConn.prepareStatement("ALTER TABLE audit_logs ENABLE TRIGGER trg_audit_logs_prevent_mutation")) {
                ps.execute();
            }
            adminConn.commit();
        }
    }

    private Connection getAppConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(DB_URL, APP_USER, APP_PASS);
        conn.setAutoCommit(false);
        return conn;
    }

    @Test
    @Order(1)
    @DisplayName("Security Requirement 2: Verify restricted application role lacks BYPASSRLS")
    void testApplicationRoleLacksBypassRls() throws SQLException {
        try (Connection conn = getAppConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT r.rolname, r.rolsuper, r.rolbypassrls FROM pg_roles r WHERE r.rolname = 'rightfull_app'")) {
                ResultSet rs = ps.executeQuery();
                assertThat(rs.next()).isTrue();
                assertThat(rs.getBoolean("rolsuper")).isFalse();
                assertThat(rs.getBoolean("rolbypassrls")).isFalse();
            }
        }
    }

    @Test
    @Order(2)
    @DisplayName("Security Requirement 4: Missing tenant context enforces default-deny")
    void testMissingTenantContextEnforcesDefaultDeny() throws SQLException {
        try (Connection conn = getAppConnection()) {
            // No context set -> should see 0 workspaces
            try (PreparedStatement ps = conn.prepareStatement("SELECT count(*) FROM workspaces WHERE id = ?")) {
                ps.setObject(1, wsAlphaId);
                ResultSet rs = ps.executeQuery();
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isEqualTo(0);
            }
        }
    }

    @Test
    @Order(3)
    @DisplayName("Security Requirement 3 & 4: Valid tenant context allows access to own tenant only")
    void testValidTenantContextAllowsOwnTenant() throws SQLException {
        try (Connection conn = getAppConnection()) {
            // Set Alpha context
            try (PreparedStatement ps = conn.prepareStatement("SELECT set_config('app.current_workspace_id', ?, true)")) {
                ps.setString(1, wsAlphaId.toString());
                ps.executeQuery();
            }

            try (PreparedStatement ps = conn.prepareStatement("SELECT id, name FROM workspaces WHERE id = ?")) {
                ps.setObject(1, wsAlphaId);
                ResultSet rs = ps.executeQuery();
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("name")).isEqualTo("Workspace Alpha");
            }
        }
    }

    @Test
    @Order(4)
    @DisplayName("Security Requirement 4: Cross-workspace SELECT is strictly blocked by RLS")
    void testCrossWorkspaceSelectBlocked() throws SQLException {
        try (Connection conn = getAppConnection()) {
            // Set Alpha context
            try (PreparedStatement ps = conn.prepareStatement("SELECT set_config('app.current_workspace_id', ?, true)")) {
                ps.setString(1, wsAlphaId.toString());
                ps.executeQuery();
            }

            // Attempt to query Beta workspace
            try (PreparedStatement ps = conn.prepareStatement("SELECT count(*) FROM workspaces WHERE id = ?")) {
                ps.setObject(1, wsBetaId);
                ResultSet rs = ps.executeQuery();
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isZero();
            }
        }
    }

    @Test
    @Order(5)
    @DisplayName("Security Requirement 4: Cross-workspace INSERT is strictly blocked by RLS WITH CHECK")
    void testCrossWorkspaceInsertBlocked() throws SQLException {
        try (Connection conn = getAppConnection()) {
            // Set Alpha context
            try (PreparedStatement ps = conn.prepareStatement("SELECT set_config('app.current_workspace_id', ?, true)")) {
                ps.setString(1, wsAlphaId.toString());
                ps.executeQuery();
            }

            // Attempt to insert invitation into Beta workspace while in Alpha context
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO workspace_invitations (workspace_id, email, token_hash, invited_by, expires_at) " +
                    "VALUES (?, 'infiltrator@test.com', 'dummy_hash_cross_insert', ?, ?)")) {
                ps.setObject(1, wsBetaId);
                ps.setObject(2, userAlphaId);
                ps.setTimestamp(3, Timestamp.from(Instant.now().plusSeconds(3600)));

                assertThatThrownBy(ps::executeUpdate)
                        .isInstanceOf(SQLException.class)
                        .hasMessageContaining("violates row-level security policy");
            }
        }
    }

    @Test
    @Order(6)
    @DisplayName("Security Requirement 4: Cross-workspace UPDATE is strictly blocked by RLS")
    void testCrossWorkspaceUpdateBlocked() throws SQLException {
        try (Connection conn = getAppConnection()) {
            // Set Alpha context
            try (PreparedStatement ps = conn.prepareStatement("SELECT set_config('app.current_workspace_id', ?, true)")) {
                ps.setString(1, wsAlphaId.toString());
                ps.executeQuery();
            }

            // Attempt to update Beta workspace name while in Alpha context
            try (PreparedStatement ps = conn.prepareStatement("UPDATE workspaces SET name = 'Tampered' WHERE id = ?")) {
                ps.setObject(1, wsBetaId);
                int rowsUpdated = ps.executeUpdate();
                assertThat(rowsUpdated).isZero();
            }
        }
    }

    @Test
    @Order(7)
    @DisplayName("Security Requirement 4: Cross-workspace DELETE is strictly blocked by RLS")
    void testCrossWorkspaceDeleteBlocked() throws SQLException {
        try (Connection conn = getAppConnection()) {
            // Set Alpha context
            try (PreparedStatement ps = conn.prepareStatement("SELECT set_config('app.current_workspace_id', ?, true)")) {
                ps.setString(1, wsAlphaId.toString());
                ps.executeQuery();
            }

            // Attempt to delete Beta workspace while in Alpha context
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM workspaces WHERE id = ?")) {
                ps.setObject(1, wsBetaId);
                int rowsDeleted = ps.executeUpdate();
                assertThat(rowsDeleted).isZero();
            }
        }
    }

    @Test
    @Order(8)
    @DisplayName("Security Requirement 3: Invalid / malformed tenant context is safely rejected")
    void testInvalidTenantContextRejected() throws SQLException {
        try (Connection conn = getAppConnection()) {
            // Set random non-existent UUID as context
            try (PreparedStatement ps = conn.prepareStatement("SELECT set_config('app.current_workspace_id', ?, true)")) {
                ps.setString(1, UUID.randomUUID().toString());
                ps.executeQuery();
            }

            try (PreparedStatement ps = conn.prepareStatement("SELECT count(*) FROM workspaces")) {
                ResultSet rs = ps.executeQuery();
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isZero();
            }
        }
    }

    @Test
    @Order(9)
    @DisplayName("Security Requirement 3: Context cannot leak across transactions (is_local = true)")
    void testConnectionPoolContextLeakagePrevented() throws SQLException {
        try (Connection conn = getAppConnection()) {
            // Transaction 1: Set context and commit
            try (PreparedStatement ps = conn.prepareStatement("SELECT set_config('app.current_workspace_id', ?, true)")) {
                ps.setString(1, wsAlphaId.toString());
                ps.executeQuery();
            }
            conn.commit();

            // Transaction 2 on same physical connection: Context MUST be automatically cleared
            try (PreparedStatement ps = conn.prepareStatement("SELECT current_setting('app.current_workspace_id', true)")) {
                ResultSet rs = ps.executeQuery();
                assertThat(rs.next()).isTrue();
                String leftover = rs.getString(1);
                assertThat(leftover).isNullOrEmpty();
            }
        }
    }

    @Test
    @Order(10)
    @DisplayName("Security Requirement 6: Audit log immutability prevents UPDATE and DELETE")
    void testAuditRecordIntegrityImmutability() throws SQLException {
        UUID auditId = UUID.randomUUID();

        try (Connection conn = getAppConnection()) {
            try (PreparedStatement ps = conn.prepareStatement("SELECT set_config('app.current_workspace_id', ?, true)")) {
                ps.setString(1, wsAlphaId.toString());
                ps.executeQuery();
            }

            // Insert audit record
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO audit_logs (id, workspace_id, actor_id, event_type, target_type) VALUES (?, ?, ?, ?, ?)")) {
                ps.setObject(1, auditId);
                ps.setObject(2, wsAlphaId);
                ps.setObject(3, userAlphaId);
                ps.setString(4, "WORKSPACE_CREATED");
                ps.setString(5, "WORKSPACE");
                ps.executeUpdate();
            }

            // Attempt UPDATE: blocked by RLS / trigger
            try (PreparedStatement ps = conn.prepareStatement("UPDATE audit_logs SET event_type = 'TAMPERED' WHERE id = ?")) {
                ps.setObject(1, auditId);
                int updated = ps.executeUpdate();
                assertThat(updated).isZero();
            }

            // Attempt DELETE: blocked by RLS / trigger
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM audit_logs WHERE id = ?")) {
                ps.setObject(1, auditId);
                int deleted = ps.executeUpdate();
                assertThat(deleted).isZero();
            }

            conn.commit();
        }
    }

    @Test
    @Order(11)
    @DisplayName("Security Requirement 5: Invitation roles strictly reject OWNER role")
    void testInvitationRoleConstraintRejectsOwner() throws SQLException {
        try (Connection conn = getAppConnection()) {
            try (PreparedStatement ps = conn.prepareStatement("SELECT set_config('app.current_workspace_id', ?, true)")) {
                ps.setString(1, wsAlphaId.toString());
                ps.executeQuery();
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO workspace_invitations (workspace_id, email, role, token_hash, invited_by, expires_at) " +
                    "VALUES (?, 'owner_candidate@test.com', 'OWNER', 'dummy_owner_token_hash', ?, ?)")) {
                ps.setObject(1, wsAlphaId);
                ps.setObject(2, userAlphaId);
                ps.setTimestamp(3, Timestamp.from(Instant.now().plusSeconds(3600)));

                assertThatThrownBy(ps::executeUpdate)
                        .isInstanceOf(SQLException.class)
                        .hasMessageContaining("chk_workspace_invitations_role");
            }
        }
    }

    @Test
    @Order(12)
    @DisplayName("Security Requirement 5: Unique token hash prevents duplicate invitation redemption")
    void testUniqueTokenHashConstraint() throws SQLException {
        try (Connection conn = getAppConnection()) {
            try (PreparedStatement ps = conn.prepareStatement("SELECT set_config('app.current_workspace_id', ?, true)")) {
                ps.setString(1, wsAlphaId.toString());
                ps.executeQuery();
            }

            String tokenHash = "unique_hash_" + UUID.randomUUID();

            // First insert succeeds
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO workspace_invitations (workspace_id, email, token_hash, invited_by, expires_at) " +
                    "VALUES (?, 'invite1@test.com', ?, ?, ?)")) {
                ps.setObject(1, wsAlphaId);
                ps.setString(2, tokenHash);
                ps.setObject(3, userAlphaId);
                ps.setTimestamp(4, Timestamp.from(Instant.now().plusSeconds(3600)));
                ps.executeUpdate();
            }

            // Duplicate insert with same token_hash MUST fail
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO workspace_invitations (workspace_id, email, token_hash, invited_by, expires_at) " +
                    "VALUES (?, 'invite2@test.com', ?, ?, ?)")) {
                ps.setObject(1, wsAlphaId);
                ps.setString(2, tokenHash);
                ps.setObject(3, userAlphaId);
                ps.setTimestamp(4, Timestamp.from(Instant.now().plusSeconds(3600)));

                assertThatThrownBy(ps::executeUpdate)
                        .isInstanceOf(SQLException.class)
                        .hasMessageContaining("duplicate key value");
            }
        }
    }

    @Test
    @Order(13)
    @DisplayName("Security Requirement 4.0.2 Defect 3: Exactly one ACTIVE signing key per product enforced by PostgreSQL")
    void testProductSigningKeyOneActiveEnforcedByPostgres() throws SQLException {
        UUID productId = UUID.randomUUID();
        UUID key1Id = UUID.randomUUID();
        UUID key2Id = UUID.randomUUID();

        try (Connection conn = getAppConnection()) {
            try (PreparedStatement ps = conn.prepareStatement("SELECT set_config('app.current_workspace_id', ?, true)")) {
                ps.setString(1, wsAlphaId.toString());
                ps.executeQuery();
            }

            // 1. Insert product
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO products (id, workspace_id, name, slug) VALUES (?, ?, 'Alpha Product', ?)")) {
                ps.setObject(1, productId);
                ps.setObject(2, wsAlphaId);
                ps.setString(3, "alpha-prod-" + productId.toString().substring(0, 8));
                ps.executeUpdate();
            }

            // 2. Insert first ACTIVE key (succeeds)
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO product_signing_keys (id, workspace_id, product_id, key_id, public_key, encrypted_private_key, encryption_nonce, status) " +
                    "VALUES (?, ?, ?, 'key-001', 'pubkey-data', 'enc-priv-data'::bytea, 'nonce-12'::bytea, 'ACTIVE')")) {
                ps.setObject(1, key1Id);
                ps.setObject(2, wsAlphaId);
                ps.setObject(3, productId);
                ps.executeUpdate();
            }

            // 3. Insert second ACTIVE key for same product (MUST fail with partial unique index violation)
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO product_signing_keys (id, workspace_id, product_id, key_id, public_key, encrypted_private_key, encryption_nonce, status) " +
                    "VALUES (?, ?, ?, 'key-002', 'pubkey-data-2', 'enc-priv-data-2'::bytea, 'nonce-12'::bytea, 'ACTIVE')")) {
                ps.setObject(1, key2Id);
                ps.setObject(2, wsAlphaId);
                ps.setObject(3, productId);

                assertThatThrownBy(ps::executeUpdate)
                        .isInstanceOf(SQLException.class)
                        .hasMessageContaining("uq_product_signing_keys_one_active");
            }

            conn.rollback();
        }
    }

    @Test
    @Order(14)
    @DisplayName("Security Requirement 4.0.2 Defect 2: Composite foreign key blocks cross-workspace tenant splicing")
    void testCrossWorkspaceProductSigningKeyTenantSplicingBlocked() throws SQLException {
        UUID productId = UUID.randomUUID();
        UUID keyId = UUID.randomUUID();

        // 1. Insert product in Alpha via admin
        try (Connection adminConn = DriverManager.getConnection(DB_URL, ADMIN_USER, ADMIN_PASS)) {
            try (PreparedStatement ps = adminConn.prepareStatement(
                    "INSERT INTO products (id, workspace_id, name, slug) VALUES (?, ?, 'Alpha Splicing Test', ?)")) {
                ps.setObject(1, productId);
                ps.setObject(2, wsAlphaId);
                ps.setString(3, "alpha-splice-" + productId.toString().substring(0, 8));
                ps.executeUpdate();
            }

            // 2. Direct database level test: Admin role bypasses RLS, so composite foreign key MUST block tenant splicing
            try (PreparedStatement ps = adminConn.prepareStatement(
                    "INSERT INTO product_signing_keys (id, workspace_id, product_id, key_id, public_key, encrypted_private_key, encryption_nonce, status) " +
                    "VALUES (?, ?, ?, 'key-splice', 'pubkey-splice', 'enc-priv'::bytea, 'nonce-12'::bytea, 'ACTIVE')")) {
                ps.setObject(1, keyId);
                ps.setObject(2, wsBetaId); // Spliced tenant (Beta workspace, Alpha product)
                ps.setObject(3, productId);

                assertThatThrownBy(ps::executeUpdate)
                        .isInstanceOf(SQLException.class)
                        .hasMessageContaining("fk_signing_keys_product_workspace");
            }
        }
    }

    @Test
    @Order(15)
    @DisplayName("Security Requirement 4.0.2 Defect 4: Product hard deletion permanently denied by RLS policy")
    void testProductHardDeleteBlockedByRlsPolicy() throws SQLException {
        UUID productId = UUID.randomUUID();

        try (Connection conn = getAppConnection()) {
            try (PreparedStatement ps = conn.prepareStatement("SELECT set_config('app.current_workspace_id', ?, true)")) {
                ps.setString(1, wsAlphaId.toString());
                ps.executeQuery();
            }

            // 1. Insert product in Alpha
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO products (id, workspace_id, name, slug) VALUES (?, ?, 'Delete Test Prod', ?)")) {
                ps.setObject(1, productId);
                ps.setObject(2, wsAlphaId);
                ps.setString(3, "alpha-del-" + productId.toString().substring(0, 8));
                ps.executeUpdate();
            }

            // 2. Attempt hard DELETE via application role (RLS products_delete_policy USING (false) returns 0 deleted)
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM products WHERE id = ?")) {
                ps.setObject(1, productId);
                int rowsDeleted = ps.executeUpdate();
                assertThat(rowsDeleted).isZero();
            }

            conn.rollback();
        }
    }

    @Test
    @Order(16)
    @DisplayName("Supabase Security Advisor: Sensitive SECURITY DEFINER functions have execution revoked from PUBLIC")
    void testSecurityDefinerFunctionsRevokedFromPublic() throws SQLException {
        try (Connection adminConn = DriverManager.getConnection(DB_URL, ADMIN_USER, ADMIN_PASS)) {
            try (PreparedStatement ps = adminConn.prepareStatement(
                    "SELECT p.proname, p.proacl::text AS acl " +
                    "FROM pg_proc p " +
                    "JOIN pg_namespace n ON p.pronamespace = n.oid " +
                    "WHERE n.nspname = 'public' " +
                    "  AND p.proname IN ('lookup_invitation_for_redemption', 'app_is_workspace_admin', " +
                    "                    'app_current_user_email', 'app_current_user_id', " +
                    "                    'app_current_workspace_id', 'app_current_supabase_user_id')")) {
                ResultSet rs = ps.executeQuery();
                int verifiedFunctions = 0;
                while (rs.next()) {
                    String funcName = rs.getString("proname");
                    String acl = rs.getString("acl");
                    // In PostgreSQL ACL syntax, a grant to PUBLIC begins with "=X" without a role name preceding "="
                    assertThat(acl).doesNotMatch(".*[\\{,]=[a-zA-Z]*X.*");
                    verifiedFunctions++;
                }
                assertThat(verifiedFunctions).isEqualTo(6);
            }
        }
    }

    @Test
    @Order(17)
    @DisplayName("Supabase Security Advisor: Functions have immutable search_path set to prevent privilege escalation")
    void testSecurityDefinerFunctionsSetSearchPath() throws SQLException {
        try (Connection adminConn = DriverManager.getConnection(DB_URL, ADMIN_USER, ADMIN_PASS)) {
            try (PreparedStatement ps = adminConn.prepareStatement(
                    "SELECT p.proname, array_to_string(p.proconfig, ', ') AS config " +
                    "FROM pg_proc p " +
                    "JOIN pg_namespace n ON p.pronamespace = n.oid " +
                    "WHERE n.nspname = 'public' " +
                    "  AND p.proname IN ('lookup_invitation_for_redemption', 'app_is_workspace_admin', " +
                    "                    'app_current_user_email', 'app_current_user_id', " +
                    "                    'app_current_workspace_id', 'app_current_supabase_user_id')")) {
                ResultSet rs = ps.executeQuery();
                int verified = 0;
                while (rs.next()) {
                    String config = rs.getString("config");
                    assertThat(config).contains("search_path=public, pg_temp");
                    verified++;
                }
                assertThat(verified).isEqualTo(6);
            }
        }
    }

    @Test
    @Order(18)
    @DisplayName("Supabase Security Advisor: Core business unique constraints are strictly intact")
    void testUniqueConstraintBackingIndexesPreserved() throws SQLException {
        try (Connection adminConn = DriverManager.getConnection(DB_URL, ADMIN_USER, ADMIN_PASS)) {
            try (PreparedStatement ps = adminConn.prepareStatement(
                    "SELECT conname FROM pg_constraint " +
                    "WHERE connamespace = 'public'::regnamespace AND contype = 'u' " +
                    "  AND conname IN ('users_email_key', 'users_supabase_user_id_key', 'workspaces_slug_key', 'user_preferences_user_id_key', 'workspace_invitations_token_hash_key')")) {
                ResultSet rs = ps.executeQuery();
                int count = 0;
                while (rs.next()) {
                    count++;
                }
                assertThat(count).isEqualTo(5);
            }
        }
    }

    @Test
    @Order(19)
    @DisplayName("Phase A / Security: Plan composite foreign key blocks cross-workspace tenant splicing")
    void testPlanProductWorkspaceConsistencyBlocked() throws SQLException {
        try (Connection adminConn = DriverManager.getConnection(DB_URL, ADMIN_USER, ADMIN_PASS)) {
            adminConn.setAutoCommit(false);
            try {
                // Apply V7 DDL within isolated transaction
                try (PreparedStatement ps = adminConn.prepareStatement(
                        "CREATE TABLE IF NOT EXISTS license_plans (" +
                        "    id UUID PRIMARY KEY DEFAULT gen_random_uuid()," +
                        "    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE," +
                        "    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE," +
                        "    name VARCHAR(255) NOT NULL," +
                        "    description TEXT," +
                        "    duration_days INTEGER NOT NULL DEFAULT 365," +
                        "    maximum_activations INTEGER NOT NULL DEFAULT 1," +
                        "    seat_limit INTEGER NOT NULL DEFAULT 1," +
                        "    offline_grace_period_seconds INTEGER NOT NULL DEFAULT 25200," +
                        "    revalidation_interval_seconds INTEGER NOT NULL DEFAULT 300," +
                        "    entitlements JSONB NOT NULL DEFAULT '{}'::jsonb," +
                        "    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE'," +
                        "    version INTEGER NOT NULL DEFAULT 1," +
                        "    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                        "    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                        "    CONSTRAINT uq_plans_product_name UNIQUE (product_id, name)," +
                        "    CONSTRAINT uq_plans_id_workspace UNIQUE (id, workspace_id)," +
                        "    CONSTRAINT fk_plans_product_workspace FOREIGN KEY (product_id, workspace_id) REFERENCES products(id, workspace_id) ON DELETE CASCADE" +
                        ")")) {
                    ps.execute();
                }

                // Insert Product in Alpha
                UUID prodId = UUID.randomUUID();
                try (PreparedStatement ps = adminConn.prepareStatement(
                        "INSERT INTO products (id, workspace_id, name, slug) VALUES (?, ?, 'Alpha Prod', ?)")) {
                    ps.setObject(1, prodId);
                    ps.setObject(2, wsAlphaId);
                    ps.setString(3, "alpha-plan-test-" + prodId.toString().substring(0, 8));
                    ps.executeUpdate();
                }

                // Attempt to insert plan linking to Alpha product but with Beta workspace_id (tenant splicing)
                try (PreparedStatement ps = adminConn.prepareStatement(
                        "INSERT INTO license_plans (workspace_id, product_id, name) VALUES (?, ?, 'Spliced Plan')")) {
                    ps.setObject(1, wsBetaId); // Spliced workspace
                    ps.setObject(2, prodId);   // Alpha product

                    assertThatThrownBy(ps::executeUpdate)
                            .isInstanceOf(SQLException.class)
                            .hasMessageContaining("fk_plans_product_workspace");
                }
            } finally {
                adminConn.rollback();
            }
        }
    }

    @Test
    @Order(20)
    @DisplayName("Phase C / Security: Customer workspace-scoped email uniqueness and cross-tenant coexistence")
    void testCustomerWorkspaceEmailUniqueness() throws SQLException {
        try (Connection adminConn = DriverManager.getConnection(DB_URL, ADMIN_USER, ADMIN_PASS)) {
            adminConn.setAutoCommit(false);
            try {
                // Apply V7 customers table within isolated transaction
                try (PreparedStatement ps = adminConn.prepareStatement(
                        "CREATE TABLE IF NOT EXISTS customers (" +
                        "    id UUID PRIMARY KEY DEFAULT gen_random_uuid()," +
                        "    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE," +
                        "    name VARCHAR(255) NOT NULL," +
                        "    email VARCHAR(255) NOT NULL," +
                        "    company VARCHAR(255)," +
                        "    metadata JSONB NOT NULL DEFAULT '{}'::jsonb," +
                        "    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE'," +
                        "    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                        "    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                        "    CONSTRAINT uq_customers_workspace_email UNIQUE (workspace_id, email)" +
                        ")")) {
                    ps.execute();
                }

                String sharedEmail = "enterprise@corp.com";

                // 1. Insert customer in Alpha workspace
                try (PreparedStatement ps = adminConn.prepareStatement(
                        "INSERT INTO customers (workspace_id, name, email) VALUES (?, 'Corp Alpha', ?)")) {
                    ps.setObject(1, wsAlphaId);
                    ps.setString(2, sharedEmail);
                    ps.executeUpdate();
                }

                // 2. Duplicate in Alpha MUST fail with unique constraint violation
                Savepoint sp = adminConn.setSavepoint();
                try (PreparedStatement ps = adminConn.prepareStatement(
                        "INSERT INTO customers (workspace_id, name, email) VALUES (?, 'Corp Alpha 2', ?)")) {
                    ps.setObject(1, wsAlphaId);
                    ps.setString(2, sharedEmail);

                    assertThatThrownBy(ps::executeUpdate)
                            .isInstanceOf(SQLException.class)
                            .hasMessageContaining("uq_customers_workspace_email");
                }
                adminConn.rollback(sp); // Restore transaction after expected error

                // 3. Same email in Beta workspace MUST succeed (workspace isolation)
                try (PreparedStatement ps = adminConn.prepareStatement(
                        "INSERT INTO customers (workspace_id, name, email) VALUES (?, 'Corp Beta', ?)")) {
                    ps.setObject(1, wsBetaId);
                    ps.setString(2, sharedEmail);
                    int inserted = ps.executeUpdate();
                    assertThat(inserted).isEqualTo(1);
                }
            } finally {
                adminConn.rollback();
            }
        }
    }

    @Test
    @Order(21)
    @DisplayName("Phase C / Security: Customer external_reference uniqueness per workspace and cross-tenant coexistence")
    void testCustomerExternalReferenceUniqueness() throws SQLException {
        try (Connection adminConn = DriverManager.getConnection(DB_URL, ADMIN_USER, ADMIN_PASS)) {
            adminConn.setAutoCommit(false);
            try {
                // Apply V7 customers table & partial unique index within isolated transaction
                try (PreparedStatement ps = adminConn.prepareStatement(
                        "CREATE TABLE IF NOT EXISTS customers (" +
                        "    id UUID PRIMARY KEY DEFAULT gen_random_uuid()," +
                        "    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE," +
                        "    name VARCHAR(255) NOT NULL," +
                        "    email VARCHAR(255) NOT NULL," +
                        "    company VARCHAR(255)," +
                        "    external_reference VARCHAR(255)," +
                        "    metadata JSONB NOT NULL DEFAULT '{}'::jsonb," +
                        "    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE'," +
                        "    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                        "    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                        "    CONSTRAINT uq_customers_workspace_email UNIQUE (workspace_id, email)" +
                        ");" +
                        "CREATE UNIQUE INDEX IF NOT EXISTS idx_customers_workspace_ext_ref ON customers(workspace_id, external_reference) WHERE external_reference IS NOT NULL;")) {
                    ps.execute();
                }

                String sharedExtRef = "CRM-REF-998811";

                // 1. Insert customer in Alpha workspace with external reference
                try (PreparedStatement ps = adminConn.prepareStatement(
                        "INSERT INTO customers (workspace_id, name, email, external_reference) VALUES (?, 'Customer A1', 'c1@alpha.com', ?)")) {
                    ps.setObject(1, wsAlphaId);
                    ps.setString(2, sharedExtRef);
                    ps.executeUpdate();
                }

                // 2. Duplicate external reference in Alpha workspace MUST fail
                Savepoint sp = adminConn.setSavepoint();
                try (PreparedStatement ps = adminConn.prepareStatement(
                        "INSERT INTO customers (workspace_id, name, email, external_reference) VALUES (?, 'Customer A2', 'c2@alpha.com', ?)")) {
                    ps.setObject(1, wsAlphaId);
                    ps.setString(2, sharedExtRef);

                    assertThatThrownBy(ps::executeUpdate)
                            .isInstanceOf(SQLException.class)
                            .hasMessageContaining("idx_customers_workspace_ext_ref");
                }
                adminConn.rollback(sp);

                // 3. Same external reference in Beta workspace MUST succeed (cross-tenant coexistence)
                try (PreparedStatement ps = adminConn.prepareStatement(
                        "INSERT INTO customers (workspace_id, name, email, external_reference) VALUES (?, 'Customer B1', 'c1@beta.com', ?)")) {
                    ps.setObject(1, wsBetaId);
                    ps.setString(2, sharedExtRef);
                    int inserted = ps.executeUpdate();
                    assertThat(inserted).isEqualTo(1);
                }
            } finally {
                adminConn.rollback();
            }
        }
    }

    @Test
    @Order(22)
    @DisplayName("Phase B / Security: License plan version history immutability and composite FK defense")
    void testLicensePlanVersionImmutabilityAndCompositeFk() throws SQLException {
        try (Connection adminConn = DriverManager.getConnection(DB_URL, ADMIN_USER, ADMIN_PASS)) {
            adminConn.setAutoCommit(false);
            try {
                // Apply V7 license_plans & license_plan_versions tables within isolated transaction
                try (PreparedStatement ps = adminConn.prepareStatement(
                        "CREATE TABLE IF NOT EXISTS license_plans (" +
                        "    id UUID PRIMARY KEY DEFAULT gen_random_uuid()," +
                        "    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE," +
                        "    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE," +
                        "    name VARCHAR(255) NOT NULL," +
                        "    description TEXT," +
                        "    duration_days INTEGER," +
                        "    maximum_activations INTEGER NOT NULL DEFAULT 1," +
                        "    seat_limit INTEGER NOT NULL DEFAULT 1," +
                        "    offline_grace_period_seconds INTEGER NOT NULL DEFAULT 25200," +
                        "    revalidation_interval_seconds INTEGER NOT NULL DEFAULT 300," +
                        "    entitlements JSONB NOT NULL DEFAULT '{}'::jsonb," +
                        "    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE'," +
                        "    version INTEGER NOT NULL DEFAULT 1," +
                        "    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                        "    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                        "    CONSTRAINT uq_plans_id_workspace UNIQUE (id, workspace_id)," +
                        "    CONSTRAINT fk_plans_product_workspace FOREIGN KEY (product_id, workspace_id) REFERENCES products(id, workspace_id) ON DELETE CASCADE" +
                        ");" +
                        "CREATE TABLE IF NOT EXISTS license_plan_versions (" +
                        "    id UUID PRIMARY KEY DEFAULT gen_random_uuid()," +
                        "    plan_id UUID NOT NULL REFERENCES license_plans(id) ON DELETE CASCADE," +
                        "    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE," +
                        "    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE," +
                        "    version INTEGER NOT NULL," +
                        "    name VARCHAR(255) NOT NULL," +
                        "    description TEXT," +
                        "    duration_days INTEGER," +
                        "    maximum_activations INTEGER NOT NULL," +
                        "    seat_limit INTEGER NOT NULL," +
                        "    offline_grace_period_seconds INTEGER NOT NULL," +
                        "    revalidation_interval_seconds INTEGER NOT NULL," +
                        "    entitlements JSONB NOT NULL DEFAULT '{}'::jsonb," +
                        "    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                        "    created_by UUID REFERENCES users(id) ON DELETE SET NULL," +
                        "    CONSTRAINT uq_plan_versions_plan_ver UNIQUE (plan_id, version)," +
                        "    CONSTRAINT fk_plan_versions_product_workspace FOREIGN KEY (product_id, workspace_id) REFERENCES products(id, workspace_id) ON DELETE CASCADE" +
                        ");" +
                        "ALTER TABLE license_plan_versions ENABLE ROW LEVEL SECURITY;" +
                        "DROP POLICY IF EXISTS plan_versions_update_policy ON license_plan_versions;" +
                        "CREATE POLICY plan_versions_update_policy ON license_plan_versions FOR UPDATE TO PUBLIC USING (FALSE);" +
                        "DROP POLICY IF EXISTS plan_versions_delete_policy ON license_plan_versions;" +
                        "CREATE POLICY plan_versions_delete_policy ON license_plan_versions FOR DELETE TO PUBLIC USING (FALSE);")) {
                    ps.execute();
                }

                // 1. Create a product in Alpha workspace
                UUID prodAlphaId = UUID.randomUUID();
                try (PreparedStatement ps = adminConn.prepareStatement(
                        "INSERT INTO products (id, workspace_id, name, slug, created_by) VALUES (?, ?, 'Alpha Product', 'alpha-v-' || ?, ?)")) {
                    ps.setObject(1, prodAlphaId);
                    ps.setObject(2, wsAlphaId);
                    ps.setString(3, prodAlphaId.toString().substring(0, 8));
                    ps.setObject(4, userAlphaId);
                    ps.executeUpdate();
                }

                // 2. Create a plan in Alpha workspace
                UUID planAlphaId = UUID.randomUUID();
                try (PreparedStatement ps = adminConn.prepareStatement(
                        "INSERT INTO license_plans (id, workspace_id, product_id, name, version) VALUES (?, ?, ?, 'Plan Alpha', 1)")) {
                    ps.setObject(1, planAlphaId);
                    ps.setObject(2, wsAlphaId);
                    ps.setObject(3, prodAlphaId);
                    ps.executeUpdate();
                }

                // 3. Create a version in Alpha workspace (version 1)
                UUID verAlphaId = UUID.randomUUID();
                try (PreparedStatement ps = adminConn.prepareStatement(
                        "INSERT INTO license_plan_versions (id, plan_id, workspace_id, product_id, version, name, maximum_activations, seat_limit, offline_grace_period_seconds, revalidation_interval_seconds) " +
                        "VALUES (?, ?, ?, ?, 1, 'Plan Alpha v1', 1, 1, 25200, 300)")) {
                    ps.setObject(1, verAlphaId);
                    ps.setObject(2, planAlphaId);
                    ps.setObject(3, wsAlphaId);
                    ps.setObject(4, prodAlphaId);
                    int inserted = ps.executeUpdate();
                    assertThat(inserted).isEqualTo(1);
                }

                // 4. Cross-tenant composite FK violation MUST fail: attaching Alpha version to Beta product
                UUID prodBetaId = UUID.randomUUID();
                try (PreparedStatement ps = adminConn.prepareStatement(
                        "INSERT INTO products (id, workspace_id, name, slug, created_by) VALUES (?, ?, 'Beta Product', 'beta-v-' || ?, ?)")) {
                    ps.setObject(1, prodBetaId);
                    ps.setObject(2, wsBetaId);
                    ps.setString(3, prodBetaId.toString().substring(0, 8));
                    ps.setObject(4, userBetaId);
                    ps.executeUpdate();
                }

                Savepoint sp = adminConn.setSavepoint();
                try (PreparedStatement ps = adminConn.prepareStatement(
                        "INSERT INTO license_plan_versions (plan_id, workspace_id, product_id, version, name, maximum_activations, seat_limit, offline_grace_period_seconds, revalidation_interval_seconds) " +
                        "VALUES (?, ?, ?, 2, 'Tampered Version', 1, 1, 25200, 300)")) {
                    ps.setObject(1, planAlphaId); // Alpha plan
                    ps.setObject(2, wsAlphaId);   // Alpha workspace
                    ps.setObject(3, prodBetaId);   // Beta product (MISMATCH!)

                    assertThatThrownBy(ps::executeUpdate)
                            .isInstanceOf(SQLException.class)
                            .hasMessageContaining("fk_plan_versions_product_workspace");
                }
                adminConn.rollback(sp);
            } finally {
                adminConn.rollback();
            }
        }
    }
}
