package com.ahs.cvm.api.llmconfig;

import java.math.BigDecimal;

/**
 * Request-Records fuer den {@link LlmConfigurationController}
 * (Iteration 34, CVM-78).
 *
 * <p>Primitive Types sind absichtlich Wrapper, damit der Client im
 * Update ein Feld auslassen kann (null = nicht aendern). {@code
 * secretClear} ist explizit, weil {@code null} beim secret "lasse so"
 * bedeutet und ein leerer String die Semantik nicht eindeutig waere.
 */
public final class LlmConfigurationRequests {

    public record Create(
            String name,
            String description,
            String provider,
            String model,
            String baseUrl,
            String secret,
            Integer maxTokens,
            BigDecimal temperature,
            Boolean active) {}

    public record Update(
            String name,
            String description,
            String provider,
            String model,
            String baseUrl,
            String secret,
            Boolean secretClear,
            Integer maxTokens,
            BigDecimal temperature,
            Boolean active) {}

    /**
     * Request fuer den Verbindungstest. {@code id} ist optional - ohne
     * id ist der Test rein ad-hoc (z.B. neues Formular), mit id werden
     * Felder aus der gespeicherten Konfiguration inklusive
     * entschluesseltem Secret ergaenzt.
     */
    public record Test(
            java.util.UUID id,
            String provider,
            String model,
            String baseUrl,
            String secret) {}

    private LlmConfigurationRequests() {}
}
