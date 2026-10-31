package com.rightfull.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Safe database connectivity verification runner executed on application startup.
 * Verifies live JDBC connection, executes 'SELECT 1', logs product name and version,
 * and ensures no sensitive credentials, hosts, or passwords are exposed.
 */
@Component
public class DatabaseConnectionVerifier implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConnectionVerifier.class);

    private final DataSource dataSource;

    public DatabaseConnectionVerifier(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("Verifying database connectivity...");
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            DatabaseMetaData metaData = connection.getMetaData();
            String databaseProductName = metaData.getDatabaseProductName();
            String databaseProductVersion = metaData.getDatabaseProductVersion();

            try (ResultSet resultSet = statement.executeQuery("SELECT 1")) {
                if (resultSet.next() && resultSet.getInt(1) == 1) {
                    log.info("Database connectivity verified successfully: {} (Version: {})",
                            databaseProductName, databaseProductVersion);
                } else {
                    throw new IllegalStateException("Database validation query 'SELECT 1' failed to return expected value 1");
                }
            }
        } catch (Exception ex) {
            log.error("Database connectivity verification failed: {}", ex.getMessage());
            throw ex;
        }
    }
}

