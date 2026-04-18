package com.ahs.cvm.api.rulesuggestion;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ahs.cvm.ai.ruleextraction.ApproveRuleResult;
import com.ahs.cvm.ai.ruleextraction.RuleSuggestionService;
import com.ahs.cvm.ai.ruleextraction.RuleSuggestionView;
import com.ahs.cvm.domain.enums.AhsSeverity;
import com.ahs.cvm.domain.enums.RuleStatus;
import com.ahs.cvm.domain.enums.RuleSuggestionStatus;
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
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = RuleSuggestionsController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class)
@Import(RuleSuggestionsExceptionHandler.class)
class RuleSuggestionsControllerWebTest {

    private static final UUID ID = UUID.fromString("77777777-7777-7777-7777-777777777777");

    @Autowired MockMvc mockMvc;
    @MockBean RuleSuggestionService service;

    private RuleSuggestionView view(RuleSuggestionStatus status) {
        return new RuleSuggestionView(
                ID, "ai-rule", AhsSeverity.LOW, "{\"all\":[]}",
                "tpl", "cluster", 7, 7, BigDecimal.ONE, 0,
                status, "system:rule-extraction",
                Instant.parse("2026-04-18T10:00:00Z"));
    }

    @Test
    @DisplayName("GET /rules/suggestions: liefert offene Liste")
    void liste() throws Exception {
        given(service.listeOffene()).willReturn(List.of(view(RuleSuggestionStatus.PROPOSED)));
        mockMvc.perform(get("/api/v1/rules/suggestions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("ai-rule"))
                .andExpect(jsonPath("$[0].proposedSeverity").value("LOW"));
    }

    @Test
    @DisplayName("POST /approve: 200 mit Rule-Info")
    void approve() throws Exception {
        given(service.approveAsView(eq(ID), eq("a.admin@ahs.test")))
                .willReturn(new ApproveRuleResult(
                        UUID.randomUUID(), "ai-abc", RuleStatus.ACTIVE,
                        Instant.parse("2026-04-18T10:00:00Z")));

        mockMvc.perform(post("/api/v1/rules/suggestions/" + ID + "/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approvedBy\":\"a.admin@ahs.test\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ruleKey").value("ai-abc"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("POST /approve: Vier-Augen-Verstoss -> 409")
    void approveVierAugen() throws Exception {
        willThrow(new IllegalStateException("Vier-Augen: approver == suggester"))
                .given(service).approveAsView(any(), any());

        mockMvc.perform(post("/api/v1/rules/suggestions/" + ID + "/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approvedBy\":\"x\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("rule_suggestion_conflict"));
    }

    @Test
    @DisplayName("POST /reject: 200, Comment Pflicht")
    void reject() throws Exception {
        given(service.rejectAsView(eq(ID), eq("a.admin@ahs.test"), eq("dup")))
                .willReturn(view(RuleSuggestionStatus.REJECTED));

        mockMvc.perform(post("/api/v1/rules/suggestions/" + ID + "/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rejectedBy\":\"a.admin@ahs.test\",\"comment\":\"dup\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    @DisplayName("POST /reject: fehlender Kommentar -> 400")
    void rejectOhneKommentar() throws Exception {
        mockMvc.perform(post("/api/v1/rules/suggestions/" + ID + "/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rejectedBy\":\"x\",\"comment\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /approve: unbekannte Id -> 404")
    void notFound() throws Exception {
        willThrow(new IllegalArgumentException("RuleSuggestion nicht gefunden: " + ID))
                .given(service).approveAsView(any(), any());
        mockMvc.perform(post("/api/v1/rules/suggestions/" + ID + "/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approvedBy\":\"a.admin@ahs.test\"}"))
                .andExpect(status().isNotFound());
    }
}
