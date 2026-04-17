package com.ahs.cvm.persistence.assessment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ahs.cvm.domain.enums.AhsSeverity;
import com.ahs.cvm.domain.enums.AssessmentStatus;
import com.ahs.cvm.domain.enums.EnvironmentStage;
import com.ahs.cvm.domain.enums.ProposalSource;
import com.ahs.cvm.persistence.AbstractPersistenceIntegrationsTest;
import com.ahs.cvm.persistence.cve.Cve;
import com.ahs.cvm.persistence.cve.CveRepository;
import com.ahs.cvm.persistence.environment.Environment;
import com.ahs.cvm.persistence.environment.EnvironmentRepository;
import com.ahs.cvm.persistence.finding.Finding;
import com.ahs.cvm.persistence.finding.FindingRepository;
import com.ahs.cvm.persistence.product.Product;
import com.ahs.cvm.persistence.product.ProductRepository;
import com.ahs.cvm.persistence.product.ProductVersion;
import com.ahs.cvm.persistence.product.ProductVersionRepository;
import com.ahs.cvm.persistence.scan.Component;
import com.ahs.cvm.persistence.scan.ComponentOccurrence;
import com.ahs.cvm.persistence.scan.ComponentOccurrenceRepository;
import com.ahs.cvm.persistence.scan.ComponentRepository;
import com.ahs.cvm.persistence.scan.Scan;
import com.ahs.cvm.persistence.scan.ScanRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;

@EnabledIf(
        value = "com.ahs.cvm.persistence.support.DockerAvailability#isAvailable",
        disabledReason = "Docker-Daemon nicht erreichbar")
class AssessmentImmutableTest extends AbstractPersistenceIntegrationsTest {

    @Autowired ProductRepository productRepository;
    @Autowired ProductVersionRepository productVersionRepository;
    @Autowired EnvironmentRepository environmentRepository;
    @Autowired ScanRepository scanRepository;
    @Autowired ComponentRepository componentRepository;
    @Autowired ComponentOccurrenceRepository occurrenceRepository;
    @Autowired CveRepository cveRepository;
    @Autowired FindingRepository findingRepository;
    @Autowired AssessmentRepository assessmentRepository;
    @Autowired EntityManager entityManager;

    @Test
    @DisplayName(
            "Assessment: direkter Update-Versuch wirft ImmutabilityException")
    void assessmentIstUnveraenderlich() {
        Assessment bestehend = bereiteAssessmentVor();

        bestehend.setRationale("geaendert");

        assertThatThrownBy(() -> {
                    assessmentRepository.saveAndFlush(bestehend);
                })
                .hasCauseInstanceOf(ImmutabilityException.class);
    }

    @Test
    @DisplayName(
            "Assessment: markiereAlsUeberholt darf superseded_at und status setzen")
    void assessmentSupersededErlaubt() {
        Assessment bestehend = bereiteAssessmentVor();

        bestehend.markiereAlsUeberholt(Instant.now());
        Assessment gespeichert = assessmentRepository.saveAndFlush(bestehend);

        assertThat(gespeichert.getStatus()).isEqualTo(AssessmentStatus.SUPERSEDED);
        assertThat(gespeichert.getSupersededAt()).isNotNull();
    }

    private Assessment bereiteAssessmentVor() {
        Product produkt = productRepository.save(
                Product.builder().name("PortalCore-Test").key("pc-imm").build());
        ProductVersion pv = productVersionRepository.save(
                ProductVersion.builder()
                        .product(produkt)
                        .version("1.0.0-imm")
                        .build());
        Environment env = environmentRepository.save(
                Environment.builder()
                        .key("REF-IMM")
                        .name("Referenz Immutable")
                        .stage(EnvironmentStage.REF)
                        .build());
        Scan scan = scanRepository.save(
                Scan.builder()
                        .productVersion(pv)
                        .sbomFormat("CycloneDX")
                        .sbomChecksum("sha256:imm")
                        .scannedAt(Instant.now())
                        .build());
        Component comp = componentRepository.save(
                Component.builder()
                        .purl("pkg:maven/org.test/imm@1.0")
                        .name("imm")
                        .version("1.0")
                        .build());
        ComponentOccurrence occ = occurrenceRepository.save(
                ComponentOccurrence.builder().scan(scan).component(comp).direct(true).build());
        Cve cve = cveRepository.save(
                Cve.builder().cveId("CVE-2026-22610").source("NVD").build());
        Finding finding = findingRepository.save(
                Finding.builder()
                        .scan(scan)
                        .componentOccurrence(occ)
                        .cve(cve)
                        .detectedAt(Instant.now())
                        .build());

        Assessment assessment = Assessment.builder()
                .finding(finding)
                .productVersion(pv)
                .environment(env)
                .cve(cve)
                .version(1)
                .status(AssessmentStatus.APPROVED)
                .severity(AhsSeverity.MEDIUM)
                .proposalSource(ProposalSource.HUMAN)
                .rationale("initial")
                .decidedBy("a.admin@ahs.test")
                .decidedAt(Instant.now())
                .build();

        Assessment gespeichert = assessmentRepository.saveAndFlush(assessment);
        entityManager.clear();
        return assessmentRepository.findById(gespeichert.getId()).orElseThrow();
    }
}
