package com.ahs.cvm.ai.reachability;

import java.util.List;
import java.util.UUID;

/**
 * Ergebnis einer Reachability-Analyse (Iteration 15, CVM-40).
 *
 * @param findingId fachliches Bezugs-Finding.
 * @param suggestionId persistierter {@code AiSuggestion}-Eintrag.
 * @param recommendation BLOCK / VERIFY / ACCEPT.
 * @param summary Zusammenfassung.
 * @param callSites detaillierte Fundstellen.
 * @param available {@code false}, wenn die Analyse nicht moeglich
 *     war (Timeout, Subprocess-Fehler, ungueltiger Output).
 * @param noteIfUnavailable optionaler Hinweis-Text.
 */
public record ReachabilityResult(
        UUID findingId,
        UUID suggestionId,
        String recommendation,
        String summary,
        List<CallSite> callSites,
        boolean available,
        String noteIfUnavailable) {

    public ReachabilityResult {
        callSites = callSites == null ? List.of() : List.copyOf(callSites);
    }

    public record CallSite(
            String file,
            int line,
            String symbol,
            String trust,
            String note) {}
}
