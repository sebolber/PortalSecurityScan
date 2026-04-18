package com.ahs.cvm.ai.ruleextraction;

import com.ahs.cvm.domain.enums.RuleStatus;
import com.ahs.cvm.persistence.rule.Rule;
import java.time.Instant;
import java.util.UUID;

/** Read-Model nach einer Suggestion-Freigabe. */
public record ApproveRuleResult(
        UUID ruleId,
        String ruleKey,
        RuleStatus status,
        Instant activatedAt) {

    public static ApproveRuleResult from(Rule rule) {
        return new ApproveRuleResult(
                rule.getId(), rule.getRuleKey(), rule.getStatus(), rule.getActivatedAt());
    }
}
