package com.ahs.cvm.application.llmconfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.ahs.cvm.application.llmconfig.LlmConfigurationCommands.Create;
import com.ahs.cvm.application.llmconfig.LlmConfigurationCommands.Update;
import com.ahs.cvm.application.llmconfig.LlmConfigurationService.LlmConfigurationNotFoundException;
import com.ahs.cvm.application.scan.SbomEncryption;
import com.ahs.cvm.application.tenant.TenantContext;
import com.ahs.cvm.application.tenant.TenantLookupService;
import com.ahs.cvm.persistence.llmconfig.LlmConfiguration;
import com.ahs.cvm.persistence.llmconfig.LlmConfigurationRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LlmConfigurationServiceTest {

    private static final UUID TENANT = UUID.randomUUID();

    private LlmConfigurationRepository repository;
    private TenantLookupService tenantLookup;
    private SbomEncryption encryption;
    private LlmConnectionTester tester;
    private LlmConfigurationService service;

    @BeforeEach
    void setUp() {
        repository = mock(LlmConfigurationRepository.class);
        tenantLookup = mock(TenantLookupService.class);
        encryption = new SbomEncryption("iteration-34-test-secret");
        tester = mock(LlmConnectionTester.class);
        service = new LlmConfigurationService(repository, tenantLookup, encryption, tester);
        TenantContext.set(TENANT);
        given(repository.save(any(LlmConfiguration.class)))
                .willAnswer(inv -> {
                    LlmConfiguration c = inv.getArgument(0);
                    if (c.getId() == null) c.setId(UUID.randomUUID());
                    setzeMinimalZeitstempel(c);
                    return c;
                });
    }

    private static void setzeMinimalZeitstempel(LlmConfiguration c) {
        java.time.Instant now = java.time.Instant.now();
        if (c.getCreatedAt() == null) c.setCreatedAt(now);
        c.setUpdatedAt(now);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Create: Default-baseUrl wird gesetzt, Secret verschluesselt")
    void createHappyPath() {
        given(repository.findByTenantIdAndName(TENANT, "OpenAI Prod"))
                .willReturn(Optional.empty());

        LlmConfigurationView view = service.create(new Create(
                "OpenAI Prod", "Produktionskonfig", "OpenAI",
                "gpt-4o", null, "sk-test12345",
                2048, new BigDecimal("0.30"), false),
                "a.admin@ahs.test");

        assertThat(view.baseUrl()).isEqualTo("https://api.openai.com/v1");
        assertThat(view.provider()).isEqualTo("openai");
        assertThat(view.secretSet()).isTrue();
        assertThat(view.secretHint()).isEqualTo("****2345");
    }

    @Test
    @DisplayName("Create: Azure ohne explizite baseUrl -> IllegalArgument")
    void createAzureOhneBaseUrl() {
        assertThatThrownBy(() -> service.create(new Create(
                        "Azure", null, "azure", "gpt-4o", null, null,
                        null, null, false),
                "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("explizite baseUrl");
    }

    @Test
    @DisplayName("Create: unbekannter Provider -> IllegalArgument")
    void createUnbekannterProvider() {
        assertThatThrownBy(() -> service.create(new Create(
                        "Foo", null, "foobar", "xx", null, null,
                        null, null, false),
                "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unbekannter Provider");
    }

    @Test
    @DisplayName("Create: temperature > 1.0 -> IllegalArgument")
    void createTemperaturUngueltig() {
        assertThatThrownBy(() -> service.create(new Create(
                        "OK", null, "openai", "gpt-4o", null, null,
                        null, new BigDecimal("1.50"), false),
                "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("temperature");
    }

    @Test
    @DisplayName("Create: active=true deaktiviert andere Aktive per Query")
    void createActiveFlagSetztAndereAuf0() {
        service.create(new Create(
                "OpenAI Prod", null, "openai", "gpt-4o", null,
                null, null, null, true), "admin");
        verify(repository).deaktiviereAndereAktive(TENANT, null);
    }

    @Test
    @DisplayName("Create: active=false loest KEIN Mass-Deactivate aus")
    void createInaktivTouchtNichts() {
        service.create(new Create(
                "OpenAI Test", null, "openai", "gpt-4o", null,
                null, null, null, false), "admin");
        verify(repository, never()).deaktiviereAndereAktive(any(), any());
    }

    @Test
    @DisplayName("Update: partiell - null-Felder bleiben, Secret wird verschluesselt")
    void updatePartial() {
        LlmConfiguration existing = LlmConfiguration.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT)
                .name("Alt")
                .provider("openai")
                .model("gpt-4o")
                .baseUrl("https://api.openai.com/v1")
                .active(false)
                .build();
        setzeMinimalZeitstempel(existing);
        given(repository.findById(existing.getId()))
                .willReturn(Optional.of(existing));

        LlmConfigurationView view = service.update(existing.getId(),
                new Update(null, "Neue Beschreibung", null, null,
                        null, "sk-neu1234", false, 4096, null, null),
                "admin");

        assertThat(view.name()).isEqualTo("Alt");
        assertThat(view.description()).isEqualTo("Neue Beschreibung");
        assertThat(view.maxTokens()).isEqualTo(4096);
        assertThat(view.secretHint()).isEqualTo("****1234");
    }

    @Test
    @DisplayName("Update: secretClear=true loescht den Secret")
    void updateSecretClear() {
        LlmConfiguration existing = LlmConfiguration.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT)
                .name("Mit Secret")
                .provider("anthropic")
                .model("claude-sonnet")
                .baseUrl("https://api.anthropic.com/v1")
                .secretRef("irgendein-ciphertext")
                .active(false)
                .build();
        setzeMinimalZeitstempel(existing);
        given(repository.findById(existing.getId()))
                .willReturn(Optional.of(existing));

        LlmConfigurationView view = service.update(existing.getId(),
                new Update(null, null, null, null, null,
                        null, true, null, null, null),
                "admin");

        assertThat(view.secretSet()).isFalse();
    }

    @Test
    @DisplayName("Update: fremder Tenant -> NotFound-Exception (Isolation)")
    void updateFremderTenant() {
        LlmConfiguration foreign = LlmConfiguration.builder()
                .id(UUID.randomUUID())
                .tenantId(UUID.randomUUID())  // anderer Mandant
                .name("x")
                .provider("openai")
                .model("gpt-4o")
                .build();
        setzeMinimalZeitstempel(foreign);
        given(repository.findById(foreign.getId())).willReturn(Optional.of(foreign));

        assertThatThrownBy(() -> service.update(foreign.getId(),
                new Update(null, null, null, null, null,
                        null, false, null, null, null),
                "admin"))
                .isInstanceOf(LlmConfigurationNotFoundException.class);
    }

    @Test
    @DisplayName("resolveSecret: liefert den entschluesselten Klartext")
    void resolveSecretRoundTrip() {
        LlmConfiguration existing = LlmConfiguration.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT)
                .name("R")
                .provider("openai")
                .model("gpt-4o")
                .baseUrl("https://api.openai.com/v1")
                .active(false)
                .build();
        setzeMinimalZeitstempel(existing);
        given(repository.findByTenantIdAndName(TENANT, "R"))
                .willReturn(Optional.empty());
        given(repository.findById(existing.getId()))
                .willReturn(Optional.of(existing));
        given(repository.save(any(LlmConfiguration.class)))
                .willAnswer(inv -> {
                    LlmConfiguration c = inv.getArgument(0);
                    if (c.getId() == null) c.setId(existing.getId());
                    existing.setSecretRef(c.getSecretRef());
                    return existing;
                });

        service.create(new Create(
                "R", null, "openai", "gpt-4o", null,
                "sk-secret-plaintext", null, null, false), "admin");

        Optional<String> recovered = service.resolveSecret(existing.getId());
        assertThat(recovered).contains("sk-secret-plaintext");
    }

    @Test
    @DisplayName("testConnection ad-hoc: reicht den Command 1:1 an den Tester weiter")
    void testConnectionAdhoc() {
        LlmConfigurationTestResult expected = LlmConfigurationTestResult.success(
                "openai", "gpt-4o", 200, 42L, "OK");
        given(tester.test(any(LlmConfigurationTestCommand.class)))
                .willReturn(expected);

        LlmConfigurationTestResult result = service.testConnection(
                new LlmConfigurationTestCommand(
                        null, "openai", "gpt-4o",
                        "https://api.openai.com/v1", "sk-test"));

        assertThat(result).isSameAs(expected);
        org.mockito.ArgumentCaptor<LlmConfigurationTestCommand> cap =
                org.mockito.ArgumentCaptor.forClass(LlmConfigurationTestCommand.class);
        verify(tester).test(cap.capture());
        assertThat(cap.getValue().id()).isNull();
        assertThat(cap.getValue().apiKey()).isEqualTo("sk-test");
    }

    @Test
    @DisplayName("testConnection mit id: fehlende Felder werden aus DB ergaenzt, Secret entschluesselt")
    void testConnectionMitId() {
        LlmConfiguration existing = LlmConfiguration.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT)
                .name("Saved")
                .provider("anthropic")
                .model("claude-sonnet-4-6")
                .baseUrl("https://api.anthropic.com")
                .secretRef(encryption.encrypt("sk-gespeichert"
                        .getBytes(java.nio.charset.StandardCharsets.UTF_8))
                        != null
                    ? java.util.Base64.getEncoder().encodeToString(
                        encryption.encrypt("sk-gespeichert"
                                .getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                    : null)
                .active(true)
                .build();
        setzeMinimalZeitstempel(existing);
        given(repository.findById(existing.getId()))
                .willReturn(Optional.of(existing));
        given(tester.test(any(LlmConfigurationTestCommand.class)))
                .willReturn(LlmConfigurationTestResult.success(
                        "anthropic", "claude-sonnet-4-6", 200, 10L, "OK"));

        service.testConnection(new LlmConfigurationTestCommand(
                existing.getId(), null, null, null, null));

        org.mockito.ArgumentCaptor<LlmConfigurationTestCommand> cap =
                org.mockito.ArgumentCaptor.forClass(LlmConfigurationTestCommand.class);
        verify(tester).test(cap.capture());
        LlmConfigurationTestCommand sent = cap.getValue();
        assertThat(sent.provider()).isEqualTo("anthropic");
        assertThat(sent.model()).isEqualTo("claude-sonnet-4-6");
        assertThat(sent.baseUrl()).isEqualTo("https://api.anthropic.com");
        assertThat(sent.apiKey()).isEqualTo("sk-gespeichert");
    }

    @Test
    @DisplayName("testConnection mit id: explizite Request-Felder ueberschreiben DB-Werte")
    void testConnectionMitIdUeberschreibt() {
        LlmConfiguration existing = LlmConfiguration.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT)
                .name("Saved")
                .provider("openai")
                .model("gpt-4o")
                .baseUrl("https://api.openai.com/v1")
                .secretRef(java.util.Base64.getEncoder().encodeToString(
                        encryption.encrypt("sk-alt"
                                .getBytes(java.nio.charset.StandardCharsets.UTF_8))))
                .active(false)
                .build();
        setzeMinimalZeitstempel(existing);
        given(repository.findById(existing.getId()))
                .willReturn(Optional.of(existing));
        given(tester.test(any(LlmConfigurationTestCommand.class)))
                .willReturn(LlmConfigurationTestResult.success(
                        "openai", "gpt-4o-mini", 200, 5L, "OK"));

        service.testConnection(new LlmConfigurationTestCommand(
                existing.getId(), null, "gpt-4o-mini", null, "sk-neu"));

        org.mockito.ArgumentCaptor<LlmConfigurationTestCommand> cap =
                org.mockito.ArgumentCaptor.forClass(LlmConfigurationTestCommand.class);
        verify(tester).test(cap.capture());
        assertThat(cap.getValue().model()).isEqualTo("gpt-4o-mini");
        assertThat(cap.getValue().apiKey()).isEqualTo("sk-neu");
        assertThat(cap.getValue().baseUrl()).isEqualTo("https://api.openai.com/v1");
    }
}
