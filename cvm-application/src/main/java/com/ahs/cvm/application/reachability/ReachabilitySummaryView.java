package com.ahs.cvm.application.reachability;

import com.ahs.cvm.domain.enums.AhsSeverity;
import com.ahs.cvm.domain.enums.AiSuggestionStatus;
import com.ahs.cvm.persistence.ai.AiSuggestion;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Lese-Projektion eines Reachability-AiSuggestion fuer die
 * Uebersichtsseite (Iteration 27e, CVM-65).
 */
public record ReachabilitySummaryView(
        UUID id,
        UUID findingId,
        AiSuggestionStatus status,
        AhsSeverity severity,
        String rationale,
        BigDecimal confidence,
        Instant createdAt) {

    public static ReachabilitySummaryView from(AiSuggestion suggestion) {
        return new ReachabilitySummaryView(
                suggestion.getId(),
                suggestion.getFinding() == null ? null : suggestion.getFinding().getId(),
                suggestion.getStatus(),
                suggestion.getSeverity(),
                suggestion.getRationale(),
                suggestion.getConfidence(),
                suggestion.getCreatedAt());
    }
}
