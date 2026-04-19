package com.ahs.cvm.llm;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Einheitliche Abstraktion ueber alle LLM-Anbieter. Kein anderer
 * Code im System darf direkt ein HTTP-Aufruf gegen ein LLM abschicken
 * &mdash; alle Calls laufen durch eine Implementierung dieses
 * Interfaces, gekapselt vom {@link AiCallAuditService}.
 */
public interface LlmClient {

    /**
     * Fuehrt einen Completion-Call aus. Aufrufer stellt sicher, dass
     * ein Audit-Eintrag im Status {@code PENDING} existiert; andernfalls
     * darf diese Methode nicht aufgerufen werden.
     */
    LlmResponse complete(LlmRequest request);

    /**
     * Identifier des Modells, das dieser Adapter liefert. Wird in das
     * Audit geschrieben.
     */
    String modelId();

    /**
     * Provider-Schluessel, passend zu {@code LlmConfiguration.provider}
     * (z.B. {@code anthropic}, {@code ollama}). Default
     * {@code unknown}; Adapter, die ueber
     * {@link TenantLlmSettings} Mandanten-Overrides erhalten sollen,
     * ueberschreiben die Methode.
     */
    default String provider() {
        return "unknown";
    }

    /** Ein einzelner Nachrichten-Eintrag im Gespraechsverlauf. */
    record Message(Role role, String content) {
        public enum Role { USER, ASSISTANT }
    }

    /**
     * Eingabe fuer {@link #complete(LlmRequest)}.
     *
     * @param useCase Kurz-Id des Aufrufers (z.B. {@code auto-assessment}).
     * @param promptTemplateId Id des geladenen Templates.
     * @param promptTemplateVersion Version des Templates.
     * @param systemPrompt Nachricht mit der System-Rolle.
     * @param messages User-/Assistant-Historie.
     * @param outputSchema JSON-Schema, gegen das validiert wird.
     * @param temperature Sampling-Parameter (0..1).
     * @param maxTokens Obergrenze fuer die Antwort.
     * @param environmentId Optionale Umgebungs-Id (fuer Audit).
     * @param triggeredBy Login des Auslosers (Pflicht fuer Audit).
     * @param ragContext Optional: RAG-Kontext, der im Audit mitgefuehrt wird.
     * @param metadata Freie Metadaten (z.B. Mandant).
     */
    record LlmRequest(
            String useCase,
            String promptTemplateId,
            String promptTemplateVersion,
            String systemPrompt,
            List<Message> messages,
            JsonNode outputSchema,
            double temperature,
            int maxTokens,
            UUID environmentId,
            String triggeredBy,
            String ragContext,
            Map<String, Object> metadata) {

        public LlmRequest {
            if (useCase == null || useCase.isBlank()) {
                throw new IllegalArgumentException("useCase darf nicht leer sein.");
            }
            if (systemPrompt == null) {
                throw new IllegalArgumentException("systemPrompt darf nicht null sein.");
            }
            if (messages == null || messages.isEmpty()) {
                throw new IllegalArgumentException("messages darf nicht leer sein.");
            }
            if (triggeredBy == null || triggeredBy.isBlank()) {
                throw new IllegalArgumentException("triggeredBy darf nicht leer sein.");
            }
            if (promptTemplateId == null || promptTemplateId.isBlank()) {
                throw new IllegalArgumentException("promptTemplateId darf nicht leer sein.");
            }
            if (promptTemplateVersion == null || promptTemplateVersion.isBlank()) {
                throw new IllegalArgumentException("promptTemplateVersion darf nicht leer sein.");
            }
            if (temperature < 0.0 || temperature > 1.0) {
                throw new IllegalArgumentException("temperature muss in [0,1] liegen.");
            }
            if (maxTokens <= 0) {
                throw new IllegalArgumentException("maxTokens muss > 0 sein.");
            }
            messages = List.copyOf(messages);
            metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        }
    }

    /**
     * Antwort eines Calls.
     *
     * @param structuredOutput geparstes JSON gegen das Output-Schema.
     * @param rawText Roh-Antwort (zur Audit-Persistenz).
     * @param usage Token-Nutzung.
     * @param latency Gemessene Latenz des HTTP-Calls.
     * @param modelId Modell-Identifier (z.B. {@code claude-sonnet-4-6}).
     */
    record LlmResponse(
            JsonNode structuredOutput,
            String rawText,
            TokenUsage usage,
            Duration latency,
            String modelId) {}

    /** Token-Nutzung; Felder koennen {@code null} sein, wenn der Anbieter sie nicht meldet. */
    record TokenUsage(Integer promptTokens, Integer completionTokens) {

        public static TokenUsage empty() {
            return new TokenUsage(null, null);
        }
    }
}
