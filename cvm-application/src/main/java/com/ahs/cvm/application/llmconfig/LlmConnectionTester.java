package com.ahs.cvm.application.llmconfig;

/**
 * Port fuer den LLM-Verbindungstest. Die konkrete HTTP-Implementierung
 * lebt in {@code cvm-integration}; {@code cvm-application} darf
 * keine externen HTTP-Clients direkt benutzen (Hexagonal-Grenze).
 *
 * <p>Implementierungen muessen einen minimalen Ping-Call gegen das im
 * Command bezeichnete Provider-Endpoint absetzen. Der Test umgeht die
 * Audit-Pipeline bewusst: er ist kein fachlicher LLM-Call.
 */
public interface LlmConnectionTester {

    /**
     * Fuehrt den Verbindungstest synchron aus. Exceptions werden in
     * das {@link LlmConfigurationTestResult#failure} uebersetzt und
     * <em>nicht</em> propagiert - die UI erwartet auch bei 401/404/
     * Timeouts ein strukturiertes Ergebnis.
     */
    LlmConfigurationTestResult test(LlmConfigurationTestCommand cmd);
}
