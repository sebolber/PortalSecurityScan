package com.ahs.cvm.application.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ConditionParserTest {

    private final ConditionParser parser = new ConditionParser();

    @Test
    @DisplayName("Parser: einfache eq-Bedingung erzeugt OpEq-Knoten")
    void parseEq() {
        ConditionNode node = parser.parse(
                "{\"eq\": {\"path\": \"cve.kev\", \"value\": true}}");

        assertThat(node).isInstanceOf(ConditionNode.Comparison.class);
        ConditionNode.Comparison op = (ConditionNode.Comparison) node;
        assertThat(op.operator()).isEqualTo("eq");
        assertThat(op.path()).isEqualTo("cve.kev");
    }

    @Test
    @DisplayName("Parser: alle skalaren Operatoren werden akzeptiert")
    void parseAlleSkalarOps() {
        String[] ops = {"eq", "ne", "gt", "lt", "matches", "in", "containsAny", "between"};
        for (String op : ops) {
            String json = switch (op) {
                case "in", "containsAny" -> "{\"" + op + "\": {\"path\": \"cve.cwes\", \"value\": [\"CWE-79\"]}}";
                case "between" -> "{\"" + op + "\": {\"path\": \"cve.epss\", \"value\": [0.1, 0.5]}}";
                default -> "{\"" + op + "\": {\"path\": \"cve.epss\", \"value\": 0.5}}";
            };
            ConditionNode node = parser.parse(json);
            assertThat(node)
                    .as("Operator " + op)
                    .isInstanceOf(ConditionNode.Comparison.class);
            assertThat(((ConditionNode.Comparison) node).operator()).isEqualTo(op);
        }
    }

    @Test
    @DisplayName("Parser: verschachtelte all/any/not werden korrekt aufgebaut")
    void parseVerschachtelt() {
        String json = """
                {
                  "all": [
                    { "eq": { "path": "cve.kev", "value": true } },
                    { "not": { "eq": { "path": "profile.architecture.windows_hosts", "value": true } } },
                    { "any": [
                      { "containsAny": { "path": "cve.cwes", "value": ["CWE-79"] } },
                      { "gt": { "path": "cve.epss", "value": 0.5 } }
                    ]}
                  ]
                }
                """;

        ConditionNode node = parser.parse(json);

        assertThat(node).isInstanceOf(ConditionNode.LogicAll.class);
        ConditionNode.LogicAll all = (ConditionNode.LogicAll) node;
        assertThat(all.children()).hasSize(3);
        assertThat(all.children().get(0)).isInstanceOf(ConditionNode.Comparison.class);
        assertThat(all.children().get(1)).isInstanceOf(ConditionNode.LogicNot.class);
        assertThat(all.children().get(2)).isInstanceOf(ConditionNode.LogicAny.class);
    }

    @Test
    @DisplayName("Parser: unbekannter Pfad-Praefix wird mit deutscher Meldung abgelehnt")
    void unbekannterPfad() {
        assertThatThrownBy(() -> parser.parse(
                "{\"eq\": {\"path\": \"foo.bar\", \"value\": 1}}"))
                .isInstanceOf(RuleConditionException.class)
                .hasMessageContaining("foo.bar");
    }

    @Test
    @DisplayName("Parser: unbekannter Operator wird abgelehnt")
    void unbekannterOperator() {
        assertThatThrownBy(() -> parser.parse(
                "{\"startsWith\": {\"path\": \"cve.description\", \"value\": \"X\"}}"))
                .isInstanceOf(RuleConditionException.class)
                .hasMessageContaining("startsWith");
    }

    @Test
    @DisplayName("Parser: leeres all-Array wird abgelehnt")
    void leeresAll() {
        assertThatThrownBy(() -> parser.parse("{\"all\": []}"))
                .isInstanceOf(RuleConditionException.class)
                .hasMessageContaining("all");
    }

    @Test
    @DisplayName("Parser: between erwartet genau zwei Werte")
    void betweenFalschWerte() {
        assertThatThrownBy(() -> parser.parse(
                "{\"between\": {\"path\": \"cve.epss\", \"value\": [0.1]}}"))
                .isInstanceOf(RuleConditionException.class)
                .hasMessageContaining("between");
    }
}
