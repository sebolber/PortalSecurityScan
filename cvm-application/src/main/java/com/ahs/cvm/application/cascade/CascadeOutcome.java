package com.ahs.cvm.application.cascade;

import com.ahs.cvm.domain.enums.AhsSeverity;
import com.ahs.cvm.domain.enums.AssessmentStatus;
import com.ahs.cvm.domain.enums.ProposalSource;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Ergebnis einer Cascade-Auswertung.
 *
 * <ul>
 *   <li>{@link ProposalSource#REUSE} &mdash; {@code reusedAssessmentId}
 *       zeigt auf das bereits freigegebene Assessment.</li>
 *   <li>{@link ProposalSource#RULE} &mdash; Regel-Treffer, Rationale und
 *       Severity sind gesetzt.</li>
 *   <li>{@link ProposalSource#AI_SUGGESTION} &mdash; KI-Vorschlag
 *       (Iteration 13). {@code aiSuggestionId} verweist auf den
 *       persistierten {@code ai_suggestion}-Datensatz; bei
 *       Halluzinations-Verdacht wird {@link #targetStatus()} auf
 *       {@link AssessmentStatus#NEEDS_VERIFICATION} gesetzt.</li>
 *   <li>{@link ProposalSource#HUMAN} &mdash; Fallback, Mensch entscheidet.
 *       Severity bleibt {@code null}.</li>
 * </ul>
 */
public record CascadeOutcome(
        ProposalSource source,
        AhsSeverity severity,
        String rationale,
        UUID reusedAssessmentId,
        UUID ruleId,
        UUID aiSuggestionId,
        BigDecimal confidence,
        AssessmentStatus targetStatus,
        List<String> sourceFields) {

    public static CascadeOutcome reuse(UUID assessmentId, AhsSeverity severity, String rationale) {
        return new CascadeOutcome(
                ProposalSource.REUSE, severity, rationale,
                assessmentId, null, null, null,
                AssessmentStatus.PROPOSED, List.of());
    }

    public static CascadeOutcome rule(
            UUID ruleId, AhsSeverity severity, String rationale, List<String> sourceFields) {
        return new CascadeOutcome(
                ProposalSource.RULE, severity, rationale,
                null, ruleId, null, null,
                AssessmentStatus.PROPOSED, List.copyOf(sourceFields));
    }

    public static CascadeOutcome ai(
            UUID aiSuggestionId,
            AhsSeverity severity,
            String rationale,
            BigDecimal confidence,
            List<String> sourceFields,
            AssessmentStatus targetStatus) {
        return new CascadeOutcome(
                ProposalSource.AI_SUGGESTION, severity, rationale,
                null, null, aiSuggestionId, confidence,
                targetStatus, List.copyOf(sourceFields));
    }

    public static CascadeOutcome manual() {
        return new CascadeOutcome(
                ProposalSource.HUMAN, null, null,
                null, null, null, null,
                AssessmentStatus.PROPOSED, List.of());
    }
}
