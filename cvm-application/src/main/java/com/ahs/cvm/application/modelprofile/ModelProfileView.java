package com.ahs.cvm.application.modelprofile;

import com.ahs.cvm.persistence.modelprofile.LlmModelProfile;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Read-View eines LLM-Modell-Profils fuer die REST-Schicht
 * (Iteration 25, CVM-56).
 */
public record ModelProfileView(
        UUID id,
        String profileKey,
        String provider,
        String modelId,
        String modelVersion,
        BigDecimal costBudgetEurMonthly,
        boolean approvedForGkvData) {

    public static ModelProfileView from(LlmModelProfile p) {
        return new ModelProfileView(
                p.getId(),
                p.getProfileKey(),
                p.getProvider() == null ? null : p.getProvider().name(),
                p.getModelId(),
                p.getModelVersion(),
                p.getCostBudgetEurMonthly(),
                p.isApprovedForGkvData());
    }
}
