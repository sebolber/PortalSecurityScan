package com.ahs.cvm.llm.embedding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Optionaler echter Embedding-Adapter gegen Ollama
 * ({@code POST /api/embeddings}). Aktivierung ueber
 * {@code cvm.llm.embedding.fake=false} und implizit, sobald die
 * Bean explizit benoetigt wird.
 *
 * <p>Pad/Truncate auf 1536 Dimensionen, damit alle persistierten
 * Embeddings den DB-Spaltentyp passen.
 */
@Component("ollamaEmbeddingClient")
@ConditionalOnProperty(prefix = "cvm.llm.embedding", name = "fake",
        havingValue = "false")
public class OllamaEmbeddingClient implements EmbeddingClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int TARGET_DIM = 1536;

    private final RestClient restClient;
    private final String model;

    @Autowired
    public OllamaEmbeddingClient(
            @Value("${cvm.llm.embedding.ollama.base-url:http://ollama:11434}") String baseUrl,
            @Value("${cvm.llm.embedding.ollama.model:nomic-embed-text}") String model) {
        this.model = model;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public OllamaEmbeddingClient(RestClient restClient, String model) {
        this.restClient = restClient;
        this.model = model;
    }

    @Override
    public EmbeddingResponse embed(String text) {
        ObjectNode body = MAPPER.createObjectNode();
        body.put("model", model);
        body.put("prompt", text == null ? "" : text);
        JsonNode response = restClient.post()
                .uri("/api/embeddings")
                .body(body)
                .retrieve()
                .body(JsonNode.class);
        if (response == null || !response.has("embedding")) {
            throw new OllamaEmbeddingException(
                    "Antwort enthaelt kein 'embedding'-Feld.");
        }
        JsonNode arr = response.get("embedding");
        float[] vector = new float[TARGET_DIM];
        int n = Math.min(arr.size(), TARGET_DIM);
        for (int i = 0; i < n; i++) {
            vector[i] = (float) arr.get(i).asDouble();
        }
        // Restliche Dimensionen bleiben 0.0f (Padding).
        return new EmbeddingResponse(vector, model, response.path("prompt_eval_count").asInt(0));
    }

    @Override
    public String modelId() {
        return model;
    }

    @Override
    public int dimensions() {
        return TARGET_DIM;
    }

    public static class OllamaEmbeddingException extends RuntimeException {
        public OllamaEmbeddingException(String message) {
            super(message);
        }
    }
}
