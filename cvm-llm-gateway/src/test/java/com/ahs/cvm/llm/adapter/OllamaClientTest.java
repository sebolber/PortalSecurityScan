package com.ahs.cvm.llm.adapter;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ahs.cvm.llm.LlmClient.LlmRequest;
import com.ahs.cvm.llm.LlmClient.LlmResponse;
import com.ahs.cvm.llm.LlmClient.Message;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

class OllamaClientTest {

    private WireMockServer wiremock;
    private OllamaClient client;

    @BeforeEach
    void setUp() {
        wiremock = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wiremock.start();
        RestClient rest = RestClient.builder()
                .baseUrl(wiremock.baseUrl())
                .requestFactory(new SimpleClientHttpRequestFactory())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        client = new OllamaClient(rest, "llama3.1:8b-instruct");
    }

    @AfterEach
    void tearDown() {
        wiremock.stop();
    }

    private LlmRequest request() {
        return new LlmRequest(
                "auto-assessment",
                "assessment.propose",
                "v1",
                "system",
                List.of(new Message(Message.Role.USER, "fragen")),
                null,
                0.1,
                1024,
                null,
                "t.tester@ahs.test",
                null,
                Map.of());
    }

    @Test
    @DisplayName("Ollama: Happy-Path liefert JSON aus message.content")
    void happyPath() {
        wiremock.stubFor(post(urlEqualTo("/api/chat"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "model": "llama3.1:8b-instruct",
                                  "message": {"role":"assistant","content":"{\\"severity\\":\\"HIGH\\"}"},
                                  "prompt_eval_count": 10,
                                  "eval_count": 4
                                }""")));

        LlmResponse resp = client.complete(request());

        assertThat(resp.rawText()).contains("HIGH");
        assertThat(resp.usage().promptTokens()).isEqualTo(10);
        assertThat(resp.usage().completionTokens()).isEqualTo(4);
        assertThat(resp.modelId()).isEqualTo("llama3.1:8b-instruct");
    }

    @Test
    @DisplayName("Ollama: HTTP-Fehler propagiert als RestClientException")
    void fehler() {
        wiremock.stubFor(post(urlEqualTo("/api/chat"))
                .willReturn(aResponse().withStatus(502).withBody("down")));

        assertThatThrownBy(() -> client.complete(request()))
                .isInstanceOf(RestClientException.class);
    }
}
