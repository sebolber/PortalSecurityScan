package com.ahs.cvm.application.cascade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;

import com.ahs.cvm.application.assessment.AssessmentLookupService;
import com.ahs.cvm.application.rules.RuleEngine;
import com.ahs.cvm.application.rules.RuleEvaluationContext;
import com.ahs.cvm.application.rules.RuleEvaluationContext.ComponentSnapshot;
import com.ahs.cvm.application.rules.RuleEvaluationContext.CveSnapshot;
import com.ahs.cvm.application.rules.RuleEvaluationContext.FindingSnapshot;
import com.ahs.cvm.domain.enums.AhsSeverity;
import com.ahs.cvm.domain.enums.AssessmentStatus;
import com.ahs.cvm.domain.enums.ProposalSource;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CascadeServiceAiStageTest {

    @Test
    @DisplayName("Cascade: AI-Port liefert Outcome - wird durchgereicht")
    void aiPortLiefert() {
        AssessmentLookupService lookup = mock(AssessmentLookupService.class);
        RuleEngine engine = mock(RuleEngine.class);
        AiAssessmentSuggesterPort port = mock(AiAssessmentSuggesterPort.class);
        UUID suggestionId = UUID.randomUUID();
        given(lookup.findeAktiveFreigaben(any(), any(), any())).willReturn(List.of());
        given(engine.evaluate(any())).willReturn(Optional.empty());
        given(port.suggest(any())).willReturn(Optional.of(CascadeOutcome.ai(
                suggestionId, AhsSeverity.HIGH, "Begruendung",
                BigDecimal.valueOf(0.9), List.of("a.b"),
                AssessmentStatus.PROPOSED)));

        CascadeService service = new CascadeService(lookup, engine, Optional.of(port));
        CascadeOutcome out = service.bewerte(input());

        assertThat(out.source()).isEqualTo(ProposalSource.AI_SUGGESTION);
        assertThat(out.aiSuggestionId()).isEqualTo(suggestionId);
        assertThat(out.targetStatus()).isEqualTo(AssessmentStatus.PROPOSED);
    }

    @Test
    @DisplayName("Cascade: AI-Port liefert empty -> MANUAL")
    void aiPortEmpty() {
        AssessmentLookupService lookup = mock(AssessmentLookupService.class);
        RuleEngine engine = mock(RuleEngine.class);
        AiAssessmentSuggesterPort port = mock(AiAssessmentSuggesterPort.class);
        given(lookup.findeAktiveFreigaben(any(), any(), any())).willReturn(List.of());
        given(engine.evaluate(any())).willReturn(Optional.empty());
        given(port.suggest(any())).willReturn(Optional.empty());

        CascadeService service = new CascadeService(lookup, engine, Optional.of(port));
        CascadeOutcome out = service.bewerte(input());

        assertThat(out.source()).isEqualTo(ProposalSource.HUMAN);
    }

    @Test
    @DisplayName("Cascade: AI-Port wirft Exception -> MANUAL (kein Bubble-Up)")
    void aiPortFehler() {
        AssessmentLookupService lookup = mock(AssessmentLookupService.class);
        RuleEngine engine = mock(RuleEngine.class);
        AiAssessmentSuggesterPort port = mock(AiAssessmentSuggesterPort.class);
        given(lookup.findeAktiveFreigaben(any(), any(), any())).willReturn(List.of());
        given(engine.evaluate(any())).willReturn(Optional.empty());
        willThrow(new RuntimeException("kaputt")).given(port).suggest(any());

        CascadeService service = new CascadeService(lookup, engine, Optional.of(port));
        CascadeOutcome out = service.bewerte(input());

        assertThat(out.source()).isEqualTo(ProposalSource.HUMAN);
    }

    private CascadeInput input() {
        UUID cveId = UUID.randomUUID();
        CveSnapshot cve = new CveSnapshot(
                cveId, "CVE-X", "summary", List.of(), false,
                BigDecimal.ZERO, BigDecimal.valueOf(7.5));
        ComponentSnapshot comp = new ComponentSnapshot("maven", "x", "1");
        FindingSnapshot fs = new FindingSnapshot(UUID.randomUUID(), Instant.now());
        RuleEvaluationContext ctx = new RuleEvaluationContext(
                cve, JsonNodeFactory.instance.objectNode(), comp, fs);
        return new CascadeInput(cveId, UUID.randomUUID(), UUID.randomUUID(), ctx);
    }
}
