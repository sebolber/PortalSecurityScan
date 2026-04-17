package com.ahs.cvm.application.rules;

import com.ahs.cvm.domain.enums.AhsSeverity;
import com.ahs.cvm.domain.enums.RuleOrigin;
import com.ahs.cvm.domain.enums.RuleStatus;
import com.ahs.cvm.persistence.rule.Rule;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Read-Model fuer Regeln. Haelt die Arch-Regel {@code api -> persistence}
 * aus dem Controller fern.
 */
public record RuleView(
        UUID id,
        String ruleKey,
        String name,
        String description,
        RuleStatus status,
        RuleOrigin origin,
        AhsSeverity proposedSeverity,
        String conditionJson,
        String rationaleTemplate,
        List<String> rationaleSourceFields,
        String createdBy,
        String activatedBy,
        Instant activatedAt,
        Instant createdAt) {

    public static RuleView from(Rule r) {
        return new RuleView(
                r.getId(),
                r.getRuleKey(),
                r.getName(),
                r.getDescription(),
                r.getStatus(),
                r.getOrigin(),
                r.getProposedSeverity(),
                r.getConditionJson(),
                r.getRationaleTemplate(),
                r.getRationaleSourceFields(),
                r.getCreatedBy(),
                r.getActivatedBy(),
                r.getActivatedAt(),
                r.getCreatedAt());
    }
}
