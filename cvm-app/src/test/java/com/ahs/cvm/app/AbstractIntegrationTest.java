package com.ahs.cvm.app;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Gemeinsame Basisklasse fuer Spring-Boot-Integrationstests. Startet einen
 * wiederverwendbaren PostgreSQL-16-Container mit pgvector-Extension.
 *
 * <p>Konkrete Testklassen muessen zusaetzlich
 * {@code @EnabledIf(value = "com.ahs.cvm.app.DockerAvailability#isAvailable", ...)}
 * tragen, damit die Testausfuehrung in Umgebungen ohne Docker-Daemon
 * uebersprungen wird. JUnit 5 erbt {@code @EnabledIf} nicht von Basisklassen.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
                    DockerImageName.parse("pgvector/pgvector:pg16")
                            .asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("cvm")
            .withUsername("cvm")
            .withPassword("cvm")
            .withReuse(true);

    static {
        if (DockerAvailability.isAvailable()) {
            try {
                POSTGRES.start();
            } catch (RuntimeException startFehler) {
                DockerAvailability.markContainerStartFailed(startFehler);
            }
        }
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.url", POSTGRES::getJdbcUrl);
        registry.add("spring.flyway.user", POSTGRES::getUsername);
        registry.add("spring.flyway.password", POSTGRES::getPassword);
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> "");
        registry.add(
                "spring.autoconfigure.exclude",
                () -> "org.springframework.boot.autoconfigure.security.oauth2."
                        + "resource.servlet.OAuth2ResourceServerAutoConfiguration");
    }
}
