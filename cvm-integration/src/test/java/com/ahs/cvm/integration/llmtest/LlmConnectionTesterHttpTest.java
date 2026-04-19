package com.ahs.cvm.integration.llmtest;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.ahs.cvm.application.llmconfig.LlmConfigurationTestCommand;
import com.ahs.cvm.application.llmconfig.LlmConfigurationTestResult;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

class LlmConnectionTesterHttpTest {

    private WireMockServer wireMock;
    private LlmConnectionTesterHttp tester;

    @BeforeEach
    void setUp() {
        wireMock = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMock.start();
        // Der RestClient wird pro Call mit der baseUrl aus dem Command
        // gebaut, weil die verschiedenen Provider unterschiedliche URLs
        // bekommen sollen.
        tester = new LlmConnectionTesterHttp(baseUrl -> RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(new SimpleClientHttpRequestFactory())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build());
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    private String baseUrl() {
        return "http://localhost:" + wireMock.port();
    }

    @Test
    @DisplayName("Anthropic: 200er liefert success=true mit Token-Zusammenfassung")
    void anthropicHappyPath() {
        wireMock.stubFor(post(urlPathEqualTo("/v1/messages"))
                .withHeader("x-api-key", equalTo("sk-test"))
                .withHeader("anthropic-version", equalTo("2023-06-01"))
                .withRequestBody(matchingJsonPath("$.model", equalTo("claude-sonnet-4-6")))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"usage\":{\"input_tokens\":3,\"output_tokens\":1}}")));

        LlmConfigurationTestResult r = tester.test(new LlmConfigurationTestCommand(
                null, "anthropic", "claude-sonnet-4-6",
                baseUrl(), "sk-test"));

        assertThat(r.success()).isTrue();
        assertThat(r.httpStatus()).isEqualTo(200);
        assertThat(r.message()).contains("Tokens=3/1");
        assertThat(r.provider()).isEqualTo("anthropic");
    }

    @Test
    @DisplayName("OpenAI: Bearer-Token wird gesetzt, erfolgreiche Antwort geparst")
    void openAiHappyPath() {
        wireMock.stubFor(post(urlPathEqualTo("/chat/completions"))
                .withHeader("Authorization", equalTo("Bearer sk-abc"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"usage\":{\"prompt_tokens\":2,\"completion_tokens\":1}}")));

        LlmConfigurationTestResult r = tester.test(new LlmConfigurationTestCommand(
                null, "openai", "gpt-4o-mini", baseUrl(), "sk-abc"));

        assertThat(r.success()).isTrue();
        assertThat(r.message()).contains("Tokens=2/1");
    }

    @Test
    @DisplayName("Azure: api-key-Header statt Bearer")
    void azureHappyPath() {
        wireMock.stubFor(post(urlPathEqualTo("/chat/completions"))
                .withHeader("api-key", equalTo("azure-key"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{}")));

        LlmConfigurationTestResult r = tester.test(new LlmConfigurationTestCommand(
                null, "azure", "gpt-4o", baseUrl(), "azure-key"));

        assertThat(r.success()).isTrue();
        assertThat(r.provider()).isEqualTo("azure");
    }

    @Test
    @DisplayName("Ollama: ohne API-Key erfolgreich, /api/chat wird angesprochen")
    void ollamaHappyPathOhneKey() {
        wireMock.stubFor(post(urlPathEqualTo("/api/chat"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"prompt_eval_count\":4,\"eval_count\":1}")));

        LlmConfigurationTestResult r = tester.test(new LlmConfigurationTestCommand(
                null, "ollama", "llama3", baseUrl(), null));

        assertThat(r.success()).isTrue();
        assertThat(r.message()).contains("Tokens=4/1");
    }

    @Test
    @DisplayName("HTTP 401 -> success=false, Statuscode und Body-Ausschnitt im Message")
    void httpFehler401() {
        wireMock.stubFor(post(urlPathEqualTo("/chat/completions"))
                .willReturn(aResponse().withStatus(401)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"invalid_api_key\"}")));

        LlmConfigurationTestResult r = tester.test(new LlmConfigurationTestCommand(
                null, "openai", "gpt-4o", baseUrl(), "wrong"));

        assertThat(r.success()).isFalse();
        assertThat(r.httpStatus()).isEqualTo(401);
        assertThat(r.message()).contains("HTTP 401");
    }

    @Test
    @DisplayName("Fehlender API-Key bei Cloud-Provider -> failure ohne HTTP-Call")
    void cloudProviderOhneKey() {
        LlmConfigurationTestResult r = tester.test(new LlmConfigurationTestCommand(
                null, "openai", "gpt-4o", baseUrl(), null));

        assertThat(r.success()).isFalse();
        assertThat(r.message()).contains("API-Key");
        assertThat(wireMock.findAllUnmatchedRequests()).isEmpty();
    }

    @Test
    @DisplayName("Unbekannter Provider -> failure")
    void unbekannterProvider() {
        LlmConfigurationTestResult r = tester.test(new LlmConfigurationTestCommand(
                UUID.randomUUID(), "mistral-custom", "mistral-7b",
                baseUrl(), "key"));

        assertThat(r.success()).isFalse();
        assertThat(r.message()).contains("Unbekannter Provider");
    }

    @Test
    @DisplayName("Modell fehlt -> failure ohne HTTP-Call")
    void modellFehlt() {
        LlmConfigurationTestResult r = tester.test(new LlmConfigurationTestCommand(
                null, "openai", "", baseUrl(), "key"));

        assertThat(r.success()).isFalse();
        assertThat(r.message()).contains("Modell");
    }
}
