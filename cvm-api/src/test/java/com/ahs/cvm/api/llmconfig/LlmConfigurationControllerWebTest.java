package com.ahs.cvm.api.llmconfig;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ahs.cvm.application.llmconfig.LlmConfigurationCommands;
import com.ahs.cvm.application.llmconfig.LlmConfigurationService;
import com.ahs.cvm.application.llmconfig.LlmConfigurationService.LlmConfigurationNotFoundException;
import com.ahs.cvm.application.llmconfig.LlmConfigurationView;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = LlmConfigurationController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class)
@org.springframework.test.context.ContextConfiguration(
        classes = LlmConfigurationControllerWebTest.LlmConfigTestApp.class)
class LlmConfigurationControllerWebTest {

    @org.springframework.boot.SpringBootConfiguration
    @org.springframework.boot.autoconfigure.EnableAutoConfiguration
    @org.springframework.context.annotation.ComponentScan(
            basePackages = "com.ahs.cvm.api.llmconfig")
    static class LlmConfigTestApp {}

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper json;
    @MockBean LlmConfigurationService service;

    private static LlmConfigurationView sample() {
        return new LlmConfigurationView(
                UUID.randomUUID(), UUID.randomUUID(),
                "OpenAI Prod", "Desc", "openai", "gpt-4o",
                "https://api.openai.com/v1",
                true, "****abcd",
                4096, new BigDecimal("0.30"), false,
                Instant.parse("2026-04-19T10:00:00Z"),
                Instant.parse("2026-04-19T10:00:00Z"),
                "admin");
    }

    @Test
    @DisplayName("GET /providers liefert alle bekannten Provider mit Default-URL")
    void providerListe() throws Exception {
        mockMvc.perform(get("/api/v1/admin/llm-configurations/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.provider=='openai')].defaultBaseUrl")
                        .value("https://api.openai.com/v1"))
                .andExpect(jsonPath("$[?(@.provider=='azure')].requiresExplicitBaseUrl")
                        .value(true));
    }

    @Test
    @DisplayName("GET / listet Konfigurationen des aktuellen Mandanten")
    void listeAlle() throws Exception {
        given(service.listForCurrentTenant()).willReturn(List.of(sample()));
        mockMvc.perform(get("/api/v1/admin/llm-configurations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].provider").value("openai"))
                .andExpect(jsonPath("$[0].secretSet").value(true));
    }

    @Test
    @DisplayName("POST / legt Konfiguration an, Location-Header gesetzt")
    void postHappyPath() throws Exception {
        LlmConfigurationView saved = sample();
        given(service.create(any(LlmConfigurationCommands.Create.class), anyString()))
                .willReturn(saved);

        Map<String, Object> body = Map.of(
                "name", "OpenAI Prod",
                "provider", "openai",
                "model", "gpt-4o",
                "active", true);

        mockMvc.perform(post("/api/v1/admin/llm-configurations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(saved.id().toString()))
                .andExpect(jsonPath("$.secretSet").value(true));
    }

    @Test
    @DisplayName("POST / mit unbekanntem Provider liefert 400")
    void postUnbekannterProvider() throws Exception {
        willThrow(new IllegalArgumentException("Unbekannter Provider 'foobar'"))
                .given(service)
                .create(any(LlmConfigurationCommands.Create.class), anyString());

        Map<String, Object> body = Map.of(
                "name", "X", "provider", "foobar", "model", "m");
        mockMvc.perform(post("/api/v1/admin/llm-configurations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("llm_configuration_validation"));
    }

    @Test
    @DisplayName("PUT /{id} Teil-Update")
    void putUpdate() throws Exception {
        UUID id = UUID.randomUUID();
        given(service.update(eq(id), any(LlmConfigurationCommands.Update.class),
                anyString())).willReturn(sample());

        Map<String, Object> body = Map.of("description", "Neuer Text");
        mockMvc.perform(put("/api/v1/admin/llm-configurations/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /{id} nicht existent liefert 404")
    void getNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        given(service.findById(id))
                .willThrow(new LlmConfigurationNotFoundException(id));
        mockMvc.perform(get("/api/v1/admin/llm-configurations/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("llm_configuration_not_found"));
    }

    @Test
    @DisplayName("DELETE liefert 204 No Content")
    void deleteOk() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(delete("/api/v1/admin/llm-configurations/" + id))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /active liefert 204 wenn nichts aktiv")
    void getActiveLeer() throws Exception {
        given(service.activeForCurrentTenant())
                .willReturn(java.util.Optional.empty());
        mockMvc.perform(get("/api/v1/admin/llm-configurations/active"))
                .andExpect(status().isNoContent());
    }
}
