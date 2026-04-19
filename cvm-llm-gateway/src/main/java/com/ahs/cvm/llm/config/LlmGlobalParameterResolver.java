package com.ahs.cvm.llm.config;

import java.util.Optional;

/**
 * Port fuer das Auflesen globaler LLM-Parameter aus dem System-
 * Parameter-Store (Iteration 66, CVM-303).
 *
 * <p>Liegt im {@code cvm-llm-gateway}-Modul, weil die Adapter-
 * Beans ({@code ClaudeApiClient}, {@code OllamaClient}, ...) hier
 * wohnen und kein Zugriff auf {@code cvm.application..} erlaubt
 * ist (ArchUnit-Regel
 * {@code llm_gateway_greift_nur_auf_domain_zu}).
 *
 * <p>Die tatsaechliche Implementierung lebt in {@code cvm-ai-services}
 * und bruecke-t auf den
 * {@code SystemParameterResolver}. Ohne Implementierung (z.B. in
 * isolierten Unit-Tests des Adapters) bleibt der Port leer und der
 * Adapter faellt auf die {@code @Value}-Defaults zurueck.
 */
@FunctionalInterface
public interface LlmGlobalParameterResolver {

    /**
     * Liefert den Wert des Parameters fuer den aktuellen Tenant-
     * Kontext - oder {@link Optional#empty()}, wenn weder ein
     * Tenant gesetzt ist noch ein expliziter Wert im Store vorliegt.
     */
    Optional<String> resolve(String paramKey);
}
