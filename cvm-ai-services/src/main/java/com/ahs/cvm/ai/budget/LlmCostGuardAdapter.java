package com.ahs.cvm.ai.budget;

import com.ahs.cvm.application.modelprofile.LlmCostGuard;
import com.ahs.cvm.llm.budget.CostBudgetPort;
import com.ahs.cvm.persistence.environment.EnvironmentRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Verdrahtet den {@link CostBudgetPort} aus {@code cvm-llm-gateway}
 * auf den echten {@link LlmCostGuard} aus {@code cvm-application}
 * (Go-Live-Nachzug zu Iteration 21, CVM-52).
 *
 * <p>Loest {@code environmentId -> Environment -> llmModelProfileId}
 * auf und delegiert dann an den Guard. Diese Verdrahtung liegt in
 * {@code cvm-ai-services}, weil dort sowohl {@code application} als
 * auch {@code llm-gateway} zur Verfuegung stehen (ArchUnit-konform).
 */
@Component
public class LlmCostGuardAdapter implements CostBudgetPort {

    private final EnvironmentRepository environmentRepository;
    private final LlmCostGuard guard;

    public LlmCostGuardAdapter(
            EnvironmentRepository environmentRepository,
            LlmCostGuard guard) {
        this.environmentRepository = environmentRepository;
        this.guard = guard;
    }

    @Override
    public boolean isUnderBudget(UUID environmentId) {
        if (environmentId == null) {
            return true;
        }
        return environmentRepository.findById(environmentId)
                .map(e -> guard.isUnderBudget(e.getLlmModelProfileId()))
                .orElse(true);
    }
}
