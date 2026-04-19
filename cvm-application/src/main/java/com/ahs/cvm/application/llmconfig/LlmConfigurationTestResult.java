package com.ahs.cvm.application.llmconfig;

/**
 * Ergebnis eines Verbindungstests gegen einen LLM-Provider.
 *
 * @param success        {@code true}, wenn der Ping-Call ein 2xx lieferte.
 * @param provider       Normalisierter Provider-Schluessel.
 * @param model          Eingesetztes Modell.
 * @param httpStatus     HTTP-Status der Antwort (falls der Call zustande kam).
 * @param latencyMs      Gemessene Latenz in Millisekunden.
 * @param message        Menschlich lesbare Kurzfassung (z.B.
 *                       "OK, Tokens=1/1" oder "HTTP 401: invalid key").
 */
public record LlmConfigurationTestResult(
        boolean success,
        String provider,
        String model,
        Integer httpStatus,
        long latencyMs,
        String message) {

    public static LlmConfigurationTestResult success(
            String provider, String model, Integer httpStatus,
            long latencyMs, String message) {
        return new LlmConfigurationTestResult(
                true, provider, model, httpStatus, latencyMs, message);
    }

    public static LlmConfigurationTestResult failure(
            String provider, String model, Integer httpStatus,
            long latencyMs, String message) {
        return new LlmConfigurationTestResult(
                false, provider, model, httpStatus, latencyMs, message);
    }
}
