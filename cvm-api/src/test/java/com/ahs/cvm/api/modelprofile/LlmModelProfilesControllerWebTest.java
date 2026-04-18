package com.ahs.cvm.api.modelprofile;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ahs.cvm.application.modelprofile.ModelProfileQueryService;
import com.ahs.cvm.application.modelprofile.ModelProfileService;
import com.ahs.cvm.application.modelprofile.ModelProfileView;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = LlmModelProfilesController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class)
class LlmModelProfilesControllerWebTest {

    @Autowired MockMvc mockMvc;
    @MockBean ModelProfileQueryService service;
    // Pflicht-Mock: der Package-ComponentScan (ModelProfileTestApi) zieht den
    // ModelProfileController mit, der ohne dieses Mock nicht initialisiert.
    @MockBean ModelProfileService switchService;

    @Test
    @DisplayName("GET /api/v1/llm-model-profiles: Liste mit Profil-Keys")
    void list() throws Exception {
        given(service.listAll()).willReturn(List.of(
                new ModelProfileView(UUID.randomUUID(),
                        "CLAUDE_CLOUD_DEFAULT", "CLAUDE_CLOUD",
                        "claude-sonnet-4-6", "sonnet-4.6",
                        new BigDecimal("100.00"), true),
                new ModelProfileView(UUID.randomUUID(),
                        "OLLAMA_ONPREM_FALLBACK", "OLLAMA_ONPREM",
                        "llama3:8b", null,
                        BigDecimal.ZERO, true)));

        mockMvc.perform(get("/api/v1/llm-model-profiles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].profileKey").value("CLAUDE_CLOUD_DEFAULT"))
                .andExpect(jsonPath("$[0].provider").value("CLAUDE_CLOUD"))
                .andExpect(jsonPath("$[1].profileKey").value("OLLAMA_ONPREM_FALLBACK"));
    }

    @Test
    @DisplayName("GET: leere Liste fuehrt zu 200 mit []")
    void emptyList() throws Exception {
        given(service.listAll()).willReturn(List.of());
        mockMvc.perform(get("/api/v1/llm-model-profiles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
