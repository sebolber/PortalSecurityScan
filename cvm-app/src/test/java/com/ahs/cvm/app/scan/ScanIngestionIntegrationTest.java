package com.ahs.cvm.app.scan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ahs.cvm.app.AbstractIntegrationTest;
import com.ahs.cvm.application.scan.ScanAlreadyIngestedException;
import com.ahs.cvm.application.scan.ScanIngestService;
import com.ahs.cvm.application.scan.ScanSummary;
import com.ahs.cvm.application.scan.ScanUploadResponse;
import com.ahs.cvm.domain.enums.EnvironmentStage;
import com.ahs.cvm.persistence.environment.Environment;
import com.ahs.cvm.persistence.environment.EnvironmentRepository;
import com.ahs.cvm.persistence.product.Product;
import com.ahs.cvm.persistence.product.ProductRepository;
import com.ahs.cvm.persistence.product.ProductVersion;
import com.ahs.cvm.persistence.product.ProductVersionRepository;
import com.ahs.cvm.persistence.scan.Scan;
import com.ahs.cvm.persistence.scan.ScanRepository;
import java.util.UUID;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

/**
 * End-to-End-Test der Scan-Ingestion: CycloneDX wird geparst, Komponenten und
 * Findings werden persistiert, Dedup greift beim zweiten Upload.
 */
@EnabledIf(
        value = "com.ahs.cvm.app.DockerAvailability#isAvailable",
        disabledReason = "Docker-Daemon nicht erreichbar")
class ScanIngestionIntegrationTest extends AbstractIntegrationTest {

    @Autowired ScanIngestService scanIngestService;
    @Autowired ProductRepository productRepository;
    @Autowired ProductVersionRepository productVersionRepository;
    @Autowired EnvironmentRepository environmentRepository;
    @Autowired ScanRepository scanRepository;

    @Test
    @DisplayName("Ingestion: kleines CycloneDX erzeugt 5 Components und 2 Findings")
    void happyPath() throws Exception {
        byte[] sbom = new ClassPathResource("fixtures/cyclonedx/klein.json")
                .getContentAsByteArray();

        Fixtures fx = erzeugeFixtures();
        ScanUploadResponse response = scanIngestService.uploadAkzeptieren(
                fx.productVersionId(), fx.environmentId(), "trivy", sbom);

        assertThat(response.scanId()).isNotNull();

        Awaitility.await().atMost(java.time.Duration.ofSeconds(15))
                .until(() -> scanIngestService.zusammenfassung(response.scanId())
                        .map(ScanSummary::componentCount)
                        .orElse(0) >= 5);

        ScanSummary summary = scanIngestService.zusammenfassung(response.scanId()).orElseThrow();
        assertThat(summary.componentCount()).isEqualTo(5);
        assertThat(summary.findingCount()).isEqualTo(2);

        Scan persistiert = scanRepository.findById(response.scanId()).orElseThrow();
        assertThat(persistiert.getContentSha256()).hasSize(64);
        assertThat(persistiert.getRawSbom()).isNotNull();
        assertThat(persistiert.getRawSbom()).isNotEqualTo(sbom);
    }

    @Test
    @DisplayName("Ingestion: gleicher Content zweimal -> ScanAlreadyIngestedException")
    void dedupSchlaegtAn() throws Exception {
        byte[] sbom = new ClassPathResource("fixtures/cyclonedx/klein.json")
                .getContentAsByteArray();

        Fixtures fx = erzeugeFixtures();
        scanIngestService.uploadAkzeptieren(
                fx.productVersionId(), fx.environmentId(), "trivy", sbom);

        assertThatThrownBy(() -> scanIngestService.uploadAkzeptieren(
                        fx.productVersionId(), fx.environmentId(), "trivy", sbom))
                .isInstanceOf(ScanAlreadyIngestedException.class);
    }

    private Fixtures erzeugeFixtures() {
        Product p = productRepository.save(Product.builder()
                .name("PortalCore-IT")
                .key("pc-it-" + UUID.randomUUID())
                .build());
        ProductVersion pv = productVersionRepository.save(ProductVersion.builder()
                .product(p)
                .version("1.0.0-it")
                .build());
        Environment env = environmentRepository.save(Environment.builder()
                .key("REF-IT-" + UUID.randomUUID())
                .name("Ref IT")
                .stage(EnvironmentStage.REF)
                .build());
        return new Fixtures(pv.getId(), env.getId());
    }

    private record Fixtures(UUID productVersionId, UUID environmentId) {}
}
