package com.ahs.cvm.ai.copilot;

import java.util.Map;
import java.util.UUID;

/**
 * Eingabe fuer {@link CopilotService#suggest(CopilotRequest)}.
 *
 * @param assessmentId Bezugs-Assessment.
 * @param useCase einer der vier Use-Cases.
 * @param userInstruction frei formulierte Anweisung des Bewerters.
 * @param triggeredBy Login fuer Audit (Pflicht).
 * @param attachments optionale Felder (z.B. {@code commit} fuer
 *     {@code EXPLAIN_COMMIT}).
 */
public record CopilotRequest(
        UUID assessmentId,
        CopilotUseCase useCase,
        String userInstruction,
        String triggeredBy,
        Map<String, String> attachments) {

    public CopilotRequest {
        if (assessmentId == null) {
            throw new IllegalArgumentException("assessmentId darf nicht null sein.");
        }
        if (useCase == null) {
            throw new IllegalArgumentException("useCase darf nicht null sein.");
        }
        if (userInstruction == null || userInstruction.isBlank()) {
            throw new IllegalArgumentException("userInstruction darf nicht leer sein.");
        }
        if (triggeredBy == null || triggeredBy.isBlank()) {
            throw new IllegalArgumentException("triggeredBy darf nicht leer sein.");
        }
        attachments = attachments == null ? Map.of() : Map.copyOf(attachments);
    }
}
