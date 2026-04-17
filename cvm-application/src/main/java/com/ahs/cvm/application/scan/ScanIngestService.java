package com.ahs.cvm.application.scan;

import com.ahs.cvm.persistence.cve.Cve;
import com.ahs.cvm.persistence.cve.CveRepository;
import com.ahs.cvm.persistence.environment.Environment;
import com.ahs.cvm.persistence.environment.EnvironmentRepository;
import com.ahs.cvm.persistence.finding.Finding;
import com.ahs.cvm.persistence.finding.FindingRepository;
import com.ahs.cvm.persistence.product.ProductVersion;
import com.ahs.cvm.persistence.product.ProductVersionRepository;
import com.ahs.cvm.persistence.scan.Component;
import com.ahs.cvm.persistence.scan.ComponentOccurrence;
import com.ahs.cvm.persistence.scan.ComponentOccurrenceRepository;
import com.ahs.cvm.persistence.scan.ComponentRepository;
import com.ahs.cvm.persistence.scan.Scan;
import com.ahs.cvm.persistence.scan.ScanRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestriert die SBOM-Ingestion:
 * {@code parse → validate → normalize → persist → event}.
 */
@Service
public class ScanIngestService {

    private static final Logger log = LoggerFactory.getLogger(ScanIngestService.class);

    private final CycloneDxParser parser;
    private final SbomEncryption encryption;
    private final ScanRepository scanRepository;
    private final ProductVersionRepository productVersionRepository;
    private final EnvironmentRepository environmentRepository;
    private final ComponentRepository componentRepository;
    private final ComponentOccurrenceRepository occurrenceRepository;
    private final CveRepository cveRepository;
    private final FindingRepository findingRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ScanIngestService selbstProxy;

    public ScanIngestService(
            CycloneDxParser parser,
            SbomEncryption encryption,
            ScanRepository scanRepository,
            ProductVersionRepository productVersionRepository,
            EnvironmentRepository environmentRepository,
            ComponentRepository componentRepository,
            ComponentOccurrenceRepository occurrenceRepository,
            CveRepository cveRepository,
            FindingRepository findingRepository,
            ApplicationEventPublisher eventPublisher,
            @Lazy ScanIngestService selbstProxy) {
        this.parser = parser;
        this.encryption = encryption;
        this.scanRepository = scanRepository;
        this.productVersionRepository = productVersionRepository;
        this.environmentRepository = environmentRepository;
        this.componentRepository = componentRepository;
        this.occurrenceRepository = occurrenceRepository;
        this.cveRepository = cveRepository;
        this.findingRepository = findingRepository;
        this.eventPublisher = eventPublisher;
        this.selbstProxy = selbstProxy;
    }

    /**
     * Synchrone Pruefung und Reservierung. Parst die SBOM, prueft Schema und
     * Dedup und legt den Scan im Status "nur Kopfdaten" an, damit der Client
     * sofort eine {@code scanId} bekommt. Die eigentliche Persistenz der
     * Komponenten und Findings laeuft danach asynchron.
     */
    @Transactional
    public ScanUploadResponse uploadAkzeptieren(
            UUID productVersionId,
            UUID environmentId,
            String scanner,
            byte[] rawSbom) {
        ProductVersion produktVersion = productVersionRepository
                .findById(productVersionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unbekannte productVersionId: " + productVersionId));
        Environment umgebung = null;
        if (environmentId != null) {
            umgebung = environmentRepository
                    .findById(environmentId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Unbekannte environmentId: " + environmentId));
        }

        parser.parse(rawSbom);
        String contentSha256 = encryption.sha256Hex(rawSbom);

        Optional<Scan> vorhanden = scanRepository
                .findByProductVersionIdAndEnvironmentIdAndContentSha256(
                        productVersionId, environmentId, contentSha256);
        if (vorhanden.isPresent()) {
            throw new ScanAlreadyIngestedException(vorhanden.get().getId());
        }

        Scan reservierterScan = scanRepository.save(
                Scan.builder()
                        .productVersion(produktVersion)
                        .environment(umgebung)
                        .sbomFormat("CycloneDX")
                        .sbomChecksum(contentSha256)
                        .contentSha256(contentSha256)
                        .rawSbom(encryption.encrypt(rawSbom))
                        .scannedAt(Instant.now())
                        .scanner(scanner != null ? scanner : "unknown")
                        .build());

        CompletableFuture<Void> future = selbstProxy.verarbeiteAsync(reservierterScan.getId(), rawSbom);
        future.exceptionally(ex -> {
            log.error("Async-Ingestion fehlgeschlagen fuer Scan {}", reservierterScan.getId(), ex);
            return null;
        });

        return new ScanUploadResponse(
                reservierterScan.getId(),
                "/api/v1/scans/" + reservierterScan.getId());
    }

    @Async(ScanIngestConfig.EXECUTOR_NAME)
    public CompletableFuture<Void> verarbeiteAsync(UUID scanId, byte[] rawSbom) {
        CycloneDxBom bom = parser.parse(rawSbom);
        ErgebnisZaehler zaehler = persistiere(scanId, bom);

        Scan scan = scanRepository.findById(scanId).orElseThrow();
        eventPublisher.publishEvent(new ScanIngestedEvent(
                scanId,
                scan.getProductVersion().getId(),
                scan.getEnvironment() != null ? scan.getEnvironment().getId() : null,
                zaehler.componentCount,
                zaehler.findingCount,
                Instant.now()));
        return CompletableFuture.completedFuture(null);
    }

    @Transactional
    public ErgebnisZaehler persistiere(UUID scanId, CycloneDxBom bom) {
        Scan scan = scanRepository.findById(scanId).orElseThrow();

        Map<String, ComponentOccurrence> occByBomRef = new HashMap<>();

        for (CycloneDxBom.Component c : Objects.requireNonNullElse(
                bom.components(), List.<CycloneDxBom.Component>of())) {
            if (c.purl() == null || c.purl().isBlank()) continue;
            Component component = componentRepository
                    .findByPurl(c.purl())
                    .orElseGet(() -> componentRepository.save(
                            Component.builder()
                                    .purl(c.purl())
                                    .name(c.name() != null ? c.name() : "unknown")
                                    .version(c.version() != null ? c.version() : "0.0.0")
                                    .type(c.type())
                                    .build()));
            ComponentOccurrence occ = occurrenceRepository.save(
                    ComponentOccurrence.builder()
                            .scan(scan)
                            .component(component)
                            .direct(Boolean.TRUE)
                            .bomRef(c.bomRef())
                            .build());
            if (c.bomRef() != null) {
                occByBomRef.put(c.bomRef(), occ);
            }
        }

        int findingCount = 0;
        for (CycloneDxBom.Vulnerability v : Objects.requireNonNullElse(
                bom.vulnerabilities(), List.<CycloneDxBom.Vulnerability>of())) {
            if (v.id() == null || v.id().isBlank()) continue;
            Cve cve = cveRepository
                    .findByCveId(v.id())
                    .orElseGet(() -> cveRepository.save(
                            Cve.builder()
                                    .cveId(v.id())
                                    .summary(v.description())
                                    .source(v.source() != null && v.source().name() != null
                                            ? v.source().name()
                                            : "UNKNOWN")
                                    .build()));

            for (CycloneDxBom.Affect a : Objects.requireNonNullElse(
                    v.affects(), List.<CycloneDxBom.Affect>of())) {
                ComponentOccurrence occ = occByBomRef.get(a.ref());
                if (occ == null) continue;
                findingRepository.save(
                        Finding.builder()
                                .scan(scan)
                                .componentOccurrence(occ)
                                .cve(cve)
                                .detectedAt(Instant.now())
                                .build());
                findingCount++;
            }
        }

        return new ErgebnisZaehler(bom.components() != null ? bom.components().size() : 0, findingCount);
    }

    public record ErgebnisZaehler(int componentCount, int findingCount) {}

    @Transactional(readOnly = true)
    public Optional<ScanSummary> zusammenfassung(UUID scanId) {
        return scanRepository.findById(scanId).map(s -> new ScanSummary(
                s.getId(),
                s.getProductVersion().getId(),
                s.getEnvironment() != null ? s.getEnvironment().getId() : null,
                s.getScanner(),
                s.getContentSha256(),
                s.getScannedAt(),
                occurrenceRepository.findByScanId(scanId).size(),
                findingRepository.findByScanId(scanId).size()));
    }
}
