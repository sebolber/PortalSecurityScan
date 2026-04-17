package com.ahs.cvm.application.cascade;

import com.ahs.cvm.application.rules.RuleEvaluationContext;
import java.util.UUID;

/**
 * Eingabedaten fuer einen Cascade-Aufruf. Der {@link RuleEvaluationContext}
 * wird vom Aufrufer bereits mit Profil-, CVE- und Komponent-Snapshot
 * vorbereitet. Umgebung und ProduktVersion werden fuer den REUSE-Lookup
 * benoetigt.
 */
public record CascadeInput(
        UUID cveId,
        UUID productVersionId,
        UUID environmentId,
        RuleEvaluationContext evaluationContext) {}
