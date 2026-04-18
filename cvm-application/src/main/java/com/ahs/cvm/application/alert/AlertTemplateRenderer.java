package com.ahs.cvm.application.alert;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Sehr schmaler Template-Renderer. Templates liegen unter
 * {@code classpath:cvm/alerts/<templateName>.html} und enthalten
 * Platzhalter {@code {{ key }}}. Werte werden HTML-escaped
 * eingesetzt.
 *
 * <p>Bewusst ohne Thymeleaf, um die Application-Schicht schlank zu
 * halten. Dynamische Logik (if/each) wird hier nicht benoetigt; das
 * fachliche Template ist statisch und dient nur als Mail-Vorlage.
 */
@Component
public class AlertTemplateRenderer {

    private static final Pattern PLATZHALTER = Pattern.compile("\\{\\{\\s*(\\w+)\\s*\\}\\}");

    public String render(String templateName, Map<String, Object> daten) {
        String quelle = ladeTemplate(templateName);
        Map<String, Object> sicher = daten == null ? Map.of() : daten;
        Matcher matcher = PLATZHALTER.matcher(quelle);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1);
            Object wert = sicher.get(key);
            String text = wert == null ? "" : escape(String.valueOf(wert));
            matcher.appendReplacement(result, Matcher.quoteReplacement(text));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    /** Reduziert HTML auf einen Plain-Text-Body fuer Mail-Fallback. */
    public String toPlainText(String html) {
        if (html == null) {
            return "";
        }
        String ohneTags = html
                .replaceAll("<br\\s*/?>", "\n")
                .replaceAll("</p>", "\n\n")
                .replaceAll("<[^>]+>", "");
        return ohneTags
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replaceAll("\\s+\n", "\n")
                .trim();
    }

    private String ladeTemplate(String templateName) {
        String pfad = "cvm/alerts/" + templateName + ".html";
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(pfad)) {
            if (in == null) {
                throw new IllegalArgumentException(
                        "Alert-Template nicht gefunden: " + pfad);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException(
                    "Alert-Template konnte nicht gelesen werden: " + pfad, ex);
        }
    }

    private String escape(String wert) {
        Map<String, String> ersetzungen = new HashMap<>();
        ersetzungen.put("&", "&amp;");
        ersetzungen.put("<", "&lt;");
        ersetzungen.put(">", "&gt;");
        ersetzungen.put("\"", "&quot;");
        ersetzungen.put("'", "&#39;");
        StringBuilder out = new StringBuilder(wert.length());
        for (int i = 0; i < wert.length(); i++) {
            String ch = String.valueOf(wert.charAt(i));
            String e = ersetzungen.get(ch);
            out.append(e == null ? ch : e);
        }
        return out.toString();
    }
}
