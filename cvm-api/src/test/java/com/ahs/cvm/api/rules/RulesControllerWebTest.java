package com.ahs.cvm.api.rules;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ahs.cvm.application.rules.DryRunResult;
import com.ahs.cvm.application.rules.DryRunService;
import com.ahs.cvm.application.rules.RuleConditionException;
import com.ahs.cvm.application.rules.RuleFourEyesViolationException;
import com.ahs.cvm.application.rules.RuleService;
import com.ahs.cvm.application.rules.RuleView;
import com.ahs.cvm.domain.enums.AhsSeverity;
import com.ahs.cvm.domain.enums.RuleOrigin;
import com.ahs.cvm.domain.enums.RuleStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
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
        controllers = RulesController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class)
@Import(RulesExceptionHandler.class)
class RulesControllerWebTest {

    @Autowired MockMvc mockMvc;

    @MockBean RuleService ruleService;
    @MockBean DryRunService dryRunService;

    private static final UUID RULE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Test
    @DisplayName("GET /rules: liefert Liste aller Regeln als JSON-Array")
    void listeRegeln() throws Exception {
        given(ruleService.listAll()).willReturn(List.of(neueView(RuleStatus.ACTIVE)));

        mockMvc.perform(get("/api/v1/rules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ruleKey").value("k-1"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    @DisplayName("POST /rules: 201 Created mit Draft-Status")
    void anlegen() throws Exception {
        given(ruleService.proposeRule(any(), eq("a.admin@ahs.test")))
                .willReturn(neueView(RuleStatus.DRAFT));

        String body = """
                {
                  "ruleKey": "k-1",
                  "name": "Regel 1",
                  "proposedSeverity": "LOW",
                  "conditionJson": "{\\"eq\\":{\\"path\\":\\"cve.kev\\",\\"value\\":true}}",
                  "rationaleTemplate": "CVE {cve.id}",
                  "createdBy": "a.admin@ahs.test"
                }
                """;

        mockMvc.perform(post("/api/v1/rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    @DisplayName("POST /rules: 400 bei ungueltiger Condition")
    void anlegenKonditionUngueltig() throws Exception {
        willThrow(new RuleConditionException("Unbekannter Operator: 'foo'"))
                .given(ruleService).proposeRule(any(), any());

        String body = """
                {
                  "ruleKey": "k-defekt",
                  "name": "Regel defekt",
                  "proposedSeverity": "LOW",
                  "conditionJson": "{\\"foo\\":{\\"path\\":\\"cve.kev\\",\\"value\\":true}}",
                  "rationaleTemplate": "x",
                  "createdBy": "a.admin@ahs.test"
                }
                """;

        mockMvc.perform(post("/api/v1/rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("rule_condition_invalid"));
    }

    @Test
    @DisplayName("POST /rules/{id}/activate: 409 bei Vier-Augen-Verstoss")
    void aktivierungVierAugenVerstoss() throws Exception {
        willThrow(new RuleFourEyesViolationException("Approver identisch"))
                .given(ruleService).activate(eq(RULE_ID), eq("t.tester@ahs.test"));

        mockMvc.perform(post("/api/v1/rules/{id}/activate", RULE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approverId\":\"t.tester@ahs.test\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("four_eyes_violation"));
    }

    @Test
    @DisplayName("POST /rules/{id}/dry-run: liefert Coverage und leere Konfliktliste")
    void dryRunHappyPath() throws Exception {
        given(ruleService.find(RULE_ID)).willReturn(Optional.of(neueView(RuleStatus.ACTIVE)));
        given(dryRunService.dryRun(eq(RULE_ID), eq(180)))
                .willReturn(new DryRunResult(
                        RULE_ID,
                        Instant.now().minusSeconds(3600),
                        Instant.now(),
                        20, 14, 0, List.of()));

        mockMvc.perform(post("/api/v1/rules/{id}/dry-run", RULE_ID).param("days", "180"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalFindings").value(20))
                .andExpect(jsonPath("$.matchedFindings").value(14))
                .andExpect(jsonPath("$.conflicts").isArray());
    }

    private RuleView neueView(RuleStatus status) {
        return new RuleView(
                RULE_ID,
                "k-1",
                "Regel 1",
                "Beschreibung",
                status,
                RuleOrigin.MANUAL,
                AhsSeverity.LOW,
                "{\"eq\":{\"path\":\"cve.kev\",\"value\":true}}",
                "CVE {cve.id}",
                List.of(),
                "a.admin@ahs.test",
                status == RuleStatus.ACTIVE ? "o.other@ahs.test" : null,
                status == RuleStatus.ACTIVE ? Instant.now() : null,
                Instant.now());
    }
}
