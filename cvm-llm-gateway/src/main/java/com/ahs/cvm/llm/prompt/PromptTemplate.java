package com.ahs.cvm.llm.prompt;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Prompt-Template mit Id, Version, System- und User-Teil.
 * Substitution ueber einfache {@code ${variable}}-Syntax. Eine
 * fehlende Variable wirft {@link IllegalArgumentException}, damit
 * keine leeren Prompts an das Modell gehen.
 */
public record PromptTemplate(
        String id,
        String version,
        String systemPrompt,
        String userTemplate) {

    private static final Pattern VARIABLE = Pattern.compile("\\$\\{\\s*(\\w+)\\s*}");

    public String renderUser(Map<String, ?> variablen) {
        return render(userTemplate, variablen);
    }

    public String renderSystem(Map<String, ?> variablen) {
        return render(systemPrompt, variablen);
    }

    private static String render(String template, Map<String, ?> variablen) {
        if (template == null) {
            throw new IllegalArgumentException("Template darf nicht null sein.");
        }
        Map<String, ?> sicher = variablen == null ? Map.of() : variablen;
        Matcher matcher = VARIABLE.matcher(template);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1);
            if (!sicher.containsKey(key)) {
                throw new IllegalArgumentException(
                        "Template-Variable fehlt: " + key);
            }
            Object wert = sicher.get(key);
            matcher.appendReplacement(out, Matcher.quoteReplacement(
                    wert == null ? "" : wert.toString()));
        }
        matcher.appendTail(out);
        return out.toString();
    }
}
