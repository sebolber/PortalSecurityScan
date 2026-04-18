package com.ahs.cvm.persistence.ai;

import com.pgvector.PGvector;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Persistierter Embedding-Chunk fuer die RAG-Suche (Iteration 12,
 * CVM-31).
 *
 * <p>{@code embedding} wird ueber den
 * {@link com.pgvector.PGvector}-Typ gemappt, der pgvector-Strings
 * (`[0.1,0.2,...]`) liest und schreibt. Hibernate erkennt den Typ
 * via Standard-Reflection des PG-Treibers.
 */
@Entity
@Table(name = "ai_embedding")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AiEmbedding {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "document_type", nullable = false, updatable = false)
    private String documentType;

    @Column(name = "document_ref", nullable = false, updatable = false)
    private String documentRef;

    @Column(name = "chunk_index", nullable = false, updatable = false)
    private Integer chunkIndex;

    @Column(name = "chunk_text", nullable = false, updatable = false, columnDefinition = "text")
    private String chunkText;

    @Column(name = "embedding", nullable = false, updatable = false,
            columnDefinition = "vector(1536)")
    @Convert(converter = PGvectorConverter.class)
    private PGvector embedding;

    @Column(name = "model_id", nullable = false, updatable = false)
    private String modelId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void initialisiere() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (chunkIndex == null) {
            chunkIndex = 0;
        }
    }
}
