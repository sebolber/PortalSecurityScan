package com.ahs.cvm.ai.reachability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.ahs.cvm.application.parameter.SystemParameterResolver;
import com.ahs.cvm.application.parameter.SystemParameterSecretCipher;
import com.ahs.cvm.application.tenant.TenantContext;
import com.ahs.cvm.domain.enums.SystemParameterType;
import com.ahs.cvm.persistence.parameter.SystemParameter;
import com.ahs.cvm.persistence.parameter.SystemParameterRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Iteration 46 (CVM-96): End-to-End-Test des Parameter-Store-Lesepfads fuer
 * {@code cvm.ai.reachability.enabled}.
 *
 * <p>Der Test verdrahtet die realen Klassen
 * ({@link SystemParameterResolver}, {@link SystemParameterSecretCipher},
 * {@link ReachabilityConfig}) und beweist, dass eine Aenderung am
 * DB-Wert das {@code enabledEffective()}-Ergebnis umdreht, ohne die
 * {@link ReachabilityConfig}-Bean neu zu bauen - also **ohne Neustart**.
 *
 * <p>Ein echter Testcontainers-Lauf ist in der Sandbox nicht moeglich
 * (kein Docker); die DB wird hier durch einen Mock des Repositories
 * simuliert, aber die Service-Klassen selbst sind die produktiven
 * Implementierungen.
 */
class ReachabilityRuntimeOverrideE2ETest {

    private static final String KEY = "cvm.ai.reachability.enabled";

    private SystemParameterRepository repository;
    private SystemParameterResolver resolver;
    private ReachabilityConfig config;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        repository = mock(SystemParameterRepository.class);
        resolver = new SystemParameterResolver(
                repository, new SystemParameterSecretCipher("e2e-test-key"));
        // Boot-Default: enabled=false. Der Resolver soll ueberschreiben.
        config = new ReachabilityConfig(false, 300, "claude");
        config.setResolver(resolver);
        tenantId = UUID.randomUUID();
        TenantContext.set(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("DB hat keinen Eintrag: enabledEffective liefert den Boot-Default (false)")
    void kein_eintrag_default() {
        given(repository.findByTenantIdAndParamKey(tenantId, KEY))
                .willReturn(Optional.empty());

        assertThat(config.enabledEffective()).isFalse();
    }

    @Test
    @DisplayName("DB-Zeile mit value=true dreht enabledEffective um (ohne Neustart der Bean)")
    void db_true_aktiviert_ohne_neustart() {
        given(repository.findByTenantIdAndParamKey(tenantId, KEY))
                .willReturn(Optional.of(eintrag("true")));

        assertThat(config.enabledEffective()).isTrue();

        // Zweite Abfrage simuliert einen erneuten Request: Der Wert bleibt true.
        assertThat(config.enabledEffective()).isTrue();
    }

    @Test
    @DisplayName("Aendert sich der DB-Wert auf false, dreht enabledEffective beim naechsten Aufruf zurueck")
    void db_wechsel_wirkt_sofort() {
        given(repository.findByTenantIdAndParamKey(tenantId, KEY))
                .willReturn(Optional.of(eintrag("true")));
        assertThat(config.enabledEffective()).isTrue();

        // Runtime-Override: Admin stellt zurueck auf false.
        given(repository.findByTenantIdAndParamKey(tenantId, KEY))
                .willReturn(Optional.of(eintrag("false")));
        assertThat(config.enabledEffective()).isFalse();
    }

    @Test
    @DisplayName("Ohne Tenant-Kontext bleibt der Boot-Default wirksam (Hintergrund-Jobs)")
    void ohne_tenant_kontext_boot_default() {
        TenantContext.clear();
        assertThat(config.enabledEffective()).isFalse();
    }

    private SystemParameter eintrag(String value) {
        return SystemParameter.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .paramKey(KEY)
                .label(KEY)
                .category("AI_REACHABILITY")
                .type(SystemParameterType.BOOLEAN)
                .value(value)
                .build();
    }
}
