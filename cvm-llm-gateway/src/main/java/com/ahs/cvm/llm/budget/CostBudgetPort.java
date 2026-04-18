package com.ahs.cvm.llm.budget;

import java.util.UUID;

/**
 * Port fuer den monatlichen KI-Kosten-Cap (Iteration 21, CVM-52).
 *
 * <p>Die Implementierung liegt in {@code cvm-application}
 * ({@code LlmCostGuardAdapter}). Der Audit-Service fragt vor jedem
 * Call, ob das Budget der aktiven Umgebung noch nicht aufgebraucht
 * ist; andernfalls wird der Call gar nicht erst abgesetzt.
 */
public interface CostBudgetPort {

    /**
     * {@code true} wenn das Budget noch nicht aufgebraucht ist.
     * Fehlende Environment-Id oder unbekanntes Profil werden als
     * "unbegrenzt" behandelt, damit der Cap nicht fuer
     * nicht-mandanten-spezifische Calls greift.
     */
    boolean isUnderBudget(UUID environmentId);
}
