package com.ahs.cvm.application.llmconfig;

import java.math.BigDecimal;

/**
 * Command-Records fuer den {@code LlmConfigurationService}
 * (Iteration 34, CVM-78).
 */
public final class LlmConfigurationCommands {

    public record Create(
            String name,
            String description,
            String provider,
            String model,
            String baseUrl,
            String secret,
            Integer maxTokens,
            BigDecimal temperature,
            boolean active) {}

    /**
     * Teil-Update: Felder, die {@code null} sind, bleiben wie vorher.
     * Ausnahme: {@code secretClear=true} loescht das Secret
     * explizit. {@code secret=null} allein heisst "unveraendert",
     * andernfalls wird das neue Secret verschluesselt.
     */
    public record Update(
            String name,
            String description,
            String provider,
            String model,
            String baseUrl,
            String secret,
            boolean secretClear,
            Integer maxTokens,
            BigDecimal temperature,
            Boolean active) {}

    private LlmConfigurationCommands() {}
}
