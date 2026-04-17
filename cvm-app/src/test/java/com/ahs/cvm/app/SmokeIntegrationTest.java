package com.ahs.cvm.app;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

/**
 * Rauchtest: Die Spring-Boot-Anwendung startet zusammen mit Testcontainers-Postgres
 * und der Actuator-Health-Endpunkt liefert Status {@code UP}.
 */
@EnabledIf(
        value = "com.ahs.cvm.app.DockerAvailability#isAvailable",
        disabledReason = "Docker-Daemon nicht erreichbar")
class SmokeIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    TestRestTemplate restTemplate;

    @Test
    @DisplayName("Actuator Health liefert UP und HTTP 200")
    void actuatorHealthIstUp() {
        ResponseEntity<String> response =
                restTemplate.getForEntity("/actuator/health", String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("\"status\":\"UP\"");
    }
}
