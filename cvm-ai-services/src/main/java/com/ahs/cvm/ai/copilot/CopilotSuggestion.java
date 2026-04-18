package com.ahs.cvm.ai.copilot;

import java.util.List;
import java.util.UUID;

/**
 * Vorschlag des Copilot. Reines Read-Model. Wichtig: enthaelt
 * <strong>keine</strong> Severity. Der Copilot darf den Bewertungs-
 * Status nie direkt setzen.
 */
public record CopilotSuggestion(
        UUID assessmentId,
        CopilotUseCase useCase,
        String text,
        List<SourceRef> sources) {

    public CopilotSuggestion {
        sources = sources == null ? List.of() : List.copyOf(sources);
    }

    public record SourceRef(String kind, String ref, String excerpt) {}
}
