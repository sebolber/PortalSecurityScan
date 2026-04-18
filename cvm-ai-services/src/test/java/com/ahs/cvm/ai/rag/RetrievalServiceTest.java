package com.ahs.cvm.ai.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.ahs.cvm.llm.embedding.FakeEmbeddingClient;
import com.ahs.cvm.persistence.ai.AiEmbeddingRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RetrievalServiceTest {

    private final FakeEmbeddingClient client = new FakeEmbeddingClient();
    private final AiEmbeddingRepository repo = mock(AiEmbeddingRepository.class);
    private final RetrievalService service = new RetrievalService(client, repo);

    @Test
    @DisplayName("Retrieval: liefert sortierte Treffer mit Score-Mapping")
    void treffer() {
        Object[] r1 = row("a", 0.92);
        Object[] r2 = row("b", 0.81);
        given(repo.findSimilar(eq("ASSESSMENT"), eq(FakeEmbeddingClient.MODEL_ID),
                        any(String.class), anyInt()))
                .willReturn(List.of(r1, r2));

        var hits = service.similar("ASSESSMENT", "Bewerte CVE", 5);

        assertThat(hits).hasSize(2);
        assertThat(hits.get(0).chunkText()).isEqualTo("a");
        assertThat(hits.get(0).score()).isEqualTo(0.92);
        assertThat(hits.get(1).score()).isEqualTo(0.81);
    }

    @Test
    @DisplayName("Retrieval: filtert auf model_id (kein Mischen verschiedener Modelle)")
    void modelFilter() {
        ArgumentCaptor<String> modelCap = ArgumentCaptor.forClass(String.class);
        given(repo.findSimilar(any(), modelCap.capture(), any(), anyInt()))
                .willReturn(List.of());

        service.similar("ADVISORY", "Inhalt", 3);

        assertThat(modelCap.getValue()).isEqualTo(FakeEmbeddingClient.MODEL_ID);
    }

    @Test
    @DisplayName("Retrieval: topK <= 0 wirft IllegalArgumentException")
    void topkUngueltig() {
        assertThatThrownBy(() -> service.similar("ASSESSMENT", "x", 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Retrieval: toVectorLiteral baut pgvector-konforme Eingabe")
    void vectorLiteral() {
        String s = RetrievalService.toVectorLiteral(new float[]{0.5f, -0.25f});
        assertThat(s).isEqualTo("[0.5,-0.25]");
    }

    private Object[] row(String chunk, double score) {
        return new Object[]{
                UUID.randomUUID(), "ASSESSMENT", "ref", 0, chunk,
                null, FakeEmbeddingClient.MODEL_ID,
                Instant.parse("2026-04-18T10:00:00Z"), score};
    }
}
