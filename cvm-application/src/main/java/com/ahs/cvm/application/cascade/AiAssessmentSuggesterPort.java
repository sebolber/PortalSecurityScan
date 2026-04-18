package com.ahs.cvm.application.cascade;

import java.util.Optional;

/**
 * Port fuer die KI-Vorbewertung als Cascade-Stufe&nbsp;3 (Iteration 13,
 * CVM-32). Implementiert wird der Port im Modul {@code cvm-ai-services};
 * ist keine Bean registriert (z.&nbsp;B. weil
 * {@code cvm.ai.auto-assessment.enabled=false}), bleibt der Cascade-
 * Service auf MANUAL.
 */
public interface AiAssessmentSuggesterPort {

    /**
     * Versucht, fuer das gegebene Cascade-Input einen KI-gestuetzten
     * Vorschlag zu erzeugen. Liefert {@link Optional#empty()}, wenn
     * der KI-Pfad nicht angewendet werden kann (Flag aus, Modell
     * verweigert, Output ungueltig).
     */
    Optional<CascadeOutcome> suggest(CascadeInput input);
}
