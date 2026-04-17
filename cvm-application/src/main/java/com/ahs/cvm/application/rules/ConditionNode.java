package com.ahs.cvm.application.rules;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

/**
 * AST einer Regel-Condition. Wird vom {@link ConditionParser} gebaut und vom
 * {@link RuleEvaluator} ausgewertet.
 */
public sealed interface ConditionNode
        permits ConditionNode.LogicAll,
                ConditionNode.LogicAny,
                ConditionNode.LogicNot,
                ConditionNode.Comparison {

    record LogicAll(List<ConditionNode> children) implements ConditionNode {}

    record LogicAny(List<ConditionNode> children) implements ConditionNode {}

    record LogicNot(ConditionNode child) implements ConditionNode {}

    /**
     * Binaere Vergleichsoperation. {@code operator} ist einer aus
     * {@code eq, ne, gt, lt, matches, in, containsAny, between}.
     */
    record Comparison(String operator, String path, JsonNode value) implements ConditionNode {}
}
