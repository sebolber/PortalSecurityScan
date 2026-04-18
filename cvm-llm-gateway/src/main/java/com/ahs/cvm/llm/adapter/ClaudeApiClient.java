package com.ahs.cvm.llm.adapter;

import com.ahs.cvm.llm.LlmClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * HTTP-Adapter fuer die Anthropic Messages-API.
 *
 * <p>Wir sprechen das API direkt ueber {@link RestClient} an (statt
 * ueber ein SDK), weil wir volle Kontrolle ueber Timeouts, Retry und
 * Audit brauchen. Die {@code anthropic-version}-Header ist Pflicht.
 *
 * <p>Strukturierte Ausgabe wird via {@code response_format.type=json}
 * erwartet; alternativ waere {@code tool_use} moeglich. Der Adapter
 * liest das JSON aus dem ersten {@code content}-Block und uebergibt
 * es dem {@link com.ahs.cvm.llm.validate.OutputValidator}.
 */
@Component
@ConditionalOnProperty(prefix = "cvm.llm", name = "enabled", havingValue = "true")
public class ClaudeApiClient implements LlmClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String DEFAULT_MODEL = "claude-sonnet-4-6";

    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    @Autowired
    public ClaudeApiClient(
            @Value("${cvm.llm.claude.base-url:https://api.anthropic.com}") String baseUrl,
            @Value("${cvm.llm.claude.version:2023-06-01}") String version,
            @Value("${cvm.llm.claude.timeout-seconds:30}") int timeoutSeconds,
            @Value("${cvm.llm.claude.model:claude-sonnet-4-6}") String model,
            @Value("${cvm.llm.claude.api-key:${ANTHROPIC_API_KEY:}}") String apiKey) {
        this.apiKey = apiKey;
        this.model = model == null || model.isBlank() ? DEFAULT_MODEL : model;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("anthropic-version", version)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /** Test-Konstruktor, damit WireMock-URLs injiziert werden koennen. */
    public ClaudeApiClient(RestClient restClient, String apiKey, String model) {
        this.restClient = restClient;
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public String modelId() {
        return model;
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        ObjectNode body = buildBody(request);
        Instant start = Instant.now();
        JsonNode response = restClient.post()
                .uri("/v1/messages")
                .header("x-api-key", apiKey)
                .body(body)
                .retrieve()
                .body(JsonNode.class);
        Duration latency = Duration.between(start, Instant.now());
        return parseResponse(response, latency);
    }

    private ObjectNode buildBody(LlmRequest request) {
        ObjectNode body = MAPPER.createObjectNode();
        body.put("model", model);
        body.put("max_tokens", request.maxTokens());
        body.put("temperature", request.temperature());
        body.put("system", request.systemPrompt());
        ArrayNode messages = body.putArray("messages");
        for (Message m : request.messages()) {
            ObjectNode msg = messages.addObject();
            msg.put("role", m.role().name().toLowerCase());
            msg.put("content", m.content());
        }
        return body;
    }

    private LlmResponse parseResponse(JsonNode response, Duration latency) {
        if (response == null) {
            throw new ClaudeApiException("Leere Antwort von Claude-API.");
        }
        JsonNode content = response.path("content");
        String rawText = "";
        if (content.isArray() && content.size() > 0) {
            rawText = content.get(0).path("text").asText("");
        }
        JsonNode structured;
        try {
            structured = rawText.isBlank()
                    ? MAPPER.createObjectNode()
                    : MAPPER.readTree(rawText);
        } catch (Exception ex) {
            throw new ClaudeApiException(
                    "Antwort von Claude war kein JSON: " + ex.getMessage());
        }
        JsonNode usage = response.path("usage");
        TokenUsage tokenUsage = new TokenUsage(
                usage.hasNonNull("input_tokens") ? usage.get("input_tokens").asInt() : null,
                usage.hasNonNull("output_tokens") ? usage.get("output_tokens").asInt() : null);
        String modelId = response.path("model").asText(model);
        return new LlmResponse(structured, rawText, tokenUsage, latency, modelId);
    }

    /** Wird bei HTTP-/Parsing-Fehlern geworfen. */
    public static class ClaudeApiException extends RuntimeException {
        public ClaudeApiException(String message) {
            super(message);
        }
    }

    /**
     * Helper fuer Tests: liefert die aktuelle Konfiguration als Map.
     */
    public Map<String, Object> configuration() {
        return Map.of("model", model, "hasKey", apiKey != null && !apiKey.isBlank());
    }

    /** Nicht verwendet, aber explizit exponiert fuer Doku-Zwecke. */
    List<?> preparedMessages(LlmRequest request) {
        ArrayNode arr = buildBody(request).withArray("messages");
        return MAPPER.convertValue(arr, List.class);
    }
}
