package com.ahs.cvm.llm.embedding;

import static org.assertj.core.api.Assertions.assertThat;

import com.ahs.cvm.llm.embedding.EmbeddingClient.EmbeddingResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FakeEmbeddingClientTest {

    private final FakeEmbeddingClient client = new FakeEmbeddingClient();

    @Test
    @DisplayName("FakeEmbedding: liefert 1536 Dimensionen und feste Modell-Id")
    void dimensionen() {
        EmbeddingResponse r = client.embed("hallo welt");
        assertThat(r.vector()).hasSize(1536);
        assertThat(r.modelId()).isEqualTo(FakeEmbeddingClient.MODEL_ID);
    }

    @Test
    @DisplayName("FakeEmbedding: gleicher Text -> gleicher Vektor (deterministisch)")
    void deterministisch() {
        float[] a = client.embed("CVE-2025-48924").vector();
        float[] b = client.embed("CVE-2025-48924").vector();
        assertThat(a).isEqualTo(b);
    }

    @Test
    @DisplayName("FakeEmbedding: unterschiedliche Texte -> unterschiedliche Vektoren")
    void unterschiedlich() {
        float[] a = client.embed("foo").vector();
        float[] b = client.embed("bar").vector();
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    @DisplayName("FakeEmbedding: Vektor ist L2-normalisiert")
    void normalisiert() {
        float[] v = client.embed("normalisierungstest").vector();
        double sumSquares = 0.0;
        for (float f : v) {
            sumSquares += f * f;
        }
        assertThat(Math.sqrt(sumSquares)).isCloseTo(1.0, org.assertj.core.data.Offset.offset(0.001));
    }

    @Test
    @DisplayName("FakeEmbedding: null-Eingabe wird wie Leerstring behandelt")
    void nullEingabe() {
        EmbeddingResponse r = client.embed(null);
        assertThat(r.vector()).hasSize(1536);
        assertThat(r.promptTokens()).isZero();
    }
}
