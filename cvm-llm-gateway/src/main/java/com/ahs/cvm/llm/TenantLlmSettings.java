package com.ahs.cvm.llm;

/**
 * Mandantenspezifische Laufzeit-Overrides fuer einen einzelnen
 * LLM-Call (Iteration 34c, CVM-78).
 *
 * <p>Enthaelt nur die Felder, die ein Adapter zum Ausfuehren eines
 * Calls wirklich braucht. Insbesondere <strong>keine</strong> Audit-
 * oder Budget-Informationen - die lesen wir weiter aus der
 * {@link com.ahs.cvm.llm.LlmClient.LlmRequest}.
 *
 * @param provider Normalisierter Provider-Schluessel aus
 *                 {@code LlmConfiguration.provider}, z.B. "anthropic".
 *                 Wird mit {@link LlmClient#provider()} abgeglichen.
 * @param model    Modell-Id, die an den Anbieter gesendet wird.
 * @param baseUrl  Ziel-URL. Darf {@code null} sein, dann greift der
 *                 Adapter-Default.
 * @param apiKey   Entschluesselter API-Key. Darf {@code null} sein
 *                 (z.B. lokales Ollama ohne Auth).
 */
public record TenantLlmSettings(
        String provider,
        String model,
        String baseUrl,
        String apiKey) {

    public TenantLlmSettings {
        if (provider == null || provider.isBlank()) {
            throw new IllegalArgumentException(
                    "provider darf nicht leer sein.");
        }
        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException(
                    "model darf nicht leer sein.");
        }
    }

    public boolean hasBaseUrl() {
        return baseUrl != null && !baseUrl.isBlank();
    }

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }
}
