package com.ahs.cvm.application.rules;

import com.ahs.cvm.domain.enums.AhsSeverity;
import java.util.List;
import java.util.UUID;

/**
 * Von der {@link RuleEngine} erzeugter Regel-Treffer. Wird vom
 * {@code CascadeService} zum Bau eines {@code PROPOSED}-Assessments in
 * Iteration 06 verwendet.
 */
public record ProposedResult(
        UUID ruleId,
        String ruleKey,
        AhsSeverity severity,
        String rationale,
        List<String> sourceFields) {}
