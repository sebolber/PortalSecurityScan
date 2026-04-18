package com.ahs.cvm.llm.embedding;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Deterministischer Fake-Embedding-Provider fuer Tests und CI.
 * Erzeugt einen festen 1536-dim-Vektor aus dem SHA-256 des Texts:
 * gleicher Text -&gt; gleicher Vektor.
 *
 * <p>Standardmaessig aktiv (keine andere Embedding-Bean da).
 * Sobald ein produktiver Embedding-Adapter (z.B. OllamaEmbedding)
 * eingeschaltet wird, kommt der Fake nicht mehr zum Zug
 * ({@link ConditionalOnMissingBean}).
 */
@Component
@ConditionalOnProperty(prefix = "cvm.llm.embedding", name = "fake",
        havingValue = "true", matchIfMissing = true)
@ConditionalOnMissingBean(name = "ollamaEmbeddingClient")
public class FakeEmbeddingClient implements EmbeddingClient {

    public static final String MODEL_ID = "fake-1536";
    private static final int DIM = 1536;

    @Override
    public EmbeddingResponse embed(String text) {
        if (text == null) {
            text = "";
        }
        float[] vector = embeddingFromHash(text, DIM);
        return new EmbeddingResponse(vector, MODEL_ID, text.length() / 4);
    }

    @Override
    public String modelId() {
        return MODEL_ID;
    }

    @Override
    public int dimensions() {
        return DIM;
    }

    /**
     * Hashbasierte Konstruktion: SHA-256 wird wiederholt verkettet,
     * bis die Zieldimension erreicht ist. Werte werden auf [-1, 1]
     * abgebildet und L2-normalisiert (Cosine-Distance ist auf
     * normalisierten Vektoren stabiler).
     */
    static float[] embeddingFromHash(String text, int dim) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] seed = md.digest(text.getBytes(StandardCharsets.UTF_8));
            float[] out = new float[dim];
            int offset = 0;
            byte[] current = seed;
            while (offset < dim) {
                for (int i = 0; i < current.length && offset < dim; i++) {
                    int signed = current[i];
                    out[offset++] = (signed / 128.0f);
                }
                MessageDigest next = MessageDigest.getInstance("SHA-256");
                next.update(current);
                next.update(seed);
                current = next.digest();
            }
            normalize(out);
            return out;
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 nicht verfuegbar", ex);
        }
    }

    private static void normalize(float[] v) {
        double sum = 0.0;
        for (float f : v) {
            sum += f * f;
        }
        double norm = Math.sqrt(sum);
        if (norm == 0.0) {
            return;
        }
        for (int i = 0; i < v.length; i++) {
            v[i] = (float) (v[i] / norm);
        }
    }
}
