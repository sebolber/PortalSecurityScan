package com.ahs.cvm.application.rules;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Mini-Datenmodell fuer die Regel-Auswertung. Alles, was der
 * {@link RuleEvaluator} und der {@link RationaleTemplateInterpolator} sehen
 * duerfen. Bewusst entkoppelt von JPA-Entities.
 */
public record RuleEvaluationContext(
        CveSnapshot cve,
        JsonNode profile,
        ComponentSnapshot component,
        FindingSnapshot finding) {

    public record CveSnapshot(
            UUID id,
            String cveId,
            String description,
            List<String> cwes,
            boolean kev,
            BigDecimal epss,
            BigDecimal cvssScore) {}

    public record ComponentSnapshot(String pkgType, String name, String version) {}

    public record FindingSnapshot(UUID id, Instant detectedAt) {}
}
