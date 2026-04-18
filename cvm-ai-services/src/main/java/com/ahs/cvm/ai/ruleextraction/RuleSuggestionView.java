package com.ahs.cvm.ai.ruleextraction;

import com.ahs.cvm.domain.enums.AhsSeverity;
import com.ahs.cvm.domain.enums.RuleSuggestionStatus;
import com.ahs.cvm.persistence.rulesuggestion.RuleSuggestion;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Read-Model fuer den RuleSuggestion-REST-Endpoint. Haelt die
 * ArchUnit-Regel {@code api -> persistence} ausser Kraft, indem
 * das Mapping bereits in der ai-services-Schicht passiert.
 */
public record RuleSuggestionView(
        UUID id,
        String name,
        AhsSeverity proposedSeverity,
        String conditionJson,
        String rationaleTemplate,
        String clusterRationale,
        int historicalMatchCount,
        int wouldHaveCovered,
        BigDecimal coverageRate,
        int conflictCount,
        RuleSuggestionStatus status,
        String suggestedBy,
        Instant createdAt) {

    public static RuleSuggestionView from(RuleSuggestion s) {
        return new RuleSuggestionView(
                s.getId(), s.getName(), s.getProposedSeverity(),
                s.getConditionJson(), s.getRationaleTemplate(),
                s.getClusterRationale(),
                s.getHistoricalMatchCount(), s.getWouldHaveCovered(),
                s.getCoverageRate(), s.getConflictCount(),
                s.getStatus(), s.getSuggestedBy(), s.getCreatedAt());
    }
}
