package com.ahs.cvm.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Prueft, dass die Baseline-Migration die benoetigten Extensions aktiviert
 * und die {@code audit_trail}-Tabelle anlegt.
 */
@EnabledIf(
        value = "com.ahs.cvm.app.DockerAvailability#isAvailable",
        disabledReason = "Docker-Daemon nicht erreichbar")
class FlywayBaselineTest extends AbstractIntegrationTest {

    @Autowired
    DataSource dataSource;

    @Test
    @DisplayName("Baseline aktiviert uuid-ossp- und vector-Extension")
    void extensionsSindInstalliert() throws Exception {
        Set<String> extensions = new HashSet<>();
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(
                        "SELECT extname FROM pg_extension")) {
            while (resultSet.next()) {
                extensions.add(resultSet.getString(1));
            }
        }
        assertThat(extensions).contains("uuid-ossp", "vector");
    }

    @Test
    @DisplayName("Baseline legt audit_trail-Tabelle an")
    void auditTrailTabelleExistiert() throws Exception {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(
                        "SELECT to_regclass('public.audit_trail') AS name")) {
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getString("name")).isEqualTo("audit_trail");
        }
    }
}
