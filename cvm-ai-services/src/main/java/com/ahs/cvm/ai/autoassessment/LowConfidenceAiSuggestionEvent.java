package com.ahs.cvm.ai.autoassessment;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Wird vom {@link AutoAssessmentOrchestrator} publiziert, sobald
 * ein AI-Vorschlag mit einer Confidence unterhalb der Auto-
 * Trigger-Schwelle abgegeben wird (Iteration 70, CVM-307).
 *
 * <p>Der Event wird bewusst NICHT als "Reachability-sofort-
 * starten" interpretiert - der Listener wendet zuvor einen
 * eigenen Schwellwert und ein Rate-Limit an. Der Event liefert
 * nur den Anlass.
 *
 * @param findingId Finding, fuer das der Vorschlag gilt.
 * @param productVersionId Produkt-Version - Teil des Rate-Limit-
 *                         Schluessels.
 * @param cveKey Kanonischer CVE-Key, z.&nbsp;B.
 *               {@code CVE-2017-18640}.
 * @param confidence Ungerundete Confidence aus dem AI-Output.
 * @param triggeredBy Login / Tech-User aus {@code LlmRequest}.
 */
public record LowConfidenceAiSuggestionEvent(
        UUID findingId,
        UUID productVersionId,
        String cveKey,
        BigDecimal confidence,
        String triggeredBy) {

    public LowConfidenceAiSuggestionEvent {
        if (findingId == null) {
            throw new IllegalArgumentException("findingId darf nicht null sein.");
        }
        if (cveKey == null || cveKey.isBlank()) {
            throw new IllegalArgumentException("cveKey darf nicht leer sein.");
        }
        if (confidence == null) {
            confidence = BigDecimal.ZERO;
        }
        if (triggeredBy == null || triggeredBy.isBlank()) {
            triggeredBy = "system:auto-assessment";
        }
    }
}
