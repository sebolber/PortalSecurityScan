package com.ahs.cvm.llm.adapter;

import com.ahs.cvm.llm.LlmClient;
import com.ahs.cvm.llm.TenantLlmSettings;
import com.ahs.cvm.llm.TenantLlmSettingsProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
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

    private static final Logger log = LoggerFactory.getLogger(OllamaClient.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    static final String PROVIDER = "ollama";

    private final RestClient defaultRestClient;
    private final String defaultBaseUrl;
    private final String defaultModel;
    private final Optional<TenantLlmSettingsProvider> settingsProvider;

    @Autowired
    public OllamaClient(
            @Value("${cvm.llm.ollama.base-url:http://ollama:11434}") String baseUrl,
            @Value("${cvm.llm.ollama.model:llama3.1:8b-instruct}") String model,
            Optional<TenantLlmSettingsProvider> settingsProvider) {
        this.defaultModel = model;
        this.defaultBaseUrl = baseUrl;
        this.settingsProvider = settingsProvider;
        this.defaultRestClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /** Test-Konstruktor fuer WireMock. */
    public OllamaClient(RestClient restClient, String model) {
        this(restClient, model, Optional.empty());
    }

    /** Test-Konstruktor mit Tenant-Provider. */
    public OllamaClient(
            RestClient restClient, String model,
            Optional<TenantLlmSettingsProvider> settingsProvider) {
        this.defaultRestClient = restClient;
        this.defaultBaseUrl = null;
        this.defaultModel = model;
        this.settingsProvider = settingsProvider == null
                ? Optional.empty()
                : settingsProvider;
    }

    @Override
    public String provider() {
        return PROVIDER;
    }

    @Override
    public String modelId() {
        return defaultModel;
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        Optional<TenantLlmSettings> tenant = resolveTenantOverride();
        RestClient client = tenant
                .filter(TenantLlmSettings::hasBaseUrl)
                .map(this::buildTenantRestClient)
                .orElse(defaultRestClient);
        String effectiveModel = tenant.map(TenantLlmSettings::model)
                .orElse(defaultModel);

        ObjectNode body = MAPPER.createObjectNode();
        body.put("model", effectiveModel);
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
        JsonNode response = client.post()
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
                effectiveModel);
    }

    private Optional<TenantLlmSettings> resolveTenantOverride() {
        return settingsProvider
                .flatMap(TenantLlmSettingsProvider::resolveCurrent)
                .filter(s -> PROVIDER.equals(s.provider()));
    }

    private RestClient buildTenantRestClient(TenantLlmSettings settings) {
        try {
            return RestClient.builder()
                    .baseUrl(settings.baseUrl())
                    .requestFactory(new SimpleClientHttpRequestFactory())
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .build();
        } catch (RuntimeException ex) {
            log.warn(
                    "Fallback auf Default-Ollama-Endpoint, Tenant-baseUrl ungueltig: {}",
                    settings.baseUrl());
            return defaultRestClient;
        }
    }

    /** HTTP-/Parsing-Fehler. */
    public static class OllamaException extends RuntimeException {
        public OllamaException(String message) {
            super(message);
        }
    }
}
