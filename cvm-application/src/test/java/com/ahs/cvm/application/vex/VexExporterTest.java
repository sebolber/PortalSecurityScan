package com.ahs.cvm.application.vex;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.ahs.cvm.domain.enums.AhsSeverity;
import com.ahs.cvm.domain.enums.AssessmentStatus;
import com.ahs.cvm.domain.enums.ProposalSource;
import com.ahs.cvm.persistence.assessment.Assessment;
import com.ahs.cvm.persistence.assessment.AssessmentRepository;
import com.ahs.cvm.persistence.cve.Cve;
import com.ahs.cvm.persistence.finding.Finding;
import com.ahs.cvm.persistence.mitigation.MitigationPlanRepository;
import com.ahs.cvm.persistence.product.Product;
import com.ahs.cvm.persistence.product.ProductVersion;
import com.ahs.cvm.persistence.product.ProductVersionRepository;
import com.ahs.cvm.persistence.scan.Component;
import com.ahs.cvm.persistence.scan.ComponentOccurrence;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class VexExporterTest {

    private static final UUID PV_ID = UUID.fromString("00000000-0000-0000-0000-0000000000aa");
    private AssessmentRepository assessmentRepo;
    private MitigationPlanRepository mitRepo;
    private ProductVersionRepository pvRepo;
    private VexExporter exporter;

    @BeforeEach
    void setUp() {
        assessmentRepo = mock(AssessmentRepository.class);
        mitRepo = mock(MitigationPlanRepository.class);
        pvRepo = mock(ProductVersionRepository.class);
        given(mitRepo.findByAssessmentId(any())).willReturn(List.of());
        ProductVersion pv = ProductVersion.builder()
                .id(PV_ID).version("1.14.2-test")
                .product(Product.builder().id(UUID.randomUUID())
                        .name("PortalCore-Test").build())
                .build();
        given(pvRepo.findById(PV_ID)).willReturn(Optional.of(pv));
        exporter = new VexExporter(assessmentRepo, mitRepo, pvRepo,
                new VexStatementMapper(),
                Clock.fixed(Instant.parse("2026-04-18T10:00:00Z"), ZoneOffset.UTC));
    }

    private Assessment assessment(AhsSeverity severity, AssessmentStatus status,
            List<String> sourceFields, String cveKey) {
        Cve cve = Cve.builder().id(UUID.randomUUID()).cveId(cveKey).build();
        Component c = Component.builder().id(UUID.randomUUID())
                .purl("pkg:maven/org.x/lib@1.2.3").build();
        ComponentOccurrence co = ComponentOccurrence.builder()
                .id(UUID.randomUUID()).component(c).build();
        Finding f = Finding.builder().id(UUID.randomUUID())
                .cve(cve).componentOccurrence(co).build();
        ProductVersion pv = ProductVersion.builder().id(PV_ID).build();
        return Assessment.builder()
                .id(UUID.randomUUID())
                .cve(cve)
                .finding(f)
                .productVersion(pv)
                .severity(severity)
                .status(status)
                .proposalSource(ProposalSource.HUMAN)
                .rationaleSourceFields(sourceFields)
                .rationale("test")
                .build();
    }

    @Test
    @DisplayName("VEX-Export: NOT_APPLICABLE wird zu not_affected mit passender Justification")
    void notAffected() {
        given(assessmentRepo.findAll()).willReturn(List.of(
                assessment(AhsSeverity.NOT_APPLICABLE, AssessmentStatus.APPROVED,
                        List.of("component.not_present"), "CVE-X")));

        String json = exporter.export(PV_ID, "cyclonedx");

        assertThat(json).contains("\"state\" : \"not_affected\"");
        assertThat(json).contains("\"justification\" : \"component_not_present\"");
        assertThat(json).contains("\"id\" : \"CVE-X\"");
    }

    @Test
    @DisplayName("VEX-Export: PROPOSED -> in_triage / under_investigation")
    void inTriage() {
        given(assessmentRepo.findAll()).willReturn(List.of(
                assessment(AhsSeverity.HIGH, AssessmentStatus.PROPOSED,
                        List.of(), "CVE-P")));

        String json = exporter.export(PV_ID, "cyclonedx");

        assertThat(json).contains("\"state\" : \"in_triage\"");
    }

    @Test
    @DisplayName("VEX-Export: APPROVED + HIGH -> exploitable")
    void affected() {
        given(assessmentRepo.findAll()).willReturn(List.of(
                assessment(AhsSeverity.HIGH, AssessmentStatus.APPROVED,
                        List.of(), "CVE-H")));

        String json = exporter.export(PV_ID, "cyclonedx");

        assertThat(json).contains("\"state\" : \"exploitable\"");
    }

    @Test
    @DisplayName("VEX-Export: Determinismus - gleiche Eingabe -> gleiches JSON")
    void determinismus() {
        given(assessmentRepo.findAll()).willReturn(List.of(
                assessment(AhsSeverity.NOT_APPLICABLE, AssessmentStatus.APPROVED,
                        List.of("component.not_present"), "CVE-A"),
                assessment(AhsSeverity.HIGH, AssessmentStatus.APPROVED,
                        List.of(), "CVE-B")));

        String a = exporter.export(PV_ID, "cyclonedx");
        String b = exporter.export(PV_ID, "cyclonedx");

        assertThat(a).isEqualTo(b);
        // Sortierung: CVE-A muss vor CVE-B stehen.
        assertThat(a.indexOf("CVE-A")).isLessThan(a.indexOf("CVE-B"));
    }

    @Test
    @DisplayName("VEX-Export: CSAF-Format enthaelt document.category csaf_vex")
    void csaf() {
        given(assessmentRepo.findAll()).willReturn(List.of(
                assessment(AhsSeverity.NOT_APPLICABLE, AssessmentStatus.APPROVED,
                        List.of(), "CVE-C")));

        String json = exporter.export(PV_ID, "csaf");

        assertThat(json).contains("\"category\" : \"csaf_vex\"");
        assertThat(json).contains("\"csaf_version\" : \"2.0\"");
        assertThat(json).contains("known_not_affected");
    }
}
