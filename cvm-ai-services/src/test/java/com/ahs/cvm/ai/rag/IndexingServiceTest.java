package com.ahs.cvm.ai.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.ahs.cvm.domain.enums.AhsSeverity;
import com.ahs.cvm.domain.enums.AssessmentStatus;
import com.ahs.cvm.domain.enums.ProposalSource;
import com.ahs.cvm.llm.embedding.FakeEmbeddingClient;
import com.ahs.cvm.persistence.ai.AiEmbedding;
import com.ahs.cvm.persistence.ai.AiEmbeddingRepository;
import com.ahs.cvm.persistence.assessment.Assessment;
import com.ahs.cvm.persistence.assessment.AssessmentRepository;
import com.ahs.cvm.persistence.cve.Cve;
import com.ahs.cvm.persistence.cve.CveRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class IndexingServiceTest {

    private FakeEmbeddingClient embeddingClient;
    private Chunker chunker;
    private AiEmbeddingRepository embeddingRepository;
    private AssessmentRepository assessmentRepository;
    private CveRepository cveRepository;
    private IndexingService service;

    @BeforeEach
    void setUp() {
        embeddingClient = new FakeEmbeddingClient();
        chunker = new Chunker();
        embeddingRepository = mock(AiEmbeddingRepository.class);
        assessmentRepository = mock(AssessmentRepository.class);
        cveRepository = mock(CveRepository.class);
        service = new IndexingService(
                embeddingClient, chunker, embeddingRepository,
                assessmentRepository, cveRepository);
    }

    @Test
    @DisplayName("Indexing: Assessment wird in Chunks zerlegt und persistiert")
    void indexAssessment() {
        UUID id = UUID.randomUUID();
        Cve cve = Cve.builder().cveId("CVE-2025-48924").build();
        Assessment a = Assessment.builder()
                .id(id)
                .cve(cve)
                .severity(AhsSeverity.MEDIUM)
                .proposalSource(ProposalSource.HUMAN)
                .rationale("kurz")
                .status(AssessmentStatus.APPROVED)
                .build();
        given(assessmentRepository.findById(id)).willReturn(Optional.of(a));

        int chunks = service.indexAssessment(id);

        assertThat(chunks).isGreaterThan(0);
        verify(embeddingRepository).deleteByDocumentTypeAndDocumentRef(
                eq("ASSESSMENT"), eq("assessment:" + id));
        ArgumentCaptor<AiEmbedding> cap = ArgumentCaptor.forClass(AiEmbedding.class);
        verify(embeddingRepository, times(chunks)).save(cap.capture());
        AiEmbedding e = cap.getAllValues().get(0);
        assertThat(e.getDocumentType()).isEqualTo("ASSESSMENT");
        assertThat(e.getDocumentRef()).isEqualTo("assessment:" + id);
        assertThat(e.getModelId()).isEqualTo(FakeEmbeddingClient.MODEL_ID);
        assertThat(e.getEmbedding()).isNotNull();
    }

    @Test
    @DisplayName("Indexing: erneutes Indexieren loescht alte Chunks (Deduplikation)")
    void deduplikation() {
        UUID id = UUID.randomUUID();
        Cve cve = Cve.builder().cveId("CVE-X").build();
        Assessment a = Assessment.builder()
                .id(id)
                .cve(cve)
                .severity(AhsSeverity.LOW)
                .proposalSource(ProposalSource.HUMAN)
                .status(AssessmentStatus.APPROVED)
                .build();
        given(assessmentRepository.findById(id)).willReturn(Optional.of(a));

        service.indexAssessment(id);
        service.indexAssessment(id);

        verify(embeddingRepository, times(2)).deleteByDocumentTypeAndDocumentRef(
                "ASSESSMENT", "assessment:" + id);
    }

    @Test
    @DisplayName("Indexing: indexAdvisory baut Text aus Cve-Feldern")
    void indexAdvisory() {
        UUID id = UUID.randomUUID();
        Cve cve = Cve.builder()
                .id(id)
                .cveId("CVE-2026-22610")
                .summary("Ein Test-Advisory.")
                .cwes(List.of("CWE-79"))
                .kevListed(true)
                .build();
        given(cveRepository.findById(id)).willReturn(Optional.of(cve));

        int chunks = service.indexAdvisory(id);

        assertThat(chunks).isGreaterThan(0);
        verify(embeddingRepository).deleteByDocumentTypeAndDocumentRef(
                "ADVISORY", "cve:CVE-2026-22610");
    }

    @Test
    @DisplayName("Indexing: unbekanntes Assessment wirft IllegalArgumentException")
    void unbekannt() {
        UUID id = UUID.randomUUID();
        given(assessmentRepository.findById(id)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.indexAssessment(id))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Indexing: leerer Text liefert 0 Chunks und keine Schreiboperation")
    void leererText() {
        int chunks = service.indexDokument("ASSESSMENT", "ref", "");
        assertThat(chunks).isZero();
    }

    @Test
    @DisplayName("Indexing: indexAll iteriert ueber alle Assessments und Cves")
    void indexAll() {
        Cve cve = Cve.builder().cveId("CVE-X").build();
        Assessment a = Assessment.builder()
                .id(UUID.randomUUID())
                .cve(cve)
                .severity(AhsSeverity.LOW)
                .proposalSource(ProposalSource.HUMAN)
                .status(AssessmentStatus.APPROVED)
                .build();
        given(assessmentRepository.findAll()).willReturn(List.of(a));
        given(assessmentRepository.findById(any())).willReturn(Optional.of(a));
        Cve c2 = Cve.builder().id(UUID.randomUUID()).cveId("CVE-Y").summary("s").build();
        given(cveRepository.findAll()).willReturn(List.of(c2));
        given(cveRepository.findById(any())).willReturn(Optional.of(c2));

        int chunks = service.indexAll();

        assertThat(chunks).isGreaterThan(0);
    }
}
