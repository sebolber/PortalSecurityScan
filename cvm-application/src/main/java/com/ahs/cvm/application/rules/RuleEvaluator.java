package com.ahs.cvm.application.rules;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.springframework.stereotype.Component;

/**
 * Werte-basierte Auswertung eines {@link ConditionNode}. Die Zugriffe auf
 * Kontextdaten laufen ueber {@link PathResolver}; somit bleibt der
 * Evaluator unabhaengig von konkreten Domain-Typen.
 */
@Component
public class RuleEvaluator {

    public boolean evaluate(ConditionNode node, RuleEvaluationContext ctx) {
        return switch (node) {
            case ConditionNode.LogicAll all -> all.children().stream()
                    .allMatch(k -> evaluate(k, ctx));
            case ConditionNode.LogicAny any -> any.children().stream()
                    .anyMatch(k -> evaluate(k, ctx));
            case ConditionNode.LogicNot not -> !evaluate(not.child(), ctx);
            case ConditionNode.Comparison cmp -> vergleiche(cmp, ctx);
        };
    }

    private boolean vergleiche(ConditionNode.Comparison cmp, RuleEvaluationContext ctx) {
        JsonNode actual = PathResolver.resolve(cmp.path(), ctx);
        JsonNode expected = cmp.value();

        return switch (cmp.operator()) {
            case "eq" -> gleich(actual, expected);
            case "ne" -> !gleich(actual, expected);
            case "gt" -> numerisch(cmp, actual, expected, true);
            case "lt" -> numerisch(cmp, actual, expected, false);
            case "between" -> between(cmp, actual, expected);
            case "in" -> inListe(expected, actual);
            case "containsAny" -> containsAny(actual, expected);
            case "matches" -> matches(cmp, actual, expected);
            default -> throw new RuleConditionException(
                    "Operator '" + cmp.operator() + "' ist im Evaluator nicht behandelt.");
        };
    }

    private boolean gleich(JsonNode actual, JsonNode expected) {
        if (istAbwesend(actual) && istAbwesend(expected)) return true;
        if (istAbwesend(actual) || istAbwesend(expected)) return false;
        if (actual.isNumber() && expected.isNumber()) {
            return actual.decimalValue().compareTo(expected.decimalValue()) == 0;
        }
        return actual.equals(expected);
    }

    private boolean numerisch(
            ConditionNode.Comparison cmp, JsonNode actual, JsonNode expected, boolean greater) {
        if (istAbwesend(actual)) return false;
        if (!actual.isNumber() || !expected.isNumber()) {
            throw new RuleConditionException(
                    "Operator '" + cmp.operator() + "' verlangt numerische Werte (Pfad '"
                            + cmp.path() + "').");
        }
        int cmpResult = actual.decimalValue().compareTo(expected.decimalValue());
        return greater ? cmpResult > 0 : cmpResult < 0;
    }

    private boolean between(
            ConditionNode.Comparison cmp, JsonNode actual, JsonNode expected) {
        if (istAbwesend(actual)) return false;
        if (!actual.isNumber()) {
            throw new RuleConditionException(
                    "Operator 'between' verlangt einen numerischen Wert am Pfad '"
                            + cmp.path() + "'.");
        }
        BigDecimal low = expected.get(0).decimalValue();
        BigDecimal high = expected.get(1).decimalValue();
        BigDecimal v = actual.decimalValue();
        return v.compareTo(low) >= 0 && v.compareTo(high) <= 0;
    }

    private boolean inListe(JsonNode expectedArray, JsonNode actual) {
        if (istAbwesend(actual)) return false;
        for (JsonNode elem : expectedArray) {
            if (gleich(actual, elem)) return true;
        }
        return false;
    }

    private boolean containsAny(JsonNode actual, JsonNode expectedArray) {
        if (istAbwesend(actual) || !actual.isArray()) return false;
        Set<String> vorhanden = new HashSet<>();
        for (JsonNode elem : actual) {
            vorhanden.add(elem.isTextual() ? elem.asText() : elem.toString());
        }
        for (JsonNode elem : expectedArray) {
            String s = elem.isTextual() ? elem.asText() : elem.toString();
            if (vorhanden.contains(s)) return true;
        }
        return false;
    }

    private boolean matches(
            ConditionNode.Comparison cmp, JsonNode actual, JsonNode expected) {
        if (istAbwesend(actual)) return false;
        if (!expected.isTextual()) {
            throw new RuleConditionException(
                    "Operator 'matches' verlangt einen String-Regex (Pfad '"
                            + cmp.path() + "').");
        }
        if (!actual.isTextual()) {
            throw new RuleConditionException(
                    "Operator 'matches' verlangt einen String-Wert am Pfad '"
                            + cmp.path() + "'.");
        }
        try {
            return Pattern.compile(expected.asText()).matcher(actual.asText()).find();
        } catch (PatternSyntaxException e) {
            throw new RuleConditionException(
                    "Regex ungueltig: " + e.getMessage(), e);
        }
    }

    private boolean istAbwesend(JsonNode node) {
        return node == null || node.isMissingNode() || node.isNull();
    }
}
