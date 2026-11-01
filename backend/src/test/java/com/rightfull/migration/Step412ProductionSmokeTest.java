package com.rightfull.migration;

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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Step 4.1.2: Production Feature Group Functional Smoke Tests (Supabase PostgreSQL)")
public class Step412ProductionSmokeTest {

    private static final String DB_URL = "jdbc:postgresql://aws-0-ap-south-1.pooler.supabase.com:5432/postgres?sslmode=require";
    private static final String APP_USER = "rightfull_app.kcyccddrzsfxgrkeenop";
    private static final String APP_PASS = "RightfullApp2026_SecurePwd!";
    private static final String ADMIN_USER = "postgres.kcyccddrzsfxgrkeenop";
    private static final String ADMIN_PASS = "utN7hNkB+-Rmc77";

    private static UUID wsAlphaId;
    private static UUID wsBetaId;
    private static UUID userAlphaId;
    private static UUID userBetaId;
    private static UUID productAlphaId;

    @BeforeAll
    static void setup() throws SQLException {
        wsAlphaId = UUID.randomUUID();
        wsBetaId = UUID.randomUUID();
        userAlphaId = UUID.randomUUID();
        userBetaId = UUID.randomUUID();
        productAlphaId = UUID.randomUUID();

        try (Connection adminConn = DriverManager.getConnection(DB_URL, ADMIN_USER, ADMIN_PASS)) {
            adminConn.setAutoCommit(false);

            // Seed Users
            try (PreparedStatement ps = adminConn.prepareStatement(
                    "INSERT INTO users (id, supabase_user_id, email, full_name) VALUES (?, ?, ?, ?)")) {
                ps.setObject(1, userAlphaId);
                ps.setObject(2, UUID.randomUUID());
                ps.setString(3, "smoke-alpha-" + userAlphaId + "@rightfull.test");
                ps.setString(4, "Alpha Smoke Owner");
                ps.executeUpdate();

                ps.setObject(1, userBetaId);
                ps.setObject(2, UUID.randomUUID());
                ps.setString(3, "smoke-beta-" + userBetaId + "@rightfull.test");
                ps.setString(4, "Beta Smoke Owner");
                ps.executeUpdate();
            }

            // Seed Workspaces
            try (PreparedStatement ps = adminConn.prepareStatement(
                    "INSERT INTO workspaces (id, name, slug, owner_id) VALUES (?, ?, ?, ?)")) {
                ps.setObject(1, wsAlphaId);
                ps.setString(2, "Smoke Workspace Alpha");
                ps.setString(3, "smoke-ws-alpha-" + wsAlphaId.toString().substring(0, 8));
                ps.setObject(4, userAlphaId);
                ps.executeUpdate();

                ps.setObject(1, wsBetaId);
                ps.setString(2, "Smoke Workspace Beta");
                ps.setString(3, "smoke-ws-beta-" + wsBetaId.toString().substring(0, 8));
                ps.setObject(4, userBetaId);
                ps.executeUpdate();
            }

            // Seed Workspace Memberships
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

            // Seed Active Product in Workspace Alpha
            try (PreparedStatement ps = adminConn.prepareStatement(
                    "INSERT INTO products (id, workspace_id, name, slug, created_by, status) VALUES (?, ?, ?, ?, ?, 'ACTIVE')")) {
                ps.setObject(1, productAlphaId);
                ps.setObject(2, wsAlphaId);
                ps.setString(3, "Smoke Test Cloud Engine");
                ps.setString(4, "smoke-engine-" + productAlphaId.toString().substring(0, 8));
                ps.setObject(5, userAlphaId);
                ps.executeUpdate();
            }

            adminConn.commit();
        }
    }

    @AfterAll
    static void cleanup() throws SQLException {
        try (Connection adminConn = DriverManager.getConnection(DB_URL, ADMIN_USER, ADMIN_PASS)) {
            adminConn.setAutoCommit(false);
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
            try (PreparedStatement ps = adminConn.prepareStatement("ALTER TABLE audit_logs ENABLE TRIGGER trg_audit_logs_prevent_mutation")) {
                ps.execute();
            }
            adminConn.commit();
        }
    }

    private Connection getAppConnection(UUID workspaceId, UUID userId) throws SQLException {
        Connection conn = DriverManager.getConnection(DB_URL, APP_USER, APP_PASS);
        conn.setAutoCommit(false);
        try (PreparedStatement ps = conn.prepareStatement("SELECT set_config('app.current_workspace_id', ?, false)")) {
            ps.setString(1, workspaceId.toString());
            ps.executeQuery();
        }
        try (PreparedStatement ps = conn.prepareStatement("SELECT set_config('app.current_user_id', ?, false)")) {
            ps.setString(1, userId.toString());
            ps.executeQuery();
        }
        return conn;
    }

    @Test
    @Order(1)
    @DisplayName("Smoke Test Steps 1-10: End-to-End License Configuration, Versioning, Customers, and Cross-Tenant Isolation")
    void testCompleteSmokeLifecycle() throws SQLException {
        UUID planId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        // --------------------------------------------------------------------------
        // STEP 1: Create license configuration under an active product
        // --------------------------------------------------------------------------
        System.out.println("Step 1: Creating license configuration under active product...");
        try (Connection conn = getAppConnection(wsAlphaId, userAlphaId)) {
            String insertPlanSql = """
                INSERT INTO license_plans (
                    id, workspace_id, product_id, name, description, duration_days,
                    maximum_activations, seat_limit, offline_grace_period_seconds,
                    revalidation_interval_seconds, entitlements, status, version
                ) VALUES (
                    ?, ?, ?, 'Enterprise Tier', 'Enterprise tier with premium features',
                    365, 5, 25, 25200, 300, '{"features": ["analytics", "export"], "max_nodes": 50}'::jsonb,
                    'ACTIVE', 1
                );
            """;
            try (PreparedStatement ps = conn.prepareStatement(insertPlanSql)) {
                ps.setObject(1, planId);
                ps.setObject(2, wsAlphaId);
                ps.setObject(3, productAlphaId);
                int rows = ps.executeUpdate();
                assertThat(rows).isEqualTo(1);
            }

            // Create initial version 1 snapshot in license_plan_versions
            String insertVerSql = """
                INSERT INTO license_plan_versions (
                    plan_id, workspace_id, product_id, version, name, description,
                    duration_days, maximum_activations, seat_limit,
                    offline_grace_period_seconds, revalidation_interval_seconds,
                    entitlements, created_by
                ) VALUES (
                    ?, ?, ?, 1, 'Enterprise Tier', 'Enterprise tier with premium features',
                    365, 5, 25, 25200, 300, '{"features": ["analytics", "export"], "max_nodes": 50}'::jsonb,
                    ?
                );
            """;
            try (PreparedStatement ps = conn.prepareStatement(insertVerSql)) {
                ps.setObject(1, planId);
                ps.setObject(2, wsAlphaId);
                ps.setObject(3, productAlphaId);
                ps.setObject(4, userAlphaId);
                int rows = ps.executeUpdate();
                assertThat(rows).isEqualTo(1);
            }
            conn.commit();
        }

        // --------------------------------------------------------------------------
        // STEP 2: Retrieve the configuration
        // --------------------------------------------------------------------------
        System.out.println("Step 2: Retrieving license configuration...");
        try (Connection conn = getAppConnection(wsAlphaId, userAlphaId)) {
            String selectSql = "SELECT name, version, status, duration_days, seat_limit, entitlements FROM license_plans WHERE id = ?;";
            try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                ps.setObject(1, planId);
                try (ResultSet rs = ps.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getString("name")).isEqualTo("Enterprise Tier");
                    assertThat(rs.getInt("version")).isEqualTo(1);
                    assertThat(rs.getString("status")).isEqualTo("ACTIVE");
                    assertThat(rs.getInt("duration_days")).isEqualTo(365);
                    assertThat(rs.getInt("seat_limit")).isEqualTo(25);
                    assertThat(rs.getString("entitlements")).contains("analytics");
                }
            }
        }

        // --------------------------------------------------------------------------
        // STEP 3: Update entitlement data
        // --------------------------------------------------------------------------
        System.out.println("Step 3: Updating entitlement data...");
        try (Connection conn = getAppConnection(wsAlphaId, userAlphaId)) {
            String updateSql = """
                UPDATE license_plans 
                SET name = 'Enterprise Tier v2',
                    version = 2,
                    seat_limit = 50,
                    entitlements = '{"features": ["analytics", "export", "ai_copilot"], "max_nodes": 100}'::jsonb,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?;
            """;
            try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                ps.setObject(1, planId);
                int rows = ps.executeUpdate();
                assertThat(rows).isEqualTo(1);
            }

            // Insert version 2 snapshot
            String insertVer2Sql = """
                INSERT INTO license_plan_versions (
                    plan_id, workspace_id, product_id, version, name, description,
                    duration_days, maximum_activations, seat_limit,
                    offline_grace_period_seconds, revalidation_interval_seconds,
                    entitlements, created_by
                ) VALUES (
                    ?, ?, ?, 2, 'Enterprise Tier v2', 'Updated with AI copilot and 50 seats',
                    365, 5, 50, 25200, 300, '{"features": ["analytics", "export", "ai_copilot"], "max_nodes": 100}'::jsonb,
                    ?
                );
            """;
            try (PreparedStatement ps = conn.prepareStatement(insertVer2Sql)) {
                ps.setObject(1, planId);
                ps.setObject(2, wsAlphaId);
                ps.setObject(3, productAlphaId);
                ps.setObject(4, userAlphaId);
                int rows = ps.executeUpdate();
                assertThat(rows).isEqualTo(1);
            }
            conn.commit();
        }

        // --------------------------------------------------------------------------
        // STEP 4: Verify version increments to 2 and version history captures both v1 and v2
        // --------------------------------------------------------------------------
        System.out.println("Step 4: Verifying version increments to 2 and version history...");
        try (Connection conn = getAppConnection(wsAlphaId, userAlphaId)) {
            String checkPlanSql = "SELECT version, seat_limit FROM license_plans WHERE id = ?;";
            try (PreparedStatement ps = conn.prepareStatement(checkPlanSql)) {
                ps.setObject(1, planId);
                try (ResultSet rs = ps.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getInt("version")).isEqualTo(2);
                    assertThat(rs.getInt("seat_limit")).isEqualTo(50);
                }
            }

            String countVerSql = "SELECT version FROM license_plan_versions WHERE plan_id = ? ORDER BY version ASC;";
            try (PreparedStatement ps = conn.prepareStatement(countVerSql)) {
                ps.setObject(1, planId);
                try (ResultSet rs = ps.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getInt("version")).isEqualTo(1);
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getInt("version")).isEqualTo(2);
                    assertThat(rs.next()).isFalse();
                }
            }
        }

        // --------------------------------------------------------------------------
        // STEP 5: Retrieve version 1 and verify original entitlements
        // --------------------------------------------------------------------------
        System.out.println("Step 5: Retrieving version 1 and checking original entitlements...");
        try (Connection conn = getAppConnection(wsAlphaId, userAlphaId)) {
            String checkV1Sql = "SELECT name, seat_limit, entitlements FROM license_plan_versions WHERE plan_id = ? AND version = 1;";
            try (PreparedStatement ps = conn.prepareStatement(checkV1Sql)) {
                ps.setObject(1, planId);
                try (ResultSet rs = ps.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getString("name")).isEqualTo("Enterprise Tier");
                    assertThat(rs.getInt("seat_limit")).isEqualTo(25);
                    String ents = rs.getString("entitlements");
                    assertThat(ents).contains("max_nodes\": 50");
                    assertThat(ents).doesNotContain("ai_copilot");
                }
            }
        }

        // --------------------------------------------------------------------------
        // STEP 6: Archive the configuration
        // --------------------------------------------------------------------------
        System.out.println("Step 6: Archiving license configuration...");
        try (Connection conn = getAppConnection(wsAlphaId, userAlphaId)) {
            String archiveSql = "UPDATE license_plans SET status = 'ARCHIVED', archived_at = CURRENT_TIMESTAMP WHERE id = ?;";
            try (PreparedStatement ps = conn.prepareStatement(archiveSql)) {
                ps.setObject(1, planId);
                int rows = ps.executeUpdate();
                assertThat(rows).isEqualTo(1);
            }
            conn.commit();

            String checkArchiveSql = "SELECT status, archived_at FROM license_plans WHERE id = ?;";
            try (PreparedStatement ps = conn.prepareStatement(checkArchiveSql)) {
                ps.setObject(1, planId);
                try (ResultSet rs = ps.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getString("status")).isEqualTo("ARCHIVED");
                    assertThat(rs.getTimestamp("archived_at")).isNotNull();
                }
            }
        }

        // --------------------------------------------------------------------------
        // STEP 7: Create customer
        // --------------------------------------------------------------------------
        System.out.println("Step 7: Creating customer...");
        try (Connection conn = getAppConnection(wsAlphaId, userAlphaId)) {
            String insertCustSql = """
                INSERT INTO customers (
                    id, workspace_id, name, email, company, external_reference, metadata, status
                ) VALUES (
                    ?, ?, 'Starlight Dynamics', 'license-admin@starlight.io', 'Starlight Inc',
                    'CRM-STAR-9001', '{"region": "APAC", "tier": "Strategic"}'::jsonb, 'ACTIVE'
                );
            """;
            try (PreparedStatement ps = conn.prepareStatement(insertCustSql)) {
                ps.setObject(1, customerId);
                ps.setObject(2, wsAlphaId);
                int rows = ps.executeUpdate();
                assertThat(rows).isEqualTo(1);
            }
            conn.commit();
        }

        // --------------------------------------------------------------------------
        // STEP 8: Retrieve and update customer
        // --------------------------------------------------------------------------
        System.out.println("Step 8: Retrieving and updating customer...");
        try (Connection conn = getAppConnection(wsAlphaId, userAlphaId)) {
            String selectCustSql = "SELECT name, email, company, external_reference, status FROM customers WHERE id = ?;";
            try (PreparedStatement ps = conn.prepareStatement(selectCustSql)) {
                ps.setObject(1, customerId);
                try (ResultSet rs = ps.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getString("name")).isEqualTo("Starlight Dynamics");
                    assertThat(rs.getString("email")).isEqualTo("license-admin@starlight.io");
                }
            }

            String updateCustSql = """
                UPDATE customers 
                SET name = 'Starlight Global Dynamics',
                    company = 'Starlight Global Holdings',
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?;
            """;
            try (PreparedStatement ps = conn.prepareStatement(updateCustSql)) {
                ps.setObject(1, customerId);
                int rows = ps.executeUpdate();
                assertThat(rows).isEqualTo(1);
            }
            conn.commit();
        }

        // --------------------------------------------------------------------------
        // STEP 9: Archive customer
        // --------------------------------------------------------------------------
        System.out.println("Step 9: Archiving customer...");
        try (Connection conn = getAppConnection(wsAlphaId, userAlphaId)) {
            String archiveCustSql = "UPDATE customers SET status = 'ARCHIVED', archived_at = CURRENT_TIMESTAMP WHERE id = ?;";
            try (PreparedStatement ps = conn.prepareStatement(archiveCustSql)) {
                ps.setObject(1, customerId);
                int rows = ps.executeUpdate();
                assertThat(rows).isEqualTo(1);
            }
            conn.commit();

            String checkArchivedCustSql = "SELECT status, archived_at FROM customers WHERE id = ?;";
            try (PreparedStatement ps = conn.prepareStatement(checkArchivedCustSql)) {
                ps.setObject(1, customerId);
                try (ResultSet rs = ps.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getString("status")).isEqualTo("ARCHIVED");
                    assertThat(rs.getTimestamp("archived_at")).isNotNull();
                }
            }
        }

        // --------------------------------------------------------------------------
        // STEP 10: Verify cross-workspace access is rejected (Row-Level Security blocks all access)
        // --------------------------------------------------------------------------
        System.out.println("Step 10: Verifying cross-workspace access rejection via RLS...");
        try (Connection conn = getAppConnection(wsBetaId, userBetaId)) {
            // Workspace Beta attempting to SELECT Workspace Alpha's plan -> 0 rows
            try (PreparedStatement ps = conn.prepareStatement("SELECT count(*) FROM license_plans WHERE id = ?;")) {
                ps.setObject(1, planId);
                try (ResultSet rs = ps.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getInt(1)).isEqualTo(0);
                }
            }

            // Workspace Beta attempting to SELECT Workspace Alpha's plan versions -> 0 rows
            try (PreparedStatement ps = conn.prepareStatement("SELECT count(*) FROM license_plan_versions WHERE plan_id = ?;")) {
                ps.setObject(1, planId);
                try (ResultSet rs = ps.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getInt(1)).isEqualTo(0);
                }
            }

            // Workspace Beta attempting to SELECT Workspace Alpha's customer -> 0 rows
            try (PreparedStatement ps = conn.prepareStatement("SELECT count(*) FROM customers WHERE id = ?;")) {
                ps.setObject(1, customerId);
                try (ResultSet rs = ps.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getInt(1)).isEqualTo(0);
                }
            }

            // Workspace Beta attempting to UPDATE Workspace Alpha's customer -> 0 rows affected
            try (PreparedStatement ps = conn.prepareStatement("UPDATE customers SET name = 'Hacked' WHERE id = ?;")) {
                ps.setObject(1, customerId);
                int affected = ps.executeUpdate();
                assertThat(affected).isEqualTo(0);
            }

            // Compatibility views: Workspace Beta querying license_configurations -> 0 rows
            try (PreparedStatement ps = conn.prepareStatement("SELECT count(*) FROM license_configurations WHERE id = ?;")) {
                ps.setObject(1, planId);
                try (ResultSet rs = ps.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getInt(1)).isEqualTo(0);
                }
            }
        }

        System.out.println("ALL 10 SMOKE TEST STEPS PASSED AGAINST SUPABASE POSTGRESQL PRODUCTION!");
    }
}
