package com.ahs.cvm.persistence.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.ahs.cvm.persistence.AbstractPersistenceIntegrationsTest;
import com.pgvector.PGvector;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Regression fuer den pgvector-Insert (Iteration nach 40, CVM-77).
 *
 * <p>Ohne {@code @JdbcTypeCode(SqlTypes.OTHER)} bindet Hibernate den
 * pgvector-String als {@code VARCHAR}; PostgreSQL wirft dann
 * "column 'embedding' is of type vector but expression is of type
 * character varying". Dieser Test sichert Save + Read + Similarity
 * gegen das echte pgvector-Image.
 */
@EnabledIf(
        value = "com.ahs.cvm.persistence.support.DockerAvailability#isAvailable",
        disabledReason = "Docker-Daemon nicht erreichbar")
class AiEmbeddingPgvectorTest extends AbstractPersistenceIntegrationsTest {

    @Autowired AiEmbeddingRepository repository;
    @Autowired EntityManager em;

    @Test
    @DisplayName("AiEmbedding: Save und Read ueber pgvector-Spalte funktionieren")
    void saveUndReadFunktioniert() {
        float[] vektor = neuerVektor(0.0125f);
        AiEmbedding e = AiEmbedding.builder()
                .documentType("ASSESSMENT")
                .documentRef("assessment:" + UUID.randomUUID())
                .chunkIndex(0)
                .chunkText("demo-chunk")
                .embedding(new PGvector(vektor))
                .modelId("test-embed-1536")
                .build();

        AiEmbedding gespeichert = repository.saveAndFlush(e);
        em.clear();

        AiEmbedding geladen = repository.findById(gespeichert.getId()).orElseThrow();
        assertThat(geladen.getEmbedding()).isNotNull();
        assertThat(geladen.getEmbedding().toArray()).hasSize(1536);
        assertThat(geladen.getDocumentType()).isEqualTo("ASSESSMENT");
    }

    @Test
    @DisplayName("AiEmbedding: findSimilar nutzt pgvector-Cosine-Distance ohne Typfehler")
    void findSimilarLiefertErgebnis() {
        AiEmbedding e = AiEmbedding.builder()
                .documentType("ASSESSMENT")
                .documentRef("assessment:" + UUID.randomUUID())
                .chunkIndex(0)
                .chunkText("alpha")
                .embedding(new PGvector(neuerVektor(0.05f)))
                .modelId("test-embed-1536")
                .build();
        repository.saveAndFlush(e);

        String query = new PGvector(neuerVektor(0.05f)).toString();
        List<Object[]> treffer = repository.findSimilar(
                "ASSESSMENT", "test-embed-1536", query, 1);

        assertThat(treffer).hasSize(1);
    }

    private static float[] neuerVektor(float start) {
        float[] v = new float[1536];
        for (int i = 0; i < v.length; i++) {
            v[i] = start + i * 0.0001f;
        }
        return v;
    }
}
