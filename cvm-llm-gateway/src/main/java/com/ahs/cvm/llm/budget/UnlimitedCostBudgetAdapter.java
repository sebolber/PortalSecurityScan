package com.ahs.cvm.llm.budget;

import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * Fallback-Adapter ({@link CostBudgetPort}), solange kein
 * konkreter Budget-Adapter (z.&nbsp;B. {@code LlmCostGuardAdapter}
 * aus {@code cvm-application}) geladen ist. Erlaubt alle Calls.
 */
@Component
@ConditionalOnMissingBean(
        value = CostBudgetPort.class,
        ignored = UnlimitedCostBudgetAdapter.class)
public class UnlimitedCostBudgetAdapter implements CostBudgetPort {

    @Override
    public boolean isUnderBudget(UUID environmentId) {
        return true;
    }
}
