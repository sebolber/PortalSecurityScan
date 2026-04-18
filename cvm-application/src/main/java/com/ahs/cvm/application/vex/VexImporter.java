package com.ahs.cvm.application.vex;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Liest VEX-Dokumente (CycloneDX) und produziert
 * {@link VexStatement}-Vorschlaege. Schreibt selbst nichts in die
 * DB - die Queue uebernimmt die Entscheidung. Unbekannte Felder
 * fuehren zu {@code warnings}, Schema-Fehler zu {@code errors}.
 */
@Service
public class VexImporter {

    private static final Logger log = LoggerFactory.getLogger(VexImporter.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    static final Set<String> ALLOWED_CYCLONE_STATES = Set.of(
            "not_affected", "exploitable", "resolved", "in_triage",
            "affected", "fixed", "under_investigation");

    public Parsed parse(String documentJson) {
        List<VexStatement> statements = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        JsonNode root;
        try {
            root = MAPPER.readTree(documentJson);
        } catch (Exception ex) {
            return new Parsed(statements, warnings,
                    List.of("Document ist kein gueltiges JSON: " + ex.getMessage()));
        }
        if (root == null || !root.isObject()) {
            errors.add("Root ist kein Objekt.");
            return new Parsed(statements, warnings, errors);
        }
        String format = root.path("bomFormat").asText("");
        if (!"CycloneDX".equalsIgnoreCase(format)) {
            errors.add("Nur CycloneDX wird unterstuetzt - gefunden: '"
                    + format + "'");
            return new Parsed(statements, warnings, errors);
        }
        if (!root.path("vulnerabilities").isArray()) {
            errors.add("Feld 'vulnerabilities' fehlt oder ist kein Array.");
            return new Parsed(statements, warnings, errors);
        }
        for (JsonNode v : root.path("vulnerabilities")) {
            String id = v.path("id").asText(null);
            if (id == null || id.isBlank()) {
                warnings.add("Vulnerability ohne id uebersprungen.");
                continue;
            }
            JsonNode analysis = v.path("analysis");
            String rawState = analysis.path("state").asText("");
            String state = rawState.toLowerCase();
            if (!ALLOWED_CYCLONE_STATES.contains(state)) {
                warnings.add("Unbekannter state '" + rawState + "' bei " + id);
                continue;
            }
            VexStatus status = mapState(state);
            String purl = null;
            JsonNode affects = v.path("affects");
            if (affects.isArray() && !affects.isEmpty()) {
                purl = affects.get(0).path("ref").asText(null);
            }
            statements.add(new VexStatement(
                    id, purl, status,
                    analysis.hasNonNull("justification")
                            ? analysis.get("justification").asText() : null,
                    analysis.path("detail").asText(""),
                    null,
                    List.of()));
        }
        if (statements.isEmpty() && errors.isEmpty()) {
            warnings.add("Keine verwertbaren Statements im Dokument.");
        }
        return new Parsed(statements, warnings, errors);
    }

    static VexStatus mapState(String state) {
        return switch (state) {
            case "not_affected" -> VexStatus.NOT_AFFECTED;
            case "resolved", "fixed" -> VexStatus.FIXED;
            case "in_triage", "under_investigation" -> VexStatus.UNDER_INVESTIGATION;
            default -> VexStatus.AFFECTED;
        };
    }

    public record Parsed(List<VexStatement> statements, List<String> warnings,
            List<String> errors) {
        public Parsed {
            statements = statements == null ? List.of() : List.copyOf(statements);
            warnings = warnings == null ? List.of() : List.copyOf(warnings);
            errors = errors == null ? List.of() : List.copyOf(errors);
        }
    }
}
