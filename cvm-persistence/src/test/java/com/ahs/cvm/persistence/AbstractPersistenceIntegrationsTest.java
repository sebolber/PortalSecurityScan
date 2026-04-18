package com.ahs.cvm.persistence;

import com.ahs.cvm.persistence.support.DockerAvailability;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Basisklasse fuer {@link DataJpaTest}-basierte Integrationstests auf einem
 * Testcontainers-Postgres. Unterklassen muessen {@code @EnabledIf} direkt
 * tragen (JUnit erbt {@code @EnabledIf} nicht ueber abstrakte Basisklassen).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public abstract class AbstractPersistenceIntegrationsTest {

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
                // Docker-Socket erreichbar, Testcontainers kann die Umgebung aber nicht
                // aushandeln (z. B. Docker-Desktop-Mac ohne Standard-Host-Alias). In diesem
                // Fall Signal an DockerAvailability geben, damit die nachfolgende
                // @EnabledIf-Pruefung die Slice-Tests sauber skippt statt in einen
                // NoClassDefFoundError zu laufen.
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
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
    }
}
