package com.ahs.cvm.api.rules;

import com.ahs.cvm.application.rules.RuleView;
import com.ahs.cvm.domain.enums.AhsSeverity;
import com.ahs.cvm.domain.enums.RuleOrigin;
import com.ahs.cvm.domain.enums.RuleStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RuleResponse(
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

    public static RuleResponse from(RuleView v) {
        return new RuleResponse(
                v.id(), v.ruleKey(), v.name(), v.description(), v.status(), v.origin(),
                v.proposedSeverity(), v.conditionJson(), v.rationaleTemplate(),
                v.rationaleSourceFields(), v.createdBy(), v.activatedBy(),
                v.activatedAt(), v.createdAt());
    }
}
