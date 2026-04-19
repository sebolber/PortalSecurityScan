package com.ahs.cvm.ai.llmconfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ahs.cvm.application.llmconfig.LlmConfigurationService;
import com.ahs.cvm.application.llmconfig.LlmConfigurationView;
import com.ahs.cvm.llm.TenantLlmSettings;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LlmConfigurationTenantSettingsProviderTest {

    private final LlmConfigurationService service = mock(LlmConfigurationService.class);
    private final LlmConfigurationTenantSettingsProvider provider =
            new LlmConfigurationTenantSettingsProvider(service);

    @Test
    @DisplayName("Keine aktive Konfig: Optional.empty()")
    void keineAktiveKonfig() {
        when(service.activeForCurrentTenant()).thenReturn(Optional.empty());
        assertThat(provider.resolveCurrent()).isEmpty();
    }

    @Test
    @DisplayName("Aktive Konfig mit Secret: Settings enthaelt entschluesselten Key")
    void aktiveKonfigMitSecret() {
        UUID id = UUID.randomUUID();
        LlmConfigurationView view = new LlmConfigurationView(
                id, UUID.randomUUID(), "OpenAI Prod", null,
                "openai", "gpt-4o", "https://api.openai.com/v1",
                true, "****1234", 1024, new BigDecimal("0.3"), true,
                Instant.now(), Instant.now(), "a.admin@ahs.test");
        when(service.activeForCurrentTenant()).thenReturn(Optional.of(view));
        when(service.resolveSecret(id)).thenReturn(Optional.of("sk-geheim-1234"));

        Optional<TenantLlmSettings> result = provider.resolveCurrent();
        assertThat(result).isPresent();
        TenantLlmSettings settings = result.orElseThrow();
        assertThat(settings.provider()).isEqualTo("openai");
        assertThat(settings.model()).isEqualTo("gpt-4o");
        assertThat(settings.baseUrl()).isEqualTo("https://api.openai.com/v1");
        assertThat(settings.apiKey()).isEqualTo("sk-geheim-1234");
    }

    @Test
    @DisplayName("Aktive Konfig ohne Secret: Settings apiKey=null")
    void aktiveKonfigOhneSecret() {
        UUID id = UUID.randomUUID();
        LlmConfigurationView view = new LlmConfigurationView(
                id, UUID.randomUUID(), "Ollama", null,
                "ollama", "llama3", "http://localhost:11434/v1",
                false, null, null, null, true,
                Instant.now(), Instant.now(), "a.admin@ahs.test");
        when(service.activeForCurrentTenant()).thenReturn(Optional.of(view));

        Optional<TenantLlmSettings> result = provider.resolveCurrent();
        assertThat(result).isPresent();
        assertThat(result.orElseThrow().apiKey()).isNull();
    }

    @Test
    @DisplayName("Service wirft: Provider schluckt Fehler und liefert empty")
    void serviceWirft() {
        when(service.activeForCurrentTenant())
                .thenThrow(new IllegalStateException("Kein Default-Mandant"));
        assertThat(provider.resolveCurrent()).isEmpty();
    }
}
