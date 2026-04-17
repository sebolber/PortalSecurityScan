package com.ahs.cvm.application.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.ahs.cvm.domain.enums.AhsSeverity;
import com.ahs.cvm.domain.enums.RuleStatus;
import com.ahs.cvm.persistence.rule.Rule;
import com.ahs.cvm.persistence.rule.RuleRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RuleEngineTest {

    private final RuleRepository ruleRepository = mock(RuleRepository.class);
    private final ConditionParser parser = new ConditionParser();
    private final RuleEvaluator evaluator = new RuleEvaluator();
    private final RationaleTemplateInterpolator interpolator =
            new RationaleTemplateInterpolator();
    private final RuleEngine engine =
            new RuleEngine(ruleRepository, parser, evaluator, interpolator);

    private RuleEvaluationContext kontext() throws Exception {
        JsonNode profile = new YAMLMapper().readTree(
                """
                schemaVersion: 1
                umgebung: {key: REF-TEST, stage: REF}
                architecture:
                  windows_hosts: false
                """);
        return new RuleEvaluationContext(
                new RuleEvaluationContext.CveSnapshot(
                        UUID.randomUUID(),
                        "CVE-2017-18640",
                        "SnakeYAML billion laughs",
                        List.of("CWE-776"),
                        true,
                        new BigDecimal("0.8"),
                        new BigDecimal("7.5")),
                profile,
                new RuleEvaluationContext.ComponentSnapshot(
                        "maven", "snakeyaml", "1.19"),
                new RuleEvaluationContext.FindingSnapshot(
                        UUID.randomUUID(), Instant.now()));
    }

    @Test
    @DisplayName("RuleEngine: erste zutreffende ACTIVE-Regel erzeugt ProposedResult")
    void erstePassendeRegelGewinnt() throws Exception {
        Rule trefferRegel = Rule.builder()
                .id(UUID.randomUUID())
                .ruleKey("nicht-windows-only")
                .name("Nur auf Windows relevant")
                .status(RuleStatus.ACTIVE)
                .proposedSeverity(AhsSeverity.LOW)
                .conditionJson(
                        "{\"eq\": {\"path\": \"profile.architecture.windows_hosts\", \"value\": false}}")
                .rationaleTemplate("CVE {cve.id} ist nur auf Windows relevant")
                .rationaleSourceFields(List.of("profile.architecture.windows_hosts"))
                .build();
        Rule andereRegel = Rule.builder()
                .id(UUID.randomUUID())
                .ruleKey("keine-kev")
                .status(RuleStatus.ACTIVE)
                .proposedSeverity(AhsSeverity.MEDIUM)
                .conditionJson("{\"eq\": {\"path\": \"cve.kev\", \"value\": false}}")
                .rationaleTemplate("kein KEV")
                .build();

        given(ruleRepository.findByStatusOrderByCreatedAtDesc(RuleStatus.ACTIVE))
                .willReturn(List.of(trefferRegel, andereRegel));

        Optional<ProposedResult> ergebnis = engine.evaluate(kontext());

        assertThat(ergebnis).isPresent();
        ProposedResult r = ergebnis.orElseThrow();
        assertThat(r.ruleKey()).isEqualTo("nicht-windows-only");
        assertThat(r.severity()).isEqualTo(AhsSeverity.LOW);
        assertThat(r.rationale()).contains("CVE-2017-18640");
        assertThat(r.sourceFields()).containsExactly("profile.architecture.windows_hosts");
    }

    @Test
    @DisplayName("RuleEngine: keine ACTIVE-Regeln, leerer Optional")
    void keineRegeln() throws Exception {
        given(ruleRepository.findByStatusOrderByCreatedAtDesc(RuleStatus.ACTIVE))
                .willReturn(List.of());

        assertThat(engine.evaluate(kontext())).isEmpty();
    }

    @Test
    @DisplayName("RuleEngine: Regel mit defektem JSON liefert RuleConditionException")
    void defekteRegelBrichtAb() throws Exception {
        Rule defekt = Rule.builder()
                .id(UUID.randomUUID())
                .ruleKey("defekt")
                .status(RuleStatus.ACTIVE)
                .proposedSeverity(AhsSeverity.MEDIUM)
                .conditionJson("{\"eq\": {\"path\": \"foo.bar\", \"value\": 1}}")
                .rationaleTemplate("x")
                .build();
        given(ruleRepository.findByStatusOrderByCreatedAtDesc(RuleStatus.ACTIVE))
                .willReturn(List.of(defekt));

        assertThatThrownBy(() -> engine.evaluate(kontext()))
                .isInstanceOf(RuleConditionException.class);
    }
}
