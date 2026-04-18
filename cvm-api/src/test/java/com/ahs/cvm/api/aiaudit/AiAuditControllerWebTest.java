package com.ahs.cvm.api.aiaudit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ahs.cvm.ai.audit.AiCallAuditQueryService;
import com.ahs.cvm.ai.audit.AiCallAuditView;
import com.ahs.cvm.domain.enums.AiCallStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = AiAuditController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class)
class AiAuditControllerWebTest {

    @Autowired MockMvc mockMvc;
    @MockBean AiCallAuditQueryService service;

    private AiCallAuditView view() {
        return new AiCallAuditView(
                UUID.fromString("aa000000-0000-0000-0000-000000000000"),
                "AUTO_ASSESSMENT",
                "claude-sonnet-4-6",
                AiCallStatus.OK,
                false,
                100, 30, 250,
                new BigDecimal("0.0015"),
                "t.tester@ahs.test",
                null,
                Instant.parse("2026-04-18T10:00:00Z"),
                Instant.parse("2026-04-18T10:00:01Z"),
                null, null);
    }

    @Test
    @DisplayName("AI-Audit: GET ohne Filter liefert paginierte Liste")
    void liste() throws Exception {
        given(service.liste(eq(null), eq(null), anyInt(), anyInt()))
                .willReturn(new PageImpl<>(List.of(view())));

        mockMvc.perform(get("/api/v1/ai/audits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].useCase").value("AUTO_ASSESSMENT"))
                .andExpect(jsonPath("$.content[0].status").value("OK"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("AI-Audit: Filter status+useCase werden weitergereicht")
    void filter() throws Exception {
        given(service.liste(eq(AiCallStatus.INVALID_OUTPUT), eq("COPILOT_REFINE_RATIONALE"),
                        eq(0), eq(50)))
                .willReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/ai/audits")
                        .param("status", "INVALID_OUTPUT")
                        .param("useCase", "COPILOT_REFINE_RATIONALE")
                        .param("page", "0")
                        .param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("AI-Audit: View enthaelt KEINE Prompt-Daten (Secret-Schutz)")
    void keineSecrets() throws Exception {
        given(service.liste(any(), any(), anyInt(), anyInt()))
                .willReturn(new PageImpl<>(List.of(view())));

        mockMvc.perform(get("/api/v1/ai/audits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].systemPrompt").doesNotExist())
                .andExpect(jsonPath("$.content[0].userPrompt").doesNotExist())
                .andExpect(jsonPath("$.content[0].rawResponse").doesNotExist());
    }
}
