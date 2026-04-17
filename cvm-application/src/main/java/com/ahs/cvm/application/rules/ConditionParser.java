package com.ahs.cvm.application.rules;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Baut aus der JSON-DSL einer Regel einen {@link ConditionNode}-Baum. Die
 * Pfad-Praefixe werden hier bereits streng geprueft, damit spaeter im
 * {@link RuleEvaluator} keine NullPointer-Fallen entstehen.
 */
@Component
public class ConditionParser {

    private static final Set<String> SKALAR_OPS = Set.of(
            "eq", "ne", "gt", "lt", "matches", "in", "containsAny", "between");

    private static final Set<String> LOGIK_OPS = Set.of("all", "any", "not");

    private static final Set<String> PFAD_PREFIXE = Set.of(
            "cve.", "profile.", "component.");

    private final ObjectMapper mapper = new ObjectMapper();

    public ConditionNode parse(String json) {
        if (json == null || json.isBlank()) {
            throw new RuleConditionException("Condition-JSON ist leer.");
        }
        try {
            return parseTree(mapper.readTree(json));
        } catch (JsonProcessingException e) {
            throw new RuleConditionException(
                    "Condition-JSON ist kein gueltiges JSON: " + e.getOriginalMessage(), e);
        }
    }

    public ConditionNode parseTree(JsonNode node) {
        if (node == null || !node.isObject()) {
            throw new RuleConditionException(
                    "Condition-Knoten muss ein JSON-Objekt sein, war: "
                            + (node == null ? "null" : node.getNodeType()));
        }
        if (node.size() != 1) {
            throw new RuleConditionException(
                    "Condition-Knoten muss genau einen Operator enthalten, hat: " + node.size());
        }
        Map.Entry<String, JsonNode> entry = node.fields().next();
        String op = entry.getKey();
        JsonNode body = entry.getValue();

        if (LOGIK_OPS.contains(op)) {
            return parseLogik(op, body);
        }
        if (SKALAR_OPS.contains(op)) {
            return parseVergleich(op, body);
        }
        throw new RuleConditionException("Unbekannter Operator: '" + op + "'.");
    }

    private ConditionNode parseLogik(String op, JsonNode body) {
        if ("not".equals(op)) {
            if (body == null || !body.isObject()) {
                throw new RuleConditionException("Operator 'not' erwartet ein Objekt.");
            }
            return new ConditionNode.LogicNot(parseTree(body));
        }
        if (body == null || !body.isArray()) {
            throw new RuleConditionException(
                    "Operator '" + op + "' erwartet ein Array von Bedingungen.");
        }
        if (body.isEmpty()) {
            throw new RuleConditionException(
                    "Operator '" + op + "' darf nicht leer sein.");
        }
        List<ConditionNode> kinder = new ArrayList<>();
        for (JsonNode kind : body) {
            kinder.add(parseTree(kind));
        }
        return "all".equals(op)
                ? new ConditionNode.LogicAll(List.copyOf(kinder))
                : new ConditionNode.LogicAny(List.copyOf(kinder));
    }

    private ConditionNode parseVergleich(String op, JsonNode body) {
        if (body == null || !body.isObject()) {
            throw new RuleConditionException(
                    "Operator '" + op + "' erwartet ein Objekt mit 'path' und 'value'.");
        }
        JsonNode pathNode = body.get("path");
        JsonNode valueNode = body.get("value");
        if (pathNode == null || !pathNode.isTextual()) {
            throw new RuleConditionException(
                    "Operator '" + op + "' braucht ein Textfeld 'path'.");
        }
        if (valueNode == null) {
            throw new RuleConditionException(
                    "Operator '" + op + "' braucht ein Feld 'value'.");
        }
        String path = pathNode.asText();
        pruefePfad(path);

        if ("between".equals(op)) {
            if (!valueNode.isArray() || valueNode.size() != 2) {
                throw new RuleConditionException(
                        "Operator 'between' erwartet genau zwei Werte [low, high].");
            }
        }
        if ("in".equals(op) || "containsAny".equals(op)) {
            if (!valueNode.isArray()) {
                throw new RuleConditionException(
                        "Operator '" + op + "' erwartet ein Array als 'value'.");
            }
        }
        return new ConditionNode.Comparison(op, path, valueNode);
    }

    private void pruefePfad(String path) {
        for (String praefix : PFAD_PREFIXE) {
            if (path.startsWith(praefix) && path.length() > praefix.length()) {
                return;
            }
        }
        throw new RuleConditionException(
                "Ungueltiger Pfad '" + path + "'. Erlaubt sind Praefixe "
                        + PFAD_PREFIXE + ".");
    }
}
