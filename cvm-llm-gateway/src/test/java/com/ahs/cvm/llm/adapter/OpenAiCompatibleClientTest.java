package com.ahs.cvm.llm.adapter;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ahs.cvm.llm.LlmClient.LlmRequest;
import com.ahs.cvm.llm.LlmClient.LlmResponse;
import com.ahs.cvm.llm.LlmClient.Message;
import com.ahs.cvm.llm.TenantLlmSettings;
import com.ahs.cvm.llm.TenantLlmSettingsProvider;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OpenAiCompatibleClientTest {

    private WireMockServer wiremock;

    @BeforeEach
    void setUp() {
        wiremock = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wiremock.start();
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
                512,
                null,
                "t.tester@ahs.test",
                null,
                Map.of());
    }

    @Test
    @DisplayName("OpenAI: Bearer-Header + chat/completions mit json_object")
    void openaiHappyPath() {
        wiremock.stubFor(post(urlEqualTo("/chat/completions"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "model": "gpt-4o",
                                  "choices": [
                                    {"message": {"content": "{\\"severity\\":\\"LOW\\"}"}}
                                  ],
                                  "usage": {"prompt_tokens": 11, "completion_tokens": 4}
                                }""")));

        TenantLlmSettingsProvider provider = () -> Optional.of(
                new TenantLlmSettings("openai", "gpt-4o",
                        wiremock.baseUrl(), "sk-geheim"));
        OpenAiCompatibleClient client = new OpenAiCompatibleClient(
                "gpt-4o-mini", provider);

        LlmResponse resp = client.complete(request());
        assertThat(resp.structuredOutput().path("severity").asText()).isEqualTo("LOW");
        assertThat(resp.usage().promptTokens()).isEqualTo(11);
        assertThat(resp.modelId()).isEqualTo("gpt-4o");
        assertThat(wiremock.findAll(postRequestedFor(urlEqualTo("/chat/completions")))
                .get(0).getHeader("Authorization")).isEqualTo("Bearer sk-geheim");
    }

    @Test
    @DisplayName("Azure: api-key-Header statt Bearer")
    void azureApiKeyHeader() {
        wiremock.stubFor(post(urlEqualTo("/chat/completions"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"choices":[{"message":{"content":"{\\"ok\\":true}"}}]}""")));

        TenantLlmSettingsProvider provider = () -> Optional.of(
                new TenantLlmSettings("azure", "gpt-4o-deployment",
                        wiremock.baseUrl(), "AZURE-KEY"));
        OpenAiCompatibleClient client = new OpenAiCompatibleClient(
                "gpt-4o-mini", provider);

        client.complete(request());
        var req = wiremock.findAll(postRequestedFor(urlEqualTo("/chat/completions"))).get(0);
        assertThat(req.getHeader("api-key")).isEqualTo("AZURE-KEY");
        assertThat(req.containsHeader("Authorization")).isFalse();
    }

    @Test
    @DisplayName("adesso-ai-hub: Bearer-Header, baseUrl wird uebernommen")
    void adessoAiHub() {
        wiremock.stubFor(post(urlEqualTo("/chat/completions"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"choices\":[{\"message\":{\"content\":\"{\\\"x\\\":1}\"}}]}")));

        TenantLlmSettingsProvider provider = () -> Optional.of(
                new TenantLlmSettings("adesso-ai-hub", "modell-1",
                        wiremock.baseUrl(), "hub-key"));
        OpenAiCompatibleClient client = new OpenAiCompatibleClient(
                "gpt-4o-mini", provider);

        LlmResponse resp = client.complete(request());
        assertThat(resp.structuredOutput().path("x").asInt()).isEqualTo(1);
    }

    @Test
    @DisplayName("Fehlendes Tenant-Setting wirft OpenAiException")
    void ohneTenantException() {
        TenantLlmSettingsProvider provider = Optional::empty;
        OpenAiCompatibleClient client = new OpenAiCompatibleClient(
                "gpt-4o-mini", provider);
        assertThatThrownBy(() -> client.complete(request()))
                .isInstanceOf(OpenAiCompatibleClient.OpenAiException.class);
    }

    @Test
    @DisplayName("supportsProvider deckt openai/azure/adesso-ai-hub ab, sonst nicht")
    void supportsProviderMultimatch() {
        OpenAiCompatibleClient client = new OpenAiCompatibleClient(
                "gpt-4o-mini", Optional.empty());
        assertThat(client.supportsProvider("openai")).isTrue();
        assertThat(client.supportsProvider("azure")).isTrue();
        assertThat(client.supportsProvider("adesso-ai-hub")).isTrue();
        assertThat(client.supportsProvider("ollama")).isFalse();
        assertThat(client.supportsProvider(null)).isFalse();
    }
}
