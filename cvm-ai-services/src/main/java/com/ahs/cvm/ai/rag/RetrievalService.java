package com.ahs.cvm.ai.rag;

import com.ahs.cvm.llm.embedding.EmbeddingClient;
import com.ahs.cvm.llm.embedding.EmbeddingClient.EmbeddingResponse;
import com.ahs.cvm.persistence.ai.AiEmbeddingRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cosine-Similarity-Suche ueber {@code ai_embedding} (Iteration 12).
 *
 * <p>Der Service belegt die Invariante "kein Mischen von Modellen
 * in einer Suche" durch eine Filterklausel auf {@code model_id}; die
 * Modell-Id wird vom uebergebenen {@link EmbeddingClient} gestellt.
 */
@Service
public class RetrievalService {

    private final EmbeddingClient embeddingClient;
    private final AiEmbeddingRepository repository;

    public RetrievalService(EmbeddingClient embeddingClient, AiEmbeddingRepository repository) {
        this.embeddingClient = embeddingClient;
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<RagHit> similar(String documentType, String queryText, int topK) {
        if (topK <= 0) {
            throw new IllegalArgumentException("topK muss > 0 sein.");
        }
        EmbeddingResponse query = embeddingClient.embed(queryText);
        String vectorString = toVectorLiteral(query.vector());
        List<Object[]> rows = repository.findSimilar(
                documentType, embeddingClient.modelId(), vectorString, topK);
        return rows.stream()
                .map(RetrievalService::mapRow)
                .toList();
    }

    static String toVectorLiteral(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(vector[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    static RagHit mapRow(Object[] row) {
        return new RagHit(
                (UUID) row[0],
                (String) row[1],
                (String) row[2],
                ((Number) row[3]).intValue(),
                (String) row[4],
                (String) row[6],
                row[7] instanceof Instant i ? i : null,
                ((Number) row[8]).doubleValue());
    }

    public record RagHit(
            UUID id,
            String documentType,
            String documentRef,
            int chunkIndex,
            String chunkText,
            String modelId,
            Instant createdAt,
            double score) {}
}
