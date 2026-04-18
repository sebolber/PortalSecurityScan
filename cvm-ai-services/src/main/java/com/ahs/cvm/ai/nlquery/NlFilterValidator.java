package com.ahs.cvm.ai.nlquery;

import com.ahs.cvm.ai.nlquery.NlFilter.SortBy;
import com.ahs.cvm.domain.enums.AhsSeverity;
import com.ahs.cvm.domain.enums.AssessmentStatus;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Whitelist-Validator fuer das JSON, das der NL-LLM-Call zurueckgibt
 * (Iteration 19, CVM-50). Kernaussage: nur die fachlich freigegebenen
 * Feldnamen duerfen vorkommen; unbekannte Felder fuehren zur
 * Ablehnung des Filters - damit entsteht nie eine Query auf Basis
 * einer Halluzination.
 */
@Component
public class NlFilterValidator {

    /** Erlaubte Keys im {@code filter}-Objekt. */
    static final Set<String> ALLOWED_FILTER_FIELDS = Set.of(
            "environment", "productVersion", "severityIn", "statusIn",
            "minAgeDays", "hasUpstreamFix", "kevOnly");

    static final Set<String> ALLOWED_SORT = Set.of(
            "age_desc", "age_asc", "severity_desc", "severity_asc");

    public ValidationResult validate(JsonNode out) {
        List<String> errors = new ArrayList<>();
        if (out == null || !out.isObject()) {
            return fail("Output kein JSON-Objekt.");
        }
        JsonNode filter = out.path("filter");
        if (!filter.isObject()) {
            return fail("Feld 'filter' muss ein Objekt sein.");
        }
        for (Iterator<String> it = filter.fieldNames(); it.hasNext(); ) {
            String key = it.next();
            if (!ALLOWED_FILTER_FIELDS.contains(key)) {
                errors.add("Unbekanntes Filterfeld: " + key);
            }
        }
        NlFilter.SortBy sortBy = NlFilter.SortBy.AGE_DESC;
        String sortText = out.path("sortBy").asText("age_desc");
        if (!ALLOWED_SORT.contains(sortText)) {
            errors.add("Unbekanntes Sortierfeld: " + sortText);
        } else {
            sortBy = SortBy.valueOf(sortText.toUpperCase());
        }
        if (!errors.isEmpty()) {
            return new ValidationResult(null, errors, out.path("explanation").asText(""));
        }

        List<AhsSeverity> severities = parseEnumList(
                filter.path("severityIn"), AhsSeverity.class, errors, "severityIn");
        List<AssessmentStatus> statuses = parseEnumList(
                filter.path("statusIn"), AssessmentStatus.class, errors, "statusIn");
        Integer minAge = filter.path("minAgeDays").isNumber()
                ? filter.get("minAgeDays").asInt() : null;
        Boolean hasFix = filter.path("hasUpstreamFix").isBoolean()
                ? filter.get("hasUpstreamFix").asBoolean() : null;
        Boolean kev = filter.path("kevOnly").isBoolean()
                ? filter.get("kevOnly").asBoolean() : null;

        if (!errors.isEmpty()) {
            return new ValidationResult(null, errors, out.path("explanation").asText(""));
        }

        NlFilter nlFilter = new NlFilter(
                textOrNull(filter.path("environment")),
                textOrNull(filter.path("productVersion")),
                severities,
                statuses,
                minAge,
                hasFix,
                kev,
                sortBy);
        return new ValidationResult(nlFilter, List.of(),
                out.path("explanation").asText(""));
    }

    private static String textOrNull(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        String s = node.asText("");
        return s.isBlank() ? null : s;
    }

    private static <E extends Enum<E>> List<E> parseEnumList(
            JsonNode arr, Class<E> enumClass, List<String> errors, String field) {
        if (arr == null || arr.isMissingNode() || arr.isNull()) {
            return List.of();
        }
        if (!arr.isArray()) {
            errors.add(field + ": muss ein Array sein");
            return List.of();
        }
        List<E> out = new ArrayList<>();
        for (JsonNode e : arr) {
            if (!e.isTextual()) {
                errors.add(field + ": Eintraege muessen Strings sein");
                continue;
            }
            try {
                out.add(Enum.valueOf(enumClass, e.asText()));
            } catch (IllegalArgumentException ex) {
                errors.add(field + ": unbekannter Wert '" + e.asText() + "'");
            }
        }
        return List.copyOf(out);
    }

    private static ValidationResult fail(String message) {
        return new ValidationResult(null, List.of(message), "");
    }

    public record ValidationResult(NlFilter filter, List<String> errors, String explanation) {
        public ValidationResult {
            errors = errors == null ? List.of() : List.copyOf(errors);
        }
        public boolean ok() { return errors.isEmpty() && filter != null; }
    }
}
