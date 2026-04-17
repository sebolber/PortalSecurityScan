package com.ahs.cvm.application.rules;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Ersetzt Tokens der Form {@code {path}} in einem Rationale-Template durch
 * konkrete Werte aus dem {@link RuleEvaluationContext}. Unbekannte Tokens
 * bleiben unveraendert und sind so im Audit-Log nachvollziehbar.
 */
@Component
public class RationaleTemplateInterpolator {

    private static final Pattern TOKEN = Pattern.compile("\\{([^}]+)}");

    public String interpolate(String template, RuleEvaluationContext ctx) {
        if (template == null || template.isEmpty()) return template;
        Matcher m = TOKEN.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String pfad = m.group(1);
            String wert = werteAuf(pfad, ctx);
            m.appendReplacement(sb, Matcher.quoteReplacement(wert));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private String werteAuf(String pfad, RuleEvaluationContext ctx) {
        try {
            JsonNode knoten = PathResolver.resolve(pfad, ctx);
            if (knoten == null || knoten.isMissingNode() || knoten.isNull()) {
                return "{" + pfad + "}";
            }
            if (knoten.isTextual()) return knoten.asText();
            if (knoten.isValueNode()) return knoten.asText();
            return knoten.toString();
        } catch (RuleConditionException e) {
            return "{" + pfad + "}";
        }
    }
}
