package com.ahs.cvm.llm.embedding;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ahs.cvm.llm.embedding.EmbeddingClient.EmbeddingResponse;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

class OllamaEmbeddingClientTest {

    private WireMockServer wiremock;
    private OllamaEmbeddingClient client;

    @BeforeEach
    void setUp() {
        wiremock = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wiremock.start();
        RestClient rest = RestClient.builder()
                .baseUrl(wiremock.baseUrl())
                .requestFactory(new SimpleClientHttpRequestFactory())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        client = new OllamaEmbeddingClient(rest, "nomic-embed-text");
    }

    @AfterEach
    void tearDown() {
        wiremock.stop();
    }

    @Test
    @DisplayName("OllamaEmbedding: parst /api/embeddings und padded auf 1536 Dimensionen")
    void happyPath() {
        wiremock.stubFor(post(urlEqualTo("/api/embeddings"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "embedding": [0.1, 0.2, 0.3],
                                  "prompt_eval_count": 7
                                }""")));

        EmbeddingResponse r = client.embed("kontext");

        assertThat(r.vector()).hasSize(1536);
        assertThat(r.vector()[0]).isEqualTo(0.1f);
        assertThat(r.vector()[1]).isEqualTo(0.2f);
        assertThat(r.vector()[2]).isEqualTo(0.3f);
        assertThat(r.vector()[3]).isEqualTo(0.0f); // Padding
        assertThat(r.modelId()).isEqualTo("nomic-embed-text");
        assertThat(r.promptTokens()).isEqualTo(7);
    }

    @Test
    @DisplayName("OllamaEmbedding: fehlendes Embedding-Feld wirft")
    void fehlendesFeld() {
        wiremock.stubFor(post(urlEqualTo("/api/embeddings"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"foo\":1}")));

        assertThatThrownBy(() -> client.embed("text"))
                .isInstanceOf(OllamaEmbeddingClient.OllamaEmbeddingException.class);
    }
}
