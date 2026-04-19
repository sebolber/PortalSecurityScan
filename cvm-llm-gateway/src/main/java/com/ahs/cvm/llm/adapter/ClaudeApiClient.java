package com.ahs.cvm.llm.adapter;

import com.ahs.cvm.llm.LlmClient;
import com.ahs.cvm.llm.TenantLlmSettings;
import com.ahs.cvm.llm.TenantLlmSettingsProvider;
import com.ahs.cvm.llm.config.LlmGlobalParameterResolver;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
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
 *
 * <p>Iteration 66 (CVM-303): api-key / model / base-url /
 * timeout-seconds werden pro Call ueber den
 * {@link LlmGlobalParameterResolver} aufgeloest. Aenderungen im
 * System-Parameter-Store greifen ohne Neustart. Der
 * {@code RestClient} wird lazy neu gebaut, wenn sich base-url oder
 * timeout-seconds aendern.
 */
@Component
@ConditionalOnProperty(prefix = "cvm.llm", name = "enabled", havingValue = "true")
public class ClaudeApiClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(ClaudeApiClient.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String DEFAULT_MODEL = "claude-sonnet-4-6";
    static final String PROVIDER = "anthropic";

    private static final String PARAM_BASE_URL = "cvm.llm.claude.base-url";
    private static final String PARAM_MODEL = "cvm.llm.claude.model";
    private static final String PARAM_API_KEY = "cvm.llm.claude.api-key";
    private static final String PARAM_TIMEOUT = "cvm.llm.claude.timeout-seconds";

    private final String defaultBaseUrl;
    private final String version;
    private final String defaultApiKey;
    private final String defaultModel;
    private final int defaultTimeoutSeconds;
    private final Optional<TenantLlmSettingsProvider> settingsProvider;
    private final Optional<LlmGlobalParameterResolver> globalResolver;

    private final AtomicReference<CachedClient> cachedDefault = new AtomicReference<>();

    @Autowired
    public ClaudeApiClient(
            @Value("${cvm.llm.claude.base-url:https://api.anthropic.com}") String baseUrl,
            @Value("${cvm.llm.claude.version:2023-06-01}") String version,
            @Value("${cvm.llm.claude.timeout-seconds:30}") int timeoutSeconds,
            @Value("${cvm.llm.claude.model:claude-sonnet-4-6}") String model,
            @Value("${cvm.llm.claude.api-key:${ANTHROPIC_API_KEY:}}") String apiKey,
            Optional<TenantLlmSettingsProvider> settingsProvider,
            Optional<LlmGlobalParameterResolver> globalResolver) {
        this.defaultApiKey = apiKey;
        this.defaultModel = model == null || model.isBlank() ? DEFAULT_MODEL : model;
        this.defaultBaseUrl = baseUrl;
        this.version = version;
        this.defaultTimeoutSeconds = Math.max(1, timeoutSeconds);
        this.settingsProvider = settingsProvider;
        this.globalResolver = globalResolver == null
                ? Optional.empty()
                : globalResolver;
    }

    /** Test-Konstruktor, damit WireMock-URLs injiziert werden koennen. */
    public ClaudeApiClient(RestClient restClient, String apiKey, String model) {
        this(restClient, apiKey, model, Optional.empty());
    }

    /** Test-Konstruktor mit Tenant-Provider. */
    public ClaudeApiClient(
            RestClient restClient, String apiKey, String model,
            Optional<TenantLlmSettingsProvider> settingsProvider) {
        this(restClient, apiKey, model, settingsProvider, Optional.empty());
    }

    /** Test-Konstruktor mit Tenant-Provider + Global-Resolver. */
    public ClaudeApiClient(
            RestClient restClient, String apiKey, String model,
            Optional<TenantLlmSettingsProvider> settingsProvider,
            Optional<LlmGlobalParameterResolver> globalResolver) {
        this.defaultBaseUrl = null;
        this.version = "2023-06-01";
        this.defaultApiKey = apiKey;
        this.defaultModel = model;
        this.defaultTimeoutSeconds = 30;
        this.settingsProvider = settingsProvider == null
                ? Optional.empty()
                : settingsProvider;
        this.globalResolver = globalResolver == null
                ? Optional.empty()
                : globalResolver;
        this.cachedDefault.set(new CachedClient(null, 30, restClient));
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

        String effectiveBaseUrl = tenant
                .filter(TenantLlmSettings::hasBaseUrl)
                .map(TenantLlmSettings::baseUrl)
                .orElseGet(() -> resolveFromGlobal(PARAM_BASE_URL, defaultBaseUrl));
        String effectiveModel = tenant
                .map(TenantLlmSettings::model)
                .orElseGet(() -> resolveFromGlobal(PARAM_MODEL, defaultModel));
        String effectiveApiKey = tenant
                .filter(TenantLlmSettings::hasApiKey)
                .map(TenantLlmSettings::apiKey)
                .orElseGet(() -> resolveFromGlobal(PARAM_API_KEY, defaultApiKey));
        int effectiveTimeout = globalResolver
                .flatMap(r -> r.resolve(PARAM_TIMEOUT))
                .map(this::parseTimeout)
                .orElse(defaultTimeoutSeconds);

        RestClient client = clientFor(effectiveBaseUrl, effectiveTimeout);

        ObjectNode body = buildBody(request, effectiveModel);
        Instant start = Instant.now();
        JsonNode response = client.post()
                .uri("/v1/messages")
                .header("x-api-key", effectiveApiKey)
                .body(body)
                .retrieve()
                .body(JsonNode.class);
        Duration latency = Duration.between(start, Instant.now());
        return parseResponse(response, latency, effectiveModel);
    }

    private String resolveFromGlobal(String paramKey, String fallback) {
        return globalResolver
                .flatMap(r -> r.resolve(paramKey))
                .filter(v -> !v.isBlank())
                .orElse(fallback);
    }

    private int parseTimeout(String raw) {
        try {
            int v = Integer.parseInt(raw.trim());
            return v > 0 ? v : defaultTimeoutSeconds;
        } catch (NumberFormatException ex) {
            return defaultTimeoutSeconds;
        }
    }

    /**
     * Liefert einen {@link RestClient} fuer die gegebene base-url
     * und Timeout-Kombination. Bei Gleichheit mit dem gecachten
     * Eintrag wird wiederverwendet, sonst lazy neu gebaut.
     */
    private RestClient clientFor(String baseUrl, int timeoutSeconds) {
        CachedClient current = cachedDefault.get();
        if (current != null
                && Objects.equals(current.baseUrl, baseUrl)
                && current.timeoutSeconds == timeoutSeconds) {
            return current.client;
        }
        RestClient fresh = buildRestClient(baseUrl, timeoutSeconds);
        cachedDefault.set(new CachedClient(baseUrl, timeoutSeconds, fresh));
        return fresh;
    }

    private RestClient buildRestClient(String baseUrl, int timeoutSeconds) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        int millis = Math.max(1, timeoutSeconds) * 1000;
        factory.setConnectTimeout(millis);
        factory.setReadTimeout(millis);
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .defaultHeader("anthropic-version", version)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    private Optional<TenantLlmSettings> resolveTenantOverride() {
        return settingsProvider
                .flatMap(TenantLlmSettingsProvider::resolveCurrent)
                .filter(s -> PROVIDER.equals(s.provider()));
    }

    private ObjectNode buildBody(LlmRequest request, String effectiveModel) {
        ObjectNode body = MAPPER.createObjectNode();
        body.put("model", effectiveModel);
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

    private LlmResponse parseResponse(
            JsonNode response, Duration latency, String effectiveModel) {
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
        String modelId = response.path("model").asText(effectiveModel);
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
        return Map.of(
                "model", defaultModel,
                "hasKey", defaultApiKey != null && !defaultApiKey.isBlank(),
                "baseUrl", defaultBaseUrl == null ? "" : defaultBaseUrl);
    }

    /** Nicht verwendet, aber explizit exponiert fuer Doku-Zwecke. */
    List<?> preparedMessages(LlmRequest request) {
        ArrayNode arr = buildBody(request, defaultModel).withArray("messages");
        return MAPPER.convertValue(arr, List.class);
    }

    private record CachedClient(String baseUrl, int timeoutSeconds, RestClient client) {}
}
