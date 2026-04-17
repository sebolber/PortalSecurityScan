package com.ahs.cvm.application.cascade;

import com.ahs.cvm.domain.enums.AhsSeverity;
import com.ahs.cvm.domain.enums.ProposalSource;
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
 *   <li>{@link ProposalSource#AI_SUGGESTION} &mdash; Iteration 13 fuellt
 *       dies aus; hier noch nicht erreichbar.</li>
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
        List<String> sourceFields) {

    public static CascadeOutcome reuse(UUID assessmentId, AhsSeverity severity, String rationale) {
        return new CascadeOutcome(
                ProposalSource.REUSE, severity, rationale, assessmentId, null, List.of());
    }

    public static CascadeOutcome rule(
            UUID ruleId, AhsSeverity severity, String rationale, List<String> sourceFields) {
        return new CascadeOutcome(
                ProposalSource.RULE, severity, rationale, null, ruleId, List.copyOf(sourceFields));
    }

    public static CascadeOutcome manual() {
        return new CascadeOutcome(ProposalSource.HUMAN, null, null, null, null, List.of());
    }
}
