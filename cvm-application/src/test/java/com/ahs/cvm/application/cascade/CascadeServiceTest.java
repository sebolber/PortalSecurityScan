package com.ahs.cvm.application.cascade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.ahs.cvm.application.assessment.AssessmentLookupService;
import com.ahs.cvm.application.rules.ProposedResult;
import com.ahs.cvm.application.rules.RuleEngine;
import com.ahs.cvm.application.rules.RuleEvaluationContext;
import com.ahs.cvm.domain.enums.AhsSeverity;
import com.ahs.cvm.domain.enums.ProposalSource;
import com.ahs.cvm.persistence.assessment.Assessment;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CascadeServiceTest {

    private AssessmentLookupService lookupService;
    private RuleEngine ruleEngine;
    private CascadeService cascadeService;

    @BeforeEach
    void setUp() {
        lookupService = mock(AssessmentLookupService.class);
        ruleEngine = mock(RuleEngine.class);
        cascadeService = new CascadeService(lookupService, ruleEngine, java.util.Optional.empty());
    }

    @Test
    @DisplayName("Cascade: bei REUSE-Treffer wird Regel-Engine nicht mehr angefragt")
    void reuseKurzschluss() {
        UUID cveId = UUID.randomUUID();
        UUID produktVersionId = UUID.randomUUID();
        UUID umgebungId = UUID.randomUUID();

        Assessment bestehend = Assessment.builder()
                .severity(AhsSeverity.MEDIUM)
                .rationale("bereits bewertet")
                .build();
        given(lookupService.findeAktiveFreigaben(cveId, produktVersionId, umgebungId))
                .willReturn(List.of(bestehend));

        CascadeOutcome outcome = cascadeService.bewerte(
                kontext(cveId, produktVersionId, umgebungId));

        assertThat(outcome.source()).isEqualTo(ProposalSource.REUSE);
        assertThat(outcome.severity()).isEqualTo(AhsSeverity.MEDIUM);
        verify(ruleEngine, never()).evaluate(any());
    }

    @Test
    @DisplayName("Cascade: ohne REUSE-Treffer wird Regel-Engine angefragt und liefert RULE")
    void ruleWennKeinReuse() {
        UUID cveId = UUID.randomUUID();
        UUID pv = UUID.randomUUID();
        UUID env = UUID.randomUUID();
        given(lookupService.findeAktiveFreigaben(cveId, pv, env)).willReturn(List.of());
        given(ruleEngine.evaluate(any()))
                .willReturn(Optional.of(new ProposedResult(
                        UUID.randomUUID(),
                        "nicht-windows",
                        AhsSeverity.LOW,
                        "keine Windows-Plattform im Einsatz",
                        List.of("profile.architecture.windows_hosts"))));

        CascadeOutcome outcome = cascadeService.bewerte(kontext(cveId, pv, env));

        assertThat(outcome.source()).isEqualTo(ProposalSource.RULE);
        assertThat(outcome.severity()).isEqualTo(AhsSeverity.LOW);
        assertThat(outcome.rationale()).contains("Windows");
    }

    @Test
    @DisplayName("Cascade: kein Treffer liefert MANUAL-Outcome (leer, Mensch entscheidet)")
    void manualAlsFallback() {
        UUID cveId = UUID.randomUUID();
        UUID pv = UUID.randomUUID();
        UUID env = UUID.randomUUID();
        given(lookupService.findeAktiveFreigaben(cveId, pv, env)).willReturn(List.of());
        given(ruleEngine.evaluate(any())).willReturn(Optional.empty());

        CascadeOutcome outcome = cascadeService.bewerte(kontext(cveId, pv, env));

        assertThat(outcome.source()).isEqualTo(ProposalSource.HUMAN);
        assertThat(outcome.severity()).isNull();
    }

    private CascadeInput kontext(UUID cveId, UUID pv, UUID env) {
        ObjectNode profile = new ObjectMapper().createObjectNode();
        profile.putObject("architecture").put("windows_hosts", false);
        RuleEvaluationContext evalCtx = new RuleEvaluationContext(
                new RuleEvaluationContext.CveSnapshot(
                        cveId, "CVE-2017-18640", "dummy", List.of(), true,
                        new BigDecimal("0.1"), new BigDecimal("5.0")),
                profile,
                new RuleEvaluationContext.ComponentSnapshot("maven", "x", "1.0"),
                new RuleEvaluationContext.FindingSnapshot(UUID.randomUUID(), Instant.now()));
        return new CascadeInput(cveId, pv, env, evalCtx);
    }
}
