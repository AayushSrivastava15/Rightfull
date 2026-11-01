package com.rightfull.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class Step412ProductionMigrationTest {

    private static final String DB_URL = "jdbc:postgresql://aws-0-ap-south-1.pooler.supabase.com:5432/postgres?sslmode=require";
    private static final String ADMIN_USER = "postgres.kcyccddrzsfxgrkeenop";
    private static final String ADMIN_PASS = "utN7hNkB+-Rmc77";

    @Test
    @Order(1)
    @DisplayName("Validate all Flyway migrations V1 through V7 are applied and valid")
    void validateAllFlywayMigrations() {
        Flyway flyway = Flyway.configure()
                .dataSource(DB_URL, ADMIN_USER, ADMIN_PASS)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .load();

        // Validate all migrations (V1-V7)
        System.out.println("Running Flyway validation on all migrations (V1-V7)...");
        flyway.validate();
        System.out.println("Flyway validation SUCCESSFUL!");

        MigrationInfoService postInfo = flyway.info();
        System.out.println("\n=== POST-MIGRATION FLYWAY INFO ===");
        int totalApplied = 0;
        for (MigrationInfo info : postInfo.all()) {
            System.out.printf("Version: %s | Description: %s | State: %s | Checksum: %s | Installed On: %s%n",
                    info.getVersion(), info.getDescription(), info.getState(), info.getChecksum(), info.getInstalledOn());
            assertThat(info.getState().isApplied()).isTrue();
            totalApplied++;
        }
        assertThat(totalApplied).isEqualTo(7);
        assertThat(postInfo.current().getVersion().getVersion()).isEqualTo("7");
    }

    @Test
    @Order(2)
    @DisplayName("Verify Schema Objects, RLS, Policies, Constraints, and Grants")
    void verifyPostMigrationDatabaseState() throws Exception {
        try (Connection conn = DriverManager.getConnection(DB_URL, ADMIN_USER, ADMIN_PASS)) {

            // 1. Verify Tables Exist and RLS is enabled
            System.out.println("\n--- Checking Table Existence and RLS ---");
            String checkRlsSql = """
                SELECT c.relname, c.relrowsecurity, c.relforcerowsecurity
                FROM pg_class c
                JOIN pg_namespace n ON n.oid = c.relnamespace
                WHERE n.nspname = 'public'
                  AND c.relname IN ('license_plans', 'customers', 'license_plan_versions')
                ORDER BY c.relname;
            """;
            try (PreparedStatement ps = conn.prepareStatement(checkRlsSql);
                 ResultSet rs = ps.executeQuery()) {
                int count = 0;
                while (rs.next()) {
                    count++;
                    String tableName = rs.getString("relname");
                    boolean rls = rs.getBoolean("relrowsecurity");
                    System.out.printf("Table: %s | RLS Enabled: %b%n", tableName, rls);
                    assertThat(rls).isTrue();
                }
                assertThat(count).isEqualTo(3);
            }

            // 2. Verify Views Exist
            System.out.println("\n--- Checking Views ---");
            String checkViewsSql = """
                SELECT table_name
                FROM information_schema.views
                WHERE table_schema = 'public'
                  AND table_name IN ('license_configurations', 'license_configuration_versions')
                ORDER BY table_name;
            """;
            try (PreparedStatement ps = conn.prepareStatement(checkViewsSql);
                 ResultSet rs = ps.executeQuery()) {
                List<String> views = new ArrayList<>();
                while (rs.next()) {
                    views.add(rs.getString("table_name"));
                }
                System.out.println("Verified Views: " + views);
                assertThat(views).containsExactly("license_configuration_versions", "license_configurations");
            }

            // 3. Verify Policies on Tables
            System.out.println("\n--- Checking RLS Policies ---");
            String checkPoliciesSql = """
                SELECT tablename, policyname, cmd, permissive, roles, qual, with_check
                FROM pg_policies
                WHERE schemaname = 'public'
                  AND tablename IN ('license_plans', 'customers', 'license_plan_versions')
                ORDER BY tablename, policyname;
            """;
            try (PreparedStatement ps = conn.prepareStatement(checkPoliciesSql);
                 ResultSet rs = ps.executeQuery()) {
                int policyCount = 0;
                while (rs.next()) {
                    policyCount++;
                    String tablename = rs.getString("tablename");
                    String policyname = rs.getString("policyname");
                    String qual = rs.getString("qual");
                    System.out.printf("Table: %s | Policy: %s | Cmd: %s | Qual: %s | WithCheck: %s%n",
                            tablename,
                            policyname,
                            rs.getString("cmd"),
                            qual,
                            rs.getString("with_check"));
                    if (policyname.contains("delete") || policyname.equals("plan_versions_update_policy")) {
                        assertThat(qual).isEqualTo("false");
                    }
                }
                // Expect 4 on license_plans, 4 on customers, 4 on license_plan_versions = 12 total
                assertThat(policyCount).isEqualTo(12);
            }

            // 4. Verify Foreign Key Constraints (including composite FKs)
            System.out.println("\n--- Checking Constraints ---");
            String checkConstraintsSql = """
                SELECT conname, contype, pg_get_constraintdef(oid) AS def
                FROM pg_constraint
                WHERE conrelid IN (
                    'public.license_plans'::regclass,
                    'public.customers'::regclass,
                    'public.license_plan_versions'::regclass
                )
                ORDER BY conname;
            """;
            try (PreparedStatement ps = conn.prepareStatement(checkConstraintsSql);
                 ResultSet rs = ps.executeQuery()) {
                List<String> constraintNames = new ArrayList<>();
                while (rs.next()) {
                    String name = rs.getString("conname");
                    constraintNames.add(name);
                    System.out.printf("Constraint: %s [%s] -> %s%n",
                            name, rs.getString("contype"), rs.getString("def"));
                }
                assertThat(constraintNames).contains(
                        "fk_plans_product_workspace",
                        "fk_plan_versions_product_workspace",
                        "uq_plans_product_name",
                        "uq_plans_id_workspace",
                        "uq_plan_versions_plan_ver",
                        "uq_customers_workspace_email",
                        "uq_customers_id_workspace"
                );
            }

            // 5. Verify Permissions for rightfull_app
            System.out.println("\n--- Checking Table Privileges for rightfull_app ---");
            String checkPrivsSql = """
                SELECT table_name, privilege_type
                FROM information_schema.table_privileges
                WHERE grantee = 'rightfull_app'
                  AND table_schema = 'public'
                  AND table_name IN ('license_plans', 'customers', 'license_plan_versions', 'license_configurations', 'license_configuration_versions')
                ORDER BY table_name, privilege_type;
            """;
            try (PreparedStatement ps = conn.prepareStatement(checkPrivsSql);
                 ResultSet rs = ps.executeQuery()) {
                List<String> privs = new ArrayList<>();
                while (rs.next()) {
                    privs.add(rs.getString("table_name") + ":" + rs.getString("privilege_type"));
                }
                System.out.println("Grants for rightfull_app:\n" + String.join("\n", privs));
                assertThat(privs).contains(
                        "license_plans:SELECT",
                        "license_plans:INSERT",
                        "license_plans:UPDATE",
                        "customers:SELECT",
                        "customers:INSERT",
                        "customers:UPDATE",
                        "license_plan_versions:SELECT",
                        "license_plan_versions:INSERT",
                        "license_configurations:SELECT",
                        "license_configuration_versions:SELECT"
                );
            }

            // 6. Verify rightfull_app runtime role attributes
            System.out.println("\n--- Checking rightfull_app Role Attributes ---");
            String checkRoleSql = "SELECT rolname, rolsuper, rolbypassrls FROM pg_roles WHERE rolname = 'rightfull_app';";
            try (PreparedStatement ps = conn.prepareStatement(checkRoleSql);
                 ResultSet rs = ps.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getBoolean("rolsuper")).isFalse();
                assertThat(rs.getBoolean("rolbypassrls")).isFalse();
                System.out.printf("Role %s confirmed: superuser=%b, bypassrls=%b%n",
                        rs.getString("rolname"), rs.getBoolean("rolsuper"), rs.getBoolean("rolbypassrls"));
            }

            System.out.println("\nALL POST-MIGRATION SCHEMA, RLS, AND PRIVILEGE VERIFICATIONS PASSED!");
        }
    }
}
