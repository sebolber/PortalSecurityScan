package com.ahs.cvm.ai.ruleextraction;

import com.ahs.cvm.application.rules.ConditionNode;
import com.ahs.cvm.application.rules.ConditionParser;
import com.ahs.cvm.application.rules.RuleEvaluationContext;
import com.ahs.cvm.application.rules.RuleEvaluationContext.ComponentSnapshot;
import com.ahs.cvm.application.rules.RuleEvaluationContext.CveSnapshot;
import com.ahs.cvm.application.rules.RuleEvaluationContext.FindingSnapshot;
import com.ahs.cvm.application.rules.RuleEvaluator;
import com.ahs.cvm.domain.enums.AhsSeverity;
import com.ahs.cvm.persistence.assessment.Assessment;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Wertet einen vorgeschlagenen Regel-Condition gegen die historische
 * Assessment-Menge aus (Iteration 17, CVM-42). Liefert Coverage-
 * Metriken und Konflikt-Liste; keine DB-Schreiboperationen.
 */
@Component
public class DryRunEvaluator {

    private static final Logger log = LoggerFactory.getLogger(DryRunEvaluator.class);

    private final ConditionParser parser;
    private final RuleEvaluator evaluator;

    public DryRunEvaluator(ConditionParser parser, RuleEvaluator evaluator) {
        this.parser = parser;
        this.evaluator = evaluator;
    }

    public DryRunReport evaluate(
            String conditionJson,
            AhsSeverity proposedSeverity,
            List<Assessment> historie) {
        ConditionNode node;
        try {
            node = parser.parse(conditionJson);
        } catch (RuntimeException ex) {
            log.warn("Condition-Struktur ungueltig: {}", ex.getMessage());
            return DryRunReport.empty();
        }

        int matches = 0;
        int wouldHaveCovered = 0;
        List<DryRunConflict> conflicts = new ArrayList<>();
        for (Assessment a : historie) {
            RuleEvaluationContext ctx = baueContext(a);
            boolean trifft;
            try {
                trifft = evaluator.evaluate(node, ctx);
            } catch (RuntimeException ex) {
                continue;
            }
            if (!trifft) {
                continue;
            }
            matches++;
            if (a.getSeverity() == proposedSeverity) {
                wouldHaveCovered++;
            } else {
                conflicts.add(new DryRunConflict(
                        a.getId(), a.getSeverity(),
                        "erwartet " + proposedSeverity + ", tatsaechlich " + a.getSeverity()));
            }
        }
        BigDecimal rate = matches == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(wouldHaveCovered)
                        .divide(BigDecimal.valueOf(matches), 3, RoundingMode.HALF_UP);
        return new DryRunReport(matches, wouldHaveCovered, rate, List.copyOf(conflicts));
    }

    static RuleEvaluationContext baueContext(Assessment a) {
        CveSnapshot cve = new CveSnapshot(
                a.getCve() == null ? null : a.getCve().getId(),
                a.getCve() == null ? null : a.getCve().getCveId(),
                a.getCve() == null ? "" : Optional.ofNullable(a.getCve().getSummary()).orElse(""),
                a.getCve() == null ? List.of()
                        : Optional.ofNullable(a.getCve().getCwes()).orElse(List.of()),
                a.getCve() != null && Boolean.TRUE.equals(a.getCve().getKevListed()),
                Optional.ofNullable(a.getCve() == null ? null : a.getCve().getEpssScore())
                        .orElse(BigDecimal.ZERO),
                Optional.ofNullable(a.getCve() == null ? null : a.getCve().getCvssBaseScore())
                        .orElse(BigDecimal.ZERO));
        ComponentSnapshot comp;
        if (a.getFinding() != null
                && a.getFinding().getComponentOccurrence() != null
                && a.getFinding().getComponentOccurrence().getComponent() != null) {
            var c = a.getFinding().getComponentOccurrence().getComponent();
            comp = new ComponentSnapshot(c.getType(), c.getName(), c.getVersion());
        } else {
            comp = new ComponentSnapshot("-", "-", "-");
        }
        FindingSnapshot fs = new FindingSnapshot(
                a.getFinding() == null ? null : a.getFinding().getId(),
                Instant.now());
        return new RuleEvaluationContext(cve, JsonNodeFactory.instance.objectNode(), comp, fs);
    }

    public record DryRunReport(
            int historicalMatchCount,
            int wouldHaveCovered,
            BigDecimal coverageRate,
            List<DryRunConflict> conflicts) {

        public static DryRunReport empty() {
            return new DryRunReport(0, 0, BigDecimal.ZERO, List.of());
        }

        public int conflictCount() {
            return conflicts.size();
        }
    }

    public record DryRunConflict(
            java.util.UUID assessmentId,
            AhsSeverity actualSeverity,
            String note) {}
}
