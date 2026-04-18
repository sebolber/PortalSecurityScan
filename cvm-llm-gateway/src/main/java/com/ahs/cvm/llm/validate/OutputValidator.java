package com.ahs.cvm.llm.validate;

import com.ahs.cvm.domain.enums.AhsSeverity;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Prueft, ob das vom Modell gelieferte JSON mit dem angeforderten
 * Schema uebereinstimmt und keine unerwuenschten Muster enthaelt.
 *
 * <p>Der Validator ist bewusst konservativ: einfache Schema-
 * Validierung (required-Feldnamen + Typen auf erster Ebene), Pruefung
 * der Severity-Werte gegen {@link AhsSeverity} und ein Pattern-Match
 * auf "Anweisung-an-den-Nutzer"-Formulierungen im Rationale-Feld.
 *
 * <p>Ziel ist es, obvious fehlerhafte oder manipulative Ausgaben
 * abzufangen, **nicht** eine vollstaendige JSON-Schema-Engine
 * abzubilden. Die Invariante "kein unzulaessiger Severity-Wert"
 * ist besonders wichtig (Konzept 4.4).
 */
@Component
public class OutputValidator {

    private static final Set<String> ERLAUBTE_SEVERITIES;
    private static final Pattern ANWEISUNGS_MUSTER = Pattern.compile(
            "\\b(please (go|click|open|visit|run|execute)|"
                    + "please ignore|as an ai|go to http|"
                    + "klicke|oeffne diesen link)\\b",
            Pattern.CASE_INSENSITIVE);

    static {
        Set<String> s = new HashSet<>();
        for (AhsSeverity sev : AhsSeverity.values()) {
            s.add(sev.name());
        }
        ERLAUBTE_SEVERITIES = Set.copyOf(s);
    }

    /**
     * Prueft die gelieferte Antwort gegen das uebergebene Schema.
     * Gibt eine Liste von Fehlermeldungen zurueck; leer = gueltig.
     *
     * @param output strukturierte Ausgabe des Modells.
     * @param expectedSchema Schema mit Feldnamen und einfachen Typen
     *     (z.B. {@code {"severity":"string","rationale":"string"}}).
     *     Darf {@code null} sein, dann wird nur Payload-Heuristik
     *     angewandt.
     */
    public List<String> validate(JsonNode output, JsonNode expectedSchema) {
        List<String> fehler = new ArrayList<>();
        if (output == null || output.isMissingNode() || output.isNull()) {
            fehler.add("Output ist null oder fehlt.");
            return fehler;
        }

        if (expectedSchema != null && expectedSchema.isObject()) {
            expectedSchema.fieldNames().forEachRemaining(field -> {
                if (!output.has(field)) {
                    fehler.add("Pflichtfeld fehlt: " + field);
                    return;
                }
                String typ = expectedSchema.path(field).asText("string");
                if (!typOk(output.path(field), typ)) {
                    fehler.add("Feld %s hat Typ %s, erwartet %s"
                            .formatted(field,
                                    output.path(field).getNodeType().name().toLowerCase(Locale.ROOT),
                                    typ));
                }
            });
        }

        if (output.has("severity") && output.get("severity").isTextual()) {
            String sev = output.get("severity").asText();
            if (!ERLAUBTE_SEVERITIES.contains(sev)) {
                fehler.add("Severity '%s' ist kein AhsSeverity-Wert.".formatted(sev));
            }
        }

        if (output.has("rationale") && output.get("rationale").isTextual()) {
            String rationale = output.get("rationale").asText();
            if (ANWEISUNGS_MUSTER.matcher(rationale).find()) {
                fehler.add("Rationale enthaelt eine Anweisung an den Nutzer.");
            }
            if (rationale.length() > 4000) {
                fehler.add("Rationale ueberschreitet 4000 Zeichen.");
            }
        }

        return List.copyOf(fehler);
    }

    private static boolean typOk(JsonNode node, String erwartet) {
        if (erwartet == null) {
            return true;
        }
        return switch (erwartet.toLowerCase(Locale.ROOT)) {
            case "string" -> node.isTextual();
            case "number" -> node.isNumber();
            case "boolean" -> node.isBoolean();
            case "array" -> node.isArray();
            case "object" -> node.isObject();
            default -> true;
        };
    }
}
