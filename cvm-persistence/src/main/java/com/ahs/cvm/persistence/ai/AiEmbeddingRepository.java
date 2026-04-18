package com.ahs.cvm.persistence.ai;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AiEmbeddingRepository extends JpaRepository<AiEmbedding, UUID> {

    List<AiEmbedding> findByDocumentTypeAndDocumentRefOrderByChunkIndexAsc(
            String documentType, String documentRef);

    long countByDocumentType(String documentType);

    @Modifying
    @Query("DELETE FROM AiEmbedding e WHERE e.documentType = :type AND e.documentRef = :ref")
    int deleteByDocumentTypeAndDocumentRef(
            @Param("type") String documentType,
            @Param("ref") String documentRef);

    /**
     * Native Cosine-Similarity-Query mit pgvector. Liefert die
     * {@code topK} naechstgelegenen Chunks zum gegebenen Embedding,
     * gefiltert nach {@code document_type} und {@code model_id}.
     *
     * <p>Die Vektor-Eingabe wird im pgvector-Stringformat
     * {@code "[0.1,0.2,...]"} uebergeben (Treiber-Konversion). Der
     * Operator {@code <=>} ist die Cosine-Distance; kleiner = besser.
     */
    @Query(value = """
            SELECT id, document_type, document_ref, chunk_index, chunk_text,
                   embedding, model_id, created_at,
                   (1.0 - (embedding <=> CAST(:queryVector AS vector))) AS score
            FROM ai_embedding
            WHERE document_type = :documentType
              AND model_id = :modelId
            ORDER BY embedding <=> CAST(:queryVector AS vector)
            LIMIT :topK
            """, nativeQuery = true)
    List<Object[]> findSimilar(
            @Param("documentType") String documentType,
            @Param("modelId") String modelId,
            @Param("queryVector") String queryVector,
            @Param("topK") int topK);
}
