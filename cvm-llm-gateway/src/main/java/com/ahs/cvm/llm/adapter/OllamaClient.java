package com.ahs.cvm.llm.adapter;

import com.ahs.cvm.llm.LlmClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * HTTP-Adapter fuer on-prem Ollama-Instanzen. Ziel: Mandanten mit
 * besonders strengen Datenhoheitsanforderungen (Konzept v0.2
 * Abschnitt 12.4).
 */
@Component
@ConditionalOnProperty(prefix = "cvm.llm", name = "enabled", havingValue = "true")
public class OllamaClient implements LlmClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final RestClient restClient;
    private final String model;

    @Autowired
    public OllamaClient(
            @Value("${cvm.llm.ollama.base-url:http://ollama:11434}") String baseUrl,
            @Value("${cvm.llm.ollama.model:llama3.1:8b-instruct}") String model) {
        this.model = model;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /** Test-Konstruktor fuer WireMock. */
    public OllamaClient(RestClient restClient, String model) {
        this.restClient = restClient;
        this.model = model;
    }

    @Override
    public String modelId() {
        return model;
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        ObjectNode body = MAPPER.createObjectNode();
        body.put("model", model);
        body.put("stream", false);
        body.put("format", "json");
        ObjectNode options = body.putObject("options");
        options.put("temperature", request.temperature());
        options.put("num_predict", request.maxTokens());
        ArrayNode messages = body.putArray("messages");
        ObjectNode systemMsg = messages.addObject();
        systemMsg.put("role", "system");
        systemMsg.put("content", request.systemPrompt());
        for (Message m : request.messages()) {
            ObjectNode msg = messages.addObject();
            msg.put("role", m.role().name().toLowerCase());
            msg.put("content", m.content());
        }
        Instant start = Instant.now();
        JsonNode response = restClient.post()
                .uri("/api/chat")
                .body(body)
                .retrieve()
                .body(JsonNode.class);
        Duration latency = Duration.between(start, Instant.now());
        if (response == null) {
            throw new OllamaException("Leere Antwort von Ollama.");
        }
        JsonNode msg = response.path("message").path("content");
        String rawText = msg.isTextual() ? msg.asText() : response.path("response").asText("");
        JsonNode structured;
        try {
            structured = rawText.isBlank()
                    ? MAPPER.createObjectNode()
                    : MAPPER.readTree(rawText);
        } catch (Exception ex) {
            throw new OllamaException("Antwort war kein JSON: " + ex.getMessage());
        }
        return new LlmResponse(
                structured,
                rawText,
                new TokenUsage(
                        response.hasNonNull("prompt_eval_count")
                                ? response.get("prompt_eval_count").asInt() : null,
                        response.hasNonNull("eval_count")
                                ? response.get("eval_count").asInt() : null),
                latency,
                model);
    }

    /** HTTP-/Parsing-Fehler. */
    public static class OllamaException extends RuntimeException {
        public OllamaException(String message) {
            super(message);
        }
    }
}
