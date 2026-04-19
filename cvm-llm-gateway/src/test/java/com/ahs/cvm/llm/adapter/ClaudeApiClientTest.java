package com.ahs.cvm.llm.adapter;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ahs.cvm.llm.LlmClient;
import com.ahs.cvm.llm.LlmClient.LlmRequest;
import com.ahs.cvm.llm.LlmClient.LlmResponse;
import com.ahs.cvm.llm.LlmClient.Message;
import com.ahs.cvm.llm.TenantLlmSettings;
import com.ahs.cvm.llm.TenantLlmSettingsProvider;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

class ClaudeApiClientTest {

    private WireMockServer wiremock;
    private ClaudeApiClient client;

    @BeforeEach
    void setUp() {
        wiremock = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wiremock.start();
        RestClient rest = RestClient.builder()
                .baseUrl(wiremock.baseUrl())
                .requestFactory(new SimpleClientHttpRequestFactory())
                .defaultHeader("anthropic-version", "2023-06-01")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        client = new ClaudeApiClient(rest, "test-key", "claude-sonnet-4-6");
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
                "System-Prompt",
                List.of(new Message(Message.Role.USER, "Hallo")),
                null,
                0.1,
                1024,
                null,
                "t.tester@ahs.test",
                null,
                Map.of());
    }

    @Test
    @DisplayName("ClaudeApi: Happy-Path liefert strukturiertes JSON und Token-Usage")
    void happyPath() {
        wiremock.stubFor(post(urlEqualTo("/v1/messages"))
                .withRequestBody(matchingJsonPath("$.model"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "id": "msg-1",
                                  "model": "claude-sonnet-4-6",
                                  "content": [
                                    {"type":"text","text":"{\\"severity\\":\\"LOW\\"}"}
                                  ],
                                  "usage": {"input_tokens": 42, "output_tokens": 7}
                                }""")));

        LlmResponse resp = client.complete(request());

        assertThat(resp.rawText()).contains("LOW");
        assertThat(resp.structuredOutput().path("severity").asText()).isEqualTo("LOW");
        assertThat(resp.usage().promptTokens()).isEqualTo(42);
        assertThat(resp.usage().completionTokens()).isEqualTo(7);
        assertThat(resp.modelId()).isEqualTo("claude-sonnet-4-6");
    }

    @Test
    @DisplayName("ClaudeApi: 429 Too Many Requests wirft RestClientException")
    void rateLimited() {
        wiremock.stubFor(post(urlEqualTo("/v1/messages"))
                .willReturn(aResponse()
                        .withStatus(429)
                        .withBody("rate limit")));

        assertThatThrownBy(() -> client.complete(request()))
                .isInstanceOf(RestClientException.class);
    }

    @Test
    @DisplayName("ClaudeApi: 500 wirft RestClientException")
    void serverError() {
        wiremock.stubFor(post(urlEqualTo("/v1/messages"))
                .willReturn(aResponse().withStatus(500).withBody("down")));

        assertThatThrownBy(() -> client.complete(request()))
                .isInstanceOf(RestClientException.class);
    }

    @Test
    @DisplayName("ClaudeApi: Tenant-Override zieht Modell und API-Key aus TenantLlmSettings")
    void tenantOverride() {
        WireMockServer tenantWiremock = new WireMockServer(
                WireMockConfiguration.options().dynamicPort());
        tenantWiremock.start();
        try {
            tenantWiremock.stubFor(post(urlEqualTo("/v1/messages"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("""
                                    {
                                      "id": "msg-2",
                                      "model": "claude-opus-4-7",
                                      "content": [
                                        {"type":"text","text":"{\\"severity\\":\\"HIGH\\"}"}
                                      ],
                                      "usage": {"input_tokens": 10, "output_tokens": 5}
                                    }""")));

            TenantLlmSettingsProvider provider = () -> Optional.of(
                    new TenantLlmSettings(
                            "anthropic",
                            "claude-opus-4-7",
                            tenantWiremock.baseUrl(),
                            "tenant-key-xyz"));
            RestClient rest = RestClient.builder()
                    .baseUrl(wiremock.baseUrl())
                    .requestFactory(new SimpleClientHttpRequestFactory())
                    .defaultHeader("anthropic-version", "2023-06-01")
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .build();
            ClaudeApiClient tenantAware = new ClaudeApiClient(
                    rest, "default-key", "claude-sonnet-4-6", Optional.of(provider));

            LlmResponse resp = tenantAware.complete(request());

            // Antwort kam vom Tenant-Server, nicht vom Default.
            assertThat(resp.modelId()).isEqualTo("claude-opus-4-7");
            assertThat(tenantWiremock.findAll(
                    WireMock.postRequestedFor(urlEqualTo("/v1/messages"))))
                    .hasSize(1);
            assertThat(tenantWiremock.findAll(
                            WireMock.postRequestedFor(urlEqualTo("/v1/messages")))
                    .get(0).getHeader("x-api-key"))
                    .isEqualTo("tenant-key-xyz");
        } finally {
            tenantWiremock.stop();
        }
    }

    @Test
    @DisplayName("ClaudeApi: ungueltiger JSON-Body wirft ClaudeApiException")
    void kaputteAntwort() {
        wiremock.stubFor(post(urlEqualTo("/v1/messages"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "content": [
                                    {"type":"text","text":"not-json"}
                                  ]
                                }""")));

        assertThatThrownBy(() -> client.complete(request()))
                .isInstanceOf(ClaudeApiClient.ClaudeApiException.class);
    }
}
