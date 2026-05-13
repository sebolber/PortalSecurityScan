package com.ahs.cvm.persistence;

import com.ahs.cvm.persistence.support.DockerAvailability;
import java.util.UUID;
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

    /**
     * Default-Mandant, der von Flyway-Migration V0023 als
     * {@code is_default = TRUE} angelegt wird. Persistenz-Tests haben
     * keine Aspect-Infrastruktur, die tenant_id automatisch fuellt -
     * Fixtures setzen die Mandanten-Zuordnung deshalb explizit hierueber.
     */
    protected static final UUID DEFAULT_TENANT_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");

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
