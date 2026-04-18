package com.ahs.cvm.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ahs.cvm.llm.LlmClient.LlmRequest;
import com.ahs.cvm.llm.LlmClient.LlmResponse;
import com.ahs.cvm.llm.LlmClientSelector.NoLlmClientForModelException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LlmClientSelectorTest {

    private final LlmGatewayConfig config = new LlmGatewayConfig(
            true, "warn", "claude-sonnet-4-6", 120, 30);

    @Test
    @DisplayName("Selector: waehlt Modell aus Resolver (Umgebung Y -> Ollama)")
    void waehltPerResolver() {
        UUID envId = UUID.fromString("00000000-0000-0000-0000-0000000000bb");
        LlmClient claude = fakeClient("claude-sonnet-4-6");
        LlmClient ollama = fakeClient("llama3.1:8b-instruct");
        LlmClientSelector selector = new LlmClientSelector(
                List.of(claude, ollama),
                Optional.of((env, uc) -> envId.equals(env)
                        ? "llama3.1:8b-instruct" : "claude-sonnet-4-6"),
                config);

        assertThat(selector.select(envId, "auto-assessment").modelId())
                .isEqualTo("llama3.1:8b-instruct");
        assertThat(selector.select(UUID.randomUUID(), "auto-assessment").modelId())
                .isEqualTo("claude-sonnet-4-6");
    }

    @Test
    @DisplayName("Selector: unbekanntes Modell wirft NoLlmClientForModelException")
    void unbekanntesModell() {
        LlmClient claude = fakeClient("claude-sonnet-4-6");
        LlmClientSelector selector = new LlmClientSelector(
                List.of(claude),
                Optional.of((env, uc) -> "mistral-medium"),
                config);

        assertThatThrownBy(() -> selector.select(null, "x"))
                .isInstanceOf(NoLlmClientForModelException.class);
    }

    @Test
    @DisplayName("Selector: Default-Resolver liefert config.defaultModel()")
    void defaultResolver() {
        LlmClient claude = fakeClient("claude-sonnet-4-6");
        LlmClientSelector selector = new LlmClientSelector(
                List.of(claude), Optional.empty(), config);
        assertThat(selector.select(null, "x").modelId()).isEqualTo("claude-sonnet-4-6");
    }

    private LlmClient fakeClient(String modelId) {
        return new LlmClient() {
            @Override
            public LlmResponse complete(LlmRequest request) {
                throw new UnsupportedOperationException();
            }

            @Override
            public String modelId() {
                return modelId;
            }
        };
    }
}
