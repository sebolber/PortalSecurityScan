package com.ahs.cvm.llm.embedding;

/**
 * Abstraktion fuer Embedding-Modelle (Iteration 12, CVM-31).
 *
 * <p>Wie {@link com.ahs.cvm.llm.LlmClient} ist auch dieser Client
 * der einzige offiziell erlaubte Pfad zu einem Embedding-Provider.
 * Audit laeuft ueber dieselbe Audit-Tabelle (use-case
 * {@code "EMBEDDING"}).
 */
public interface EmbeddingClient {

    /**
     * Berechnet ein Embedding fuer den Eingabetext.
     */
    EmbeddingResponse embed(String text);

    /** Modell-Identifier. */
    String modelId();

    /** Anzahl Dimensionen, die das Modell liefert. */
    int dimensions();

    record EmbeddingResponse(float[] vector, String modelId, int promptTokens) {}
}
