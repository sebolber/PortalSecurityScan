package com.ahs.cvm.domain.enums;

/**
 * Lebenszyklus eines KI-Regel-Vorschlags (Iteration 17, CVM-42).
 *
 * <ul>
 *   <li>{@link #PROPOSED} - vom Extraktions-Job erzeugt, wartet auf
 *       Admin-Entscheidung.</li>
 *   <li>{@link #APPROVED} - vom Admin freigegeben; eine
 *       {@code Rule(origin=AI_EXTRACTED)} ist daraus entstanden.</li>
 *   <li>{@link #REJECTED} - verworfen (mit Begruendung).</li>
 *   <li>{@link #EXPIRED} - zu alt und nicht bearbeitet; Pflege durch
 *       Scheduler (optional).</li>
 * </ul>
 */
public enum RuleSuggestionStatus {
    PROPOSED,
    APPROVED,
    REJECTED,
    EXPIRED
}
