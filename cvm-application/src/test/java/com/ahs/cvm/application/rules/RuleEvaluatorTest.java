package com.ahs.cvm.application.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RuleEvaluatorTest {

    private final ConditionParser parser = new ConditionParser();
    private final RuleEvaluator evaluator = new RuleEvaluator();
    private final YAMLMapper yaml = new YAMLMapper();

    private RuleEvaluationContext kontext() throws Exception {
        JsonNode profile = yaml.readTree(
                """
                schemaVersion: 1
                umgebung:
                  key: REF-TEST
                  stage: REF
                architecture:
                  windows_hosts: false
                  linux_hosts: true
                network:
                  internet_exposure: false
                """);
        return new RuleEvaluationContext(
                new RuleEvaluationContext.CveSnapshot(
                        UUID.randomUUID(),
                        "CVE-2017-18640",
                        "SnakeYAML billion laughs",
                        List.of("CWE-776"),
                        true,
                        new BigDecimal("0.8123"),
                        new BigDecimal("7.5")),
                profile,
                new RuleEvaluationContext.ComponentSnapshot(
                        "maven", "snakeyaml", "1.19"),
                new RuleEvaluationContext.FindingSnapshot(
                        UUID.randomUUID(), Instant.now()));
    }

    @Test
    @DisplayName("Evaluator: eq boolean true matcht kev=true im Kontext")
    void eqBoolean() throws Exception {
        ConditionNode node = parser.parse(
                "{\"eq\": {\"path\": \"cve.kev\", \"value\": true}}");
        assertThat(evaluator.evaluate(node, kontext())).isTrue();
    }

    @Test
    @DisplayName("Evaluator: gt auf epss liefert true fuer 0.8 > 0.5")
    void gtNumerisch() throws Exception {
        ConditionNode node = parser.parse(
                "{\"gt\": {\"path\": \"cve.epss\", \"value\": 0.5}}");
        assertThat(evaluator.evaluate(node, kontext())).isTrue();
    }

    @Test
    @DisplayName("Evaluator: between inklusiv, 0.5 in [0.1, 0.9]")
    void betweenInklusiv() throws Exception {
        ConditionNode node = parser.parse(
                "{\"between\": {\"path\": \"cve.epss\", \"value\": [0.1, 0.9]}}");
        assertThat(evaluator.evaluate(node, kontext())).isTrue();
    }

    @Test
    @DisplayName("Evaluator: containsAny mit Treffer in cwes")
    void containsAnyTreffer() throws Exception {
        ConditionNode node = parser.parse(
                "{\"containsAny\": {\"path\": \"cve.cwes\", \"value\": [\"CWE-776\", \"CWE-79\"]}}");
        assertThat(evaluator.evaluate(node, kontext())).isTrue();
    }

    @Test
    @DisplayName("Evaluator: matches Regex auf Beschreibung")
    void matchesRegex() throws Exception {
        ConditionNode node = parser.parse(
                "{\"matches\": {\"path\": \"cve.description\", \"value\": \"(?i)billion.*laughs\"}}");
        assertThat(evaluator.evaluate(node, kontext())).isTrue();
    }

    @Test
    @DisplayName("Evaluator: profile-Pfad greift auf verschachtelte YAML-Struktur zu")
    void profilPfad() throws Exception {
        ConditionNode node = parser.parse(
                "{\"eq\": {\"path\": \"profile.architecture.windows_hosts\", \"value\": false}}");
        assertThat(evaluator.evaluate(node, kontext())).isTrue();
    }

    @Test
    @DisplayName("Evaluator: not invertiert das Kindergebnis")
    void notInvertiert() throws Exception {
        ConditionNode node = parser.parse(
                "{\"not\": {\"eq\": {\"path\": \"cve.kev\", \"value\": false}}}");
        assertThat(evaluator.evaluate(node, kontext())).isTrue();
    }

    @Test
    @DisplayName("Evaluator: all braucht jeden Kind-Treffer")
    void allKombiniertKonjunktiv() throws Exception {
        ConditionNode node = parser.parse(
                """
                { "all": [
                  { "eq": { "path": "cve.kev", "value": true } },
                  { "eq": { "path": "profile.architecture.windows_hosts", "value": false } }
                ]}
                """);
        assertThat(evaluator.evaluate(node, kontext())).isTrue();
    }

    @Test
    @DisplayName("Evaluator: any reicht ein Treffer")
    void anyKombiniertDisjunktiv() throws Exception {
        ConditionNode node = parser.parse(
                """
                { "any": [
                  { "eq": { "path": "cve.kev", "value": false } },
                  { "gt": { "path": "cve.epss", "value": 0.99 } },
                  { "eq": { "path": "cve.kev", "value": true } }
                ]}
                """);
        assertThat(evaluator.evaluate(node, kontext())).isTrue();
    }

    @Test
    @DisplayName("Evaluator: fehlendes Feld liefert false statt NullPointerException")
    void fehlendesFeldLiefertFalse() throws Exception {
        ConditionNode node = parser.parse(
                "{\"eq\": {\"path\": \"profile.compliance.frameworks\", \"value\": [\"ISO27001\"]}}");
        assertThat(evaluator.evaluate(node, kontext())).isFalse();
    }

    @Test
    @DisplayName("Evaluator: Typ-Mismatch (gt auf String) wirft RuleConditionException")
    void typMismatch() throws Exception {
        ConditionNode node = parser.parse(
                "{\"gt\": {\"path\": \"cve.description\", \"value\": 1}}");
        assertThatThrownBy(() -> evaluator.evaluate(node, kontext()))
                .isInstanceOf(RuleConditionException.class)
                .hasMessageContaining("gt");
    }
}
