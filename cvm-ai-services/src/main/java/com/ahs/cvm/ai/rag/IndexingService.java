package com.ahs.cvm.ai.rag;

import com.ahs.cvm.llm.embedding.EmbeddingClient;
import com.ahs.cvm.llm.embedding.EmbeddingClient.EmbeddingResponse;
import com.ahs.cvm.persistence.ai.AiEmbedding;
import com.ahs.cvm.persistence.ai.AiEmbeddingRepository;
import com.ahs.cvm.persistence.assessment.Assessment;
import com.ahs.cvm.persistence.assessment.AssessmentRepository;
import com.ahs.cvm.persistence.cve.Cve;
import com.ahs.cvm.persistence.cve.CveRepository;
import com.pgvector.PGvector;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Indiziert Dokumente fuer die RAG-Suche (Iteration 12, CVM-31).
 *
 * <ul>
 *   <li>{@link #indexAssessment(UUID)} - bei
 *       {@code AssessmentApprovedEvent}.</li>
 *   <li>{@link #indexAdvisory(UUID)} - bei CVE-Anreicherung
 *       (Iteration 03).</li>
 *   <li>{@link #indexAll()} - initialer Aufbau / Re-Index.</li>
 * </ul>
 *
 * <p>Duplikate werden vermieden, indem vor dem Schreiben alle
 * vorhandenen Chunks fuer die gleiche {@code (document_type,
 * document_ref)}-Kombination geloescht werden.
 */
@Service
public class IndexingService {

    private static final Logger log = LoggerFactory.getLogger(IndexingService.class);

    public static final String TYPE_ASSESSMENT = "ASSESSMENT";
    public static final String TYPE_ADVISORY = "ADVISORY";

    private final EmbeddingClient embeddingClient;
    private final Chunker chunker;
    private final AiEmbeddingRepository embeddingRepository;
    private final AssessmentRepository assessmentRepository;
    private final CveRepository cveRepository;

    public IndexingService(
            EmbeddingClient embeddingClient,
            Chunker chunker,
            AiEmbeddingRepository embeddingRepository,
            AssessmentRepository assessmentRepository,
            CveRepository cveRepository) {
        this.embeddingClient = embeddingClient;
        this.chunker = chunker;
        this.embeddingRepository = embeddingRepository;
        this.assessmentRepository = assessmentRepository;
        this.cveRepository = cveRepository;
    }

    @Transactional
    public int indexAssessment(UUID assessmentId) {
        Assessment a = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Assessment nicht gefunden: " + assessmentId));
        String text = baueAssessmentText(a);
        return indexDokument(TYPE_ASSESSMENT, "assessment:" + assessmentId, text);
    }

    @Transactional
    public int indexAdvisory(UUID cveId) {
        Cve cve = cveRepository.findById(cveId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Cve nicht gefunden: " + cveId));
        String text = baueAdvisoryText(cve);
        return indexDokument(TYPE_ADVISORY, "cve:" + cve.getCveId(), text);
    }

    /**
     * Voller Re-Index. Aktuell laeuft das ueber {@code findAll()};
     * fuer realistische Datenmengen sollte spaeter ein
     * Streaming-/Pageable-Verfahren her.
     */
    @Transactional
    public int indexAll() {
        int summe = 0;
        for (Assessment a : assessmentRepository.findAll()) {
            if (a.getSupersededAt() == null) {
                summe += indexAssessment(a.getId());
            }
        }
        for (Cve cve : cveRepository.findAll()) {
            summe += indexAdvisory(cve.getId());
        }
        log.info("RAG-ReIndex abgeschlossen: {} Chunks geschrieben.", summe);
        return summe;
    }

    int indexDokument(String type, String ref, String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        List<String> chunks = chunker.split(text);
        embeddingRepository.deleteByDocumentTypeAndDocumentRef(type, ref);
        int idx = 0;
        for (String chunk : chunks) {
            EmbeddingResponse resp = embeddingClient.embed(chunk);
            embeddingRepository.save(AiEmbedding.builder()
                    .documentType(type)
                    .documentRef(ref)
                    .chunkIndex(idx++)
                    .chunkText(chunk)
                    .embedding(new PGvector(resp.vector()))
                    .modelId(resp.modelId())
                    .build());
        }
        log.debug("RAG: {} Chunks fuer {} indiziert.", chunks.size(), ref);
        return chunks.size();
    }

    String baueAssessmentText(Assessment a) {
        StringBuilder sb = new StringBuilder();
        sb.append("Assessment ").append(a.getId()).append('\n');
        sb.append("CVE: ").append(a.getCve().getCveId()).append('\n');
        sb.append("Severity: ").append(a.getSeverity()).append('\n');
        sb.append("Source: ").append(a.getProposalSource()).append('\n');
        if (a.getRationale() != null) {
            sb.append("Rationale: ").append(a.getRationale()).append('\n');
        }
        if (a.getRationaleSourceFields() != null) {
            sb.append("Quellfelder: ")
                    .append(String.join(", ", a.getRationaleSourceFields()))
                    .append('\n');
        }
        return sb.toString();
    }

    String baueAdvisoryText(Cve cve) {
        StringBuilder sb = new StringBuilder();
        sb.append("CVE: ").append(cve.getCveId()).append('\n');
        if (cve.getSummary() != null) {
            sb.append("Summary: ").append(cve.getSummary()).append('\n');
        }
        if (cve.getCvssBaseScore() != null) {
            sb.append("CVSS: ").append(cve.getCvssBaseScore()).append('\n');
        }
        if (cve.getCwes() != null && !cve.getCwes().isEmpty()) {
            sb.append("CWE: ").append(String.join(", ", cve.getCwes())).append('\n');
        }
        if (Boolean.TRUE.equals(cve.getKevListed())) {
            sb.append("KEV: ja\n");
        }
        return sb.toString();
    }
}
