package com.ahs.cvm.integration.llmtest;

import com.ahs.cvm.application.llmconfig.LlmConfigurationTestCommand;
import com.ahs.cvm.application.llmconfig.LlmConfigurationTestResult;
import com.ahs.cvm.application.llmconfig.LlmConnectionTester;
import com.ahs.cvm.application.llmconfig.ProviderDefaults;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * HTTP-Implementierung des {@link LlmConnectionTester}-Ports.
 *
 * <p>Pro Provider wird derselbe Endpoint und dasselbe Auth-Schema
 * verwendet wie in den produktiven Adaptern
 * ({@code ClaudeApiClient}, {@code OpenAiCompatibleClient},
 * {@code OllamaClient}) - ein erfolgreicher Test bedeutet, dass auch
 * die produktiven Calls funktionieren sollten.
 *
 * <p>Exceptions werden in {@link LlmConfigurationTestResult#failure}
 * uebersetzt; die UI erwartet auch bei 401/404/Timeouts eine
 * strukturierte Antwort.
 */
@Component
public class LlmConnectionTesterHttp implements LlmConnectionTester {

    private static final Logger log = LoggerFactory.getLogger(
            LlmConnectionTesterHttp.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(15);
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final Function<String, RestClient> clientFactory;

    public LlmConnectionTesterHttp() {
        this(baseUrl -> {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout((int) DEFAULT_TIMEOUT.toMillis());
            factory.setReadTimeout((int) DEFAULT_TIMEOUT.toMillis());
            return RestClient.builder()
                    .baseUrl(baseUrl)
                    .requestFactory(factory)
                    .defaultHeader(HttpHeaders.CONTENT_TYPE,
                            MediaType.APPLICATION_JSON_VALUE)
                    .build();
        });
    }

    /** Test-Hook, damit WireMock-URLs injiziert werden koennen. */
    LlmConnectionTesterHttp(Function<String, RestClient> clientFactory) {
        this.clientFactory = clientFactory;
    }

    @Override
    public LlmConfigurationTestResult test(LlmConfigurationTestCommand cmd) {
        String provider = ProviderDefaults.normalize(cmd.provider());
        if (provider == null || provider.isBlank()) {
            return LlmConfigurationTestResult.failure(
                    null, cmd.model(), null, 0L,
                    "Provider fehlt.");
        }
        String model = cmd.model() == null ? null : cmd.model().trim();
        if (model == null || model.isEmpty()) {
            return LlmConfigurationTestResult.failure(
                    provider, null, null, 0L, "Modell fehlt.");
        }
        String baseUrl = aufloeseBaseUrl(provider, cmd.baseUrl());
        if (baseUrl == null || baseUrl.isBlank()) {
            return LlmConfigurationTestResult.failure(
                    provider, model, null, 0L,
                    "Keine base-url fuer Provider '" + provider + "' gesetzt.");
        }
        // Cloud-Provider brauchen einen API-Key; Ollama laeuft per Default ohne Auth.
        if (!"ollama".equals(provider) && (cmd.apiKey() == null || cmd.apiKey().isBlank())) {
            return LlmConfigurationTestResult.failure(
                    provider, model, null, 0L,
                    "API-Key / Secret fehlt.");
        }

        return switch (provider) {
            case "anthropic" -> testAnthropic(baseUrl, model, cmd.apiKey());
            case "openai", "adesso-ai-hub" -> testOpenAiBearer(
                    provider, baseUrl, model, cmd.apiKey());
            case "azure" -> testAzure(baseUrl, model, cmd.apiKey());
            case "ollama" -> testOllama(baseUrl, model);
            default -> LlmConfigurationTestResult.failure(
                    provider, model, null, 0L,
                    "Unbekannter Provider: " + provider);
        };
    }

    private LlmConfigurationTestResult testAnthropic(
            String baseUrl, String model, String apiKey) {
        ObjectNode body = MAPPER.createObjectNode();
        body.put("model", model);
        body.put("max_tokens", 1);
        ArrayNode messages = body.putArray("messages");
        ObjectNode msg = messages.addObject();
        msg.put("role", "user");
        msg.put("content", "ping");

        return ausfuehren("anthropic", model, baseUrl,
                client -> client.post()
                        .uri("/v1/messages")
                        .header("x-api-key", apiKey)
                        .header("anthropic-version", ANTHROPIC_VERSION)
                        .body(body)
                        .retrieve()
                        .body(JsonNode.class));
    }

    private LlmConfigurationTestResult testOpenAiBearer(
            String provider, String baseUrl, String model, String apiKey) {
        ObjectNode body = openAiMinimalBody(model);
        return ausfuehren(provider, model, baseUrl,
                client -> client.post()
                        .uri("/chat/completions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                        .body(body)
                        .retrieve()
                        .body(JsonNode.class));
    }

    private LlmConfigurationTestResult testAzure(
            String baseUrl, String model, String apiKey) {
        ObjectNode body = openAiMinimalBody(model);
        return ausfuehren("azure", model, baseUrl,
                client -> client.post()
                        .uri("/chat/completions")
                        .header("api-key", apiKey)
                        .body(body)
                        .retrieve()
                        .body(JsonNode.class));
    }

    private LlmConfigurationTestResult testOllama(String baseUrl, String model) {
        ObjectNode body = MAPPER.createObjectNode();
        body.put("model", model);
        body.put("stream", false);
        ArrayNode messages = body.putArray("messages");
        ObjectNode msg = messages.addObject();
        msg.put("role", "user");
        msg.put("content", "ping");

        return ausfuehren("ollama", model, baseUrl,
                client -> client.post()
                        .uri("/api/chat")
                        .body(body)
                        .retrieve()
                        .body(JsonNode.class));
    }

    private static ObjectNode openAiMinimalBody(String model) {
        ObjectNode body = MAPPER.createObjectNode();
        body.put("model", model);
        body.put("max_tokens", 1);
        ArrayNode messages = body.putArray("messages");
        ObjectNode msg = messages.addObject();
        msg.put("role", "user");
        msg.put("content", "ping");
        return body;
    }

    private LlmConfigurationTestResult ausfuehren(
            String provider, String model, String baseUrl,
            Function<RestClient, JsonNode> call) {
        RestClient client = clientFactory.apply(baseUrl);
        Instant start = Instant.now();
        try {
            JsonNode response = call.apply(client);
            long latency = Duration.between(start, Instant.now()).toMillis();
            String meldung = beschreibeErfolg(provider, response);
            return LlmConfigurationTestResult.success(
                    provider, model, 200, latency, meldung);
        } catch (HttpStatusCodeException ex) {
            long latency = Duration.between(start, Instant.now()).toMillis();
            int status = ex.getStatusCode().value();
            String body = ex.getResponseBodyAsString();
            String msg = "HTTP " + status + ": "
                    + (body == null || body.isBlank()
                            ? ex.getStatusText() : kuerzen(body, 240));
            log.info("LLM-Test '{}' fehlgeschlagen: {}", provider, msg);
            return LlmConfigurationTestResult.failure(
                    provider, model, status, latency, msg);
        } catch (ResourceAccessException ex) {
            long latency = Duration.between(start, Instant.now()).toMillis();
            String msg = "Netzwerk-/Timeout-Fehler: " + ex.getMessage();
            log.info("LLM-Test '{}' Netzwerkfehler: {}", provider, msg);
            return LlmConfigurationTestResult.failure(
                    provider, model, null, latency, msg);
        } catch (RuntimeException ex) {
            long latency = Duration.between(start, Instant.now()).toMillis();
            String msg = ex.getClass().getSimpleName() + ": "
                    + (ex.getMessage() == null ? "unbekannt" : ex.getMessage());
            log.warn("LLM-Test '{}' unerwarteter Fehler: {}", provider, msg, ex);
            return LlmConfigurationTestResult.failure(
                    provider, model, null, latency, msg);
        }
    }

    private static String beschreibeErfolg(String provider, JsonNode response) {
        if (response == null) {
            return "HTTP 200, aber leere Antwort.";
        }
        Integer in = null;
        Integer out = null;
        switch (provider) {
            case "anthropic" -> {
                JsonNode usage = response.path("usage");
                in = usage.hasNonNull("input_tokens") ? usage.get("input_tokens").asInt() : null;
                out = usage.hasNonNull("output_tokens") ? usage.get("output_tokens").asInt() : null;
            }
            case "ollama" -> {
                in = response.hasNonNull("prompt_eval_count")
                        ? response.get("prompt_eval_count").asInt() : null;
                out = response.hasNonNull("eval_count")
                        ? response.get("eval_count").asInt() : null;
            }
            default -> {
                JsonNode usage = response.path("usage");
                in = usage.hasNonNull("prompt_tokens") ? usage.get("prompt_tokens").asInt() : null;
                out = usage.hasNonNull("completion_tokens") ? usage.get("completion_tokens").asInt() : null;
            }
        }
        if (in != null || out != null) {
            return "OK, Tokens=" + (in == null ? "?" : in) + "/"
                    + (out == null ? "?" : out);
        }
        return "OK (keine Token-Angaben geliefert).";
    }

    private static String aufloeseBaseUrl(String provider, String raw) {
        if (raw != null && !raw.isBlank()) {
            return raw.trim();
        }
        return ProviderDefaults.defaultBaseUrl(provider).orElse(null);
    }

    private static String kuerzen(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
