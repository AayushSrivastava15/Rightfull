package com.rightfull.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

public class FlywayMigrationVerifier {

    private static final String DB_URL = "jdbc:postgresql://aws-0-ap-south-1.pooler.supabase.com:5432/postgres?sslmode=require";
    private static final String ADMIN_USER = "postgres.kcyccddrzsfxgrkeenop";
    private static final String ADMIN_PASS = "utN7hNkB+-Rmc77";

    @Test
    @DisplayName("Step 4.0.3: Execute Flyway Migration Staging for V4, V5, V6")
    void executeFlywayMigrationStaging() {
        Flyway flyway = Flyway.configure()
                .dataSource(DB_URL, ADMIN_USER, ADMIN_PASS)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .load();

        MigrationInfoService infoService = flyway.info();
        System.out.println("=== PRE-MIGRATION FLYWAY INFO ===");
        for (MigrationInfo info : infoService.all()) {
            System.out.printf("Version: %s | Description: %s | State: %s | Checksum: %s | Installed On: %s%n",
                    info.getVersion(), info.getDescription(), info.getState(), info.getChecksum(), info.getInstalledOn());
        }

        // Execute pending migrations
        System.out.println("\nApplying pending migrations...");
        MigrateResult result = flyway.migrate();
        System.out.printf("Flyway migrate complete! Migrations executed: %d, Target schema version: %s, Success: %b%n",
                result.migrationsExecuted, result.targetSchemaVersion, result.success);

        assertThat(result.success).isTrue();

        // Validate applied migrations post-migration
        System.out.println("\nValidating all migrations...");
        flyway.validate();
        System.out.println("Flyway validate succeeded!");

        MigrationInfoService postInfo = flyway.info();
        System.out.println("\n=== POST-MIGRATION FLYWAY INFO ===");
        for (MigrationInfo info : postInfo.all()) {
            System.out.printf("Version: %s | Description: %s | State: %s | Checksum: %s | Installed On: %s%n",
                    info.getVersion(), info.getDescription(), info.getState(), info.getChecksum(), info.getInstalledOn());
        }

        // Verify that all 6 migrations are SUCCESS
        for (MigrationInfo info : postInfo.all()) {
            assertThat(info.getState().isApplied()).isTrue();
        }
    }
}
