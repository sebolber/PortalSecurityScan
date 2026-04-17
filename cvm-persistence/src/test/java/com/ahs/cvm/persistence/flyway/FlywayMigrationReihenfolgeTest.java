package com.ahs.cvm.persistence.flyway;

import static org.assertj.core.api.Assertions.assertThat;

import com.ahs.cvm.persistence.AbstractPersistenceIntegrationsTest;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Prueft, dass die Migrationen V0000 bis V0005 geordnet durchlaufen und alle
 * fachlichen Tabellen erzeugen.
 */
@EnabledIf(
        value = "com.ahs.cvm.persistence.support.DockerAvailability#isAvailable",
        disabledReason = "Docker-Daemon nicht erreichbar")
class FlywayMigrationReihenfolgeTest extends AbstractPersistenceIntegrationsTest {

    @Autowired
    DataSource dataSource;

    @Test
    @DisplayName("flyway_schema_history: Migrationen V0000-V0005 sind gelaufen")
    void migrationenSindVollstaendig() throws Exception {
        List<String> versionen = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(
                        "SELECT version FROM flyway_schema_history "
                                + "WHERE success = TRUE ORDER BY installed_rank")) {
            while (resultSet.next()) {
                versionen.add(resultSet.getString(1));
            }
        }
        assertThat(versionen)
                .startsWith("0000", "0001", "0002", "0003", "0004", "0005");
    }

    @Test
    @DisplayName("Fachliche Tabellen existieren nach Migration")
    void tabellenExistieren() throws Exception {
        List<String> erwartet = List.of(
                "product",
                "product_version",
                "environment",
                "environment_deployment",
                "context_profile",
                "scan",
                "component",
                "component_occurrence",
                "cve",
                "finding",
                "assessment",
                "mitigation_plan",
                "audit_trail");
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
            for (String tabelle : erwartet) {
                try (ResultSet rs = statement.executeQuery(
                        "SELECT to_regclass('public." + tabelle + "')")) {
                    rs.next();
                    assertThat(rs.getString(1))
                            .as("Tabelle %s existiert", tabelle)
                            .isEqualTo(tabelle);
                }
            }
        }
    }
}
