package com.ahs.cvm.application.rules;

import com.ahs.cvm.domain.enums.RuleStatus;
import com.ahs.cvm.persistence.rule.Rule;
import com.ahs.cvm.persistence.rule.RuleRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Evaluiert alle {@link RuleStatus#ACTIVE}-Regeln gegen einen
 * {@link RuleEvaluationContext}. Die erste passende Regel liefert das
 * Ergebnis (Stable Reihenfolge: {@code createdAt DESC} aus dem Repository).
 */
@Service
public class RuleEngine {

    private final RuleRepository ruleRepository;
    private final ConditionParser parser;
    private final RuleEvaluator evaluator;
    private final RationaleTemplateInterpolator interpolator;

    public RuleEngine(
            RuleRepository ruleRepository,
            ConditionParser parser,
            RuleEvaluator evaluator,
            RationaleTemplateInterpolator interpolator) {
        this.ruleRepository = ruleRepository;
        this.parser = parser;
        this.evaluator = evaluator;
        this.interpolator = interpolator;
    }

    @Transactional(readOnly = true)
    public Optional<ProposedResult> evaluate(RuleEvaluationContext ctx) {
        // Iteration 50 (CVM-100): Soft-Delete herausfiltern.
        List<Rule> regeln = ruleRepository
                .findByStatusAndDeletedAtIsNullOrderByCreatedAtDesc(RuleStatus.ACTIVE);
        for (Rule rule : regeln) {
            ConditionNode condition = parser.parse(rule.getConditionJson());
            if (evaluator.evaluate(condition, ctx)) {
                String rationale = interpolator.interpolate(rule.getRationaleTemplate(), ctx);
                return Optional.of(new ProposedResult(
                        rule.getId(),
                        rule.getRuleKey(),
                        rule.getProposedSeverity(),
                        rationale,
                        rule.getRationaleSourceFields() == null
                                ? List.of()
                                : List.copyOf(rule.getRationaleSourceFields())));
            }
        }
        return Optional.empty();
    }
}
