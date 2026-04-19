package com.ahs.cvm.application.cve;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ahs.cvm.domain.enums.AhsSeverity;
import com.ahs.cvm.domain.enums.AssessmentStatus;
import com.ahs.cvm.domain.enums.ProposalSource;
import com.ahs.cvm.persistence.assessment.Assessment;
import com.ahs.cvm.persistence.assessment.AssessmentRepository;
import com.ahs.cvm.persistence.cve.Cve;
import com.ahs.cvm.persistence.cve.CveRepository;
import com.ahs.cvm.persistence.finding.Finding;
import com.ahs.cvm.persistence.finding.FindingRepository;
import com.ahs.cvm.persistence.scan.Component;
import com.ahs.cvm.persistence.scan.ComponentOccurrence;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CveQueryServiceDetailTest {

    private final CveRepository cves = mock(CveRepository.class);
    private final FindingRepository findings = mock(FindingRepository.class);
    private final AssessmentRepository assessments = mock(AssessmentRepository.class);
    private final CveQueryService service = new CveQueryService(cves, findings, assessments);

    @Test
    @DisplayName("Detail: unbekannte CVE liefert Optional.empty()")
    void unbekannteCve() {
        when(cves.findByCveId("CVE-UNBEKANNT")).thenReturn(Optional.empty());
        assertThat(service.findDetail("CVE-UNBEKANNT")).isEmpty();
    }

    @Test
    @DisplayName("Detail: bekannte CVE ohne Findings liefert leere Listen")
    void keineFindings() {
        Cve cve = cveBasis("CVE-2017-18640", UUID.randomUUID());
        when(cves.findByCveId("CVE-2017-18640")).thenReturn(Optional.of(cve));
        when(findings.findByCveId(cve.getId())).thenReturn(List.of());

        Optional<CveDetailView> result = service.findDetail("CVE-2017-18640");
        assertThat(result).isPresent();
        assertThat(result.orElseThrow().findings()).isEmpty();
        assertThat(result.orElseThrow().assessments()).isEmpty();
    }

    @Test
    @DisplayName("Detail: waehlt pro Finding das juengste Assessment")
    void juengstesAssessmentProFinding() {
        UUID cveRowId = UUID.randomUUID();
        Cve cve = cveBasis("CVE-2025-48924", cveRowId);
        when(cves.findByCveId("CVE-2025-48924")).thenReturn(Optional.of(cve));

        Finding f = findingBasis();
        when(findings.findByCveId(cveRowId)).thenReturn(List.of(f));

        Assessment latest = assessmentBasis(f, 3, AssessmentStatus.APPROVED);
        when(assessments.findFirstByFindingIdOrderByVersionDesc(f.getId()))
                .thenReturn(Optional.of(latest));

        Optional<CveDetailView> result = service.findDetail("CVE-2025-48924");
        assertThat(result).isPresent();
        List<CveDetailView.AssessmentEntry> entries = result.orElseThrow().assessments();
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).version()).isEqualTo(3);
        assertThat(entries.get(0).status()).isEqualTo(AssessmentStatus.APPROVED);
    }

    @Test
    @DisplayName("Detail: leere Eingabe liefert empty ohne Repo-Call")
    void leereEingabe() {
        assertThat(service.findDetail(null)).isEmpty();
        assertThat(service.findDetail("")).isEmpty();
        assertThat(service.findDetail("   ")).isEmpty();
    }

    // --- Helfer ---

    private Cve cveBasis(String cveId, UUID id) {
        return Cve.builder()
                .id(id)
                .cveId(cveId)
                .summary("Test")
                .cvssBaseScore(new java.math.BigDecimal("7.5"))
                .publishedAt(Instant.now())
                .lastModifiedAt(Instant.now())
                .source("NVD")
                .build();
    }

    private Finding findingBasis() {
        Component component = Component.builder()
                .id(UUID.randomUUID())
                .purl("pkg:maven/org.example/foo@1.0.0")
                .name("foo")
                .version("1.0.0")
                .type("library")
                .build();
        ComponentOccurrence occ = ComponentOccurrence.builder()
                .id(UUID.randomUUID())
                .component(component)
                .direct(true)
                .build();
        return Finding.builder()
                .id(UUID.randomUUID())
                .componentOccurrence(occ)
                .detectedAt(Instant.now())
                .build();
    }

    private Assessment assessmentBasis(Finding f, int version, AssessmentStatus status) {
        return Assessment.builder()
                .id(UUID.randomUUID())
                .finding(f)
                .version(version)
                .severity(AhsSeverity.HIGH)
                .status(status)
                .proposalSource(ProposalSource.RULE)
                .rationale("test")
                .decidedBy("a.admin@ahs.test")
                .createdAt(Instant.now())
                .build();
    }
}
