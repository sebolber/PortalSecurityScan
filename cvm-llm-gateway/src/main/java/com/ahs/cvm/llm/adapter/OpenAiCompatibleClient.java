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
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * OpenAI-kompatibler Chat-Completions-Adapter (Iteration 40, CVM-84).
 *
 * <p>Bedient die Provider {@code openai}, {@code azure} und
 * {@code adesso-ai-hub}. Alle drei benutzen das OpenAI-Chat-
 * Completions-API-Format (POST {@code /chat/completions} mit
 * {@code response_format:{"type":"json_object"}} fuer strukturierte
 * Ausgabe). Der Adapter wird nur aktiv, wenn eine aktive
 * {@link TenantLlmSettings} einen der drei Provider angibt - die
 * Default-Konfiguration aus {@code application.yaml} ist bewusst
 * nicht vorgesehen, weil wir keine Credentials im Repo haben.
 */
@Component
@ConditionalOnProperty(prefix = "cvm.llm", name = "enabled", havingValue = "true")
public class OpenAiCompatibleClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleClient.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    static final Set<String> PROVIDERS = Set.of("openai", "azure", "adesso-ai-hub");
    static final String PROVIDER_KEY = "openai";

    private final String defaultModel;
    private final Optional<TenantLlmSettingsProvider> settingsProvider;

    public OpenAiCompatibleClient(
            @Value("${cvm.llm.openai.default-model:gpt-4o-mini}") String defaultModel,
            Optional<TenantLlmSettingsProvider> settingsProvider) {
        this.defaultModel = defaultModel;
        this.settingsProvider = settingsProvider;
    }

    /** Test-Konstruktor. */
    OpenAiCompatibleClient(
            String defaultModel,
            TenantLlmSettingsProvider provider) {
        this(defaultModel, Optional.ofNullable(provider));
    }

    /**
     * <strong>Wichtig</strong>: Der Adapter meldet den Sammel-Provider-
     * Schluessel {@code openai}. Der Selector matcht damit allerdings
     * genau drei Provider-Werte aus {@link TenantLlmSettings}. Die
     * Unterscheidung erfolgt erst beim Aufruf (unterschiedliche
     * BaseUrl, Azure mit {@code api-key}-Header).
     */
    @Override
    public String provider() {
        return PROVIDER_KEY;
    }

    @Override
    public boolean supportsProvider(String providerKey) {
        return providerKey != null && PROVIDERS.contains(providerKey);
    }

    @Override
    public String modelId() {
        return defaultModel;
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        TenantLlmSettings settings = settingsProvider
                .flatMap(TenantLlmSettingsProvider::resolveCurrent)
                .filter(s -> PROVIDERS.contains(s.provider()))
                .orElseThrow(() -> new OpenAiException(
                        "Kein Tenant-Setting fuer OpenAI-kompatible Provider. "
                                + "Adapter wurde nie ueber Default-Spring-Values konfiguriert."));

        if (!settings.hasBaseUrl()) {
            throw new OpenAiException(
                    "TenantLlmSettings.baseUrl fehlt (Provider " + settings.provider() + ").");
        }

        RestClient client = RestClient.builder()
                .baseUrl(settings.baseUrl())
                .requestFactory(new SimpleClientHttpRequestFactory())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        ObjectNode body = buildBody(request, settings.model());
        Instant start = Instant.now();
        JsonNode response = client.post()
                .uri("/chat/completions")
                .headers(h -> authHeaders(h, settings))
                .body(body)
                .retrieve()
                .body(JsonNode.class);
        Duration latency = Duration.between(start, Instant.now());
        return parseResponse(response, latency, settings.model());
    }

    private static void authHeaders(HttpHeaders headers, TenantLlmSettings settings) {
        if (!settings.hasApiKey()) {
            return;
        }
        if ("azure".equals(settings.provider())) {
            // Azure nutzt den {@code api-key}-Header statt Bearer.
            headers.add("api-key", settings.apiKey());
        } else {
            headers.setBearerAuth(settings.apiKey());
        }
    }

    private ObjectNode buildBody(LlmRequest request, String effectiveModel) {
        ObjectNode body = MAPPER.createObjectNode();
        body.put("model", effectiveModel);
        body.put("max_tokens", request.maxTokens());
        body.put("temperature", request.temperature());
        // Strukturierte Ausgabe (json_object, nicht json_schema, weil
        // Azure und adesso-ai-hub das erweiterte Schema-Feld heute
        // noch nicht durchgaengig unterstuetzen).
        ObjectNode responseFormat = body.putObject("response_format");
        responseFormat.put("type", "json_object");
        ArrayNode messages = body.putArray("messages");
        ObjectNode systemMsg = messages.addObject();
        systemMsg.put("role", "system");
        systemMsg.put("content", request.systemPrompt());
        for (Message m : request.messages()) {
            ObjectNode msg = messages.addObject();
            msg.put("role", m.role().name().toLowerCase());
            msg.put("content", m.content());
        }
        return body;
    }

    private LlmResponse parseResponse(
            JsonNode response, Duration latency, String effectiveModel) {
        if (response == null) {
            throw new OpenAiException("Leere Antwort vom OpenAI-kompatiblen Endpoint.");
        }
        JsonNode choices = response.path("choices");
        String rawText = "";
        if (choices.isArray() && choices.size() > 0) {
            rawText = choices.get(0).path("message").path("content").asText("");
        }
        JsonNode structured;
        try {
            structured = rawText.isBlank()
                    ? MAPPER.createObjectNode()
                    : MAPPER.readTree(rawText);
        } catch (Exception ex) {
            throw new OpenAiException(
                    "Antwort war kein JSON: " + ex.getMessage());
        }
        JsonNode usage = response.path("usage");
        TokenUsage tokenUsage = new TokenUsage(
                usage.hasNonNull("prompt_tokens") ? usage.get("prompt_tokens").asInt() : null,
                usage.hasNonNull("completion_tokens") ? usage.get("completion_tokens").asInt() : null);
        String modelId = response.path("model").asText(effectiveModel);
        return new LlmResponse(structured, rawText, tokenUsage, latency, modelId);
    }

    public static class OpenAiException extends RuntimeException {
        public OpenAiException(String message) {
            super(message);
        }
    }
}
