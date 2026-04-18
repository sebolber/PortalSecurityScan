package com.ahs.cvm.application.cve;

import com.ahs.cvm.application.scan.ScanIngestedEvent;
import com.ahs.cvm.persistence.cve.Cve;
import com.ahs.cvm.persistence.cve.CveRepository;
import com.ahs.cvm.persistence.finding.Finding;
import com.ahs.cvm.persistence.finding.FindingRepository;
import com.ahs.cvm.persistence.scan.ComponentOccurrence;
import com.ahs.cvm.persistence.scan.ComponentOccurrenceRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Iteration 33 (CVM-77): Nach jedem ingestierten Scan werden die
 * {@code ComponentOccurrence}-Eintraege gegen einen externen
 * Vulnerability-Feed ({@link ComponentVulnerabilityLookup}, i.d.R. OSV)
 * gematcht. Funde landen als {@link Finding} im Store und werden von
 * dem bestehenden {@link CveEnrichmentOnScanIngestedListener} mit den
 * ueblichen Feeds angereichert.
 *
 * <p>Kein Hard-Fail: Der Port liefert bei Fehlern leere Listen.
 */
@Component
public class ComponentCveMatchingOnScanIngestedListener {

    private static final Logger log = LoggerFactory.getLogger(
            ComponentCveMatchingOnScanIngestedListener.class);

    private final ComponentOccurrenceRepository occurrenceRepository;
    private final FindingRepository findingRepository;
    private final CveRepository cveRepository;
    private final ComponentVulnerabilityLookup lookup;
    private final CveEnrichmentService enrichmentService;

    public ComponentCveMatchingOnScanIngestedListener(
            ComponentOccurrenceRepository occurrenceRepository,
            FindingRepository findingRepository,
            CveRepository cveRepository,
            ComponentVulnerabilityLookup lookup,
            CveEnrichmentService enrichmentService) {
        this.occurrenceRepository = occurrenceRepository;
        this.findingRepository = findingRepository;
        this.cveRepository = cveRepository;
        this.lookup = lookup;
        this.enrichmentService = enrichmentService;
    }

    /**
     * Spring verlangt {@code REQUIRES_NEW} (oder {@code NOT_SUPPORTED})
     * auf einer @TransactionalEventListener-Methode mit @Transactional:
     * Der Listener feuert nach dem Commit der Ur-Transaktion, eine
     * Default-Propagation wuerde eine bereits geschlossene Transaktion
     * wiederaufnehmen wollen und den Context-Start verhindern.
     */
    @Async("sbom-ingest")
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onScanIngested(ScanIngestedEvent event) {
        if (!lookup.isEnabled()) {
            return;
        }

        List<ComponentOccurrence> occurrences =
                occurrenceRepository.findByScanId(event.scanId());
        List<String> purls = new ArrayList<>();
        for (ComponentOccurrence occ : occurrences) {
            String purl = occ.getComponent() != null
                    ? occ.getComponent().getPurl() : null;
            if (purl != null && !purl.isBlank()) {
                purls.add(purl);
            }
        }
        if (purls.isEmpty()) {
            return;
        }

        Map<String, List<String>> treffer = lookup.findCveIdsForPurls(purls);
        if (treffer.isEmpty()) {
            log.info("OSV: keine Treffer fuer {} PURLs in Scan {}",
                    purls.size(), event.scanId());
            return;
        }

        int neueFindings = 0;
        java.util.Set<String> zuAnreichern = new java.util.LinkedHashSet<>();
        for (ComponentOccurrence occ : occurrences) {
            String purl = occ.getComponent() != null
                    ? occ.getComponent().getPurl() : null;
            if (purl == null) {
                continue;
            }
            List<String> cveIds = treffer.getOrDefault(purl, List.of());
            for (String cveId : cveIds) {
                if (findingRepository
                        .existsByScanIdAndComponentOccurrenceIdAndCveCveId(
                                event.scanId(), occ.getId(), cveId)) {
                    continue;
                }
                Cve cve = upsertCve(cveId);
                findingRepository.save(Finding.builder()
                        .scan(occ.getScan())
                        .componentOccurrence(occ)
                        .cve(cve)
                        .detectedAt(Instant.now())
                        .build());
                neueFindings++;
                zuAnreichern.add(cveId);
            }
        }
        log.info("OSV: {} neue Findings in Scan {} angelegt",
                neueFindings, event.scanId());

        // Kette an die Feed-Anreicherung an. CveEnrichmentOnScanIngestedListener
        // laeuft parallel auf demselben Event und sieht die gerade
        // angelegten CVEs deswegen in der Regel NICHT. Damit die 77 OSV-
        // Matches trotzdem NVD/GHSA/KEV/EPSS-Daten bekommen, reichern wir
        // sie hier explizit an. Fehler in einzelnen Feeds werden
        // geschluckt - der Scan selbst bleibt dann trotzdem konsistent.
        for (String cveId : zuAnreichern) {
            try {
                enrichmentService.enrich(cveId);
            } catch (RuntimeException ex) {
                log.warn("Anreicherung fuer OSV-Treffer {} fehlgeschlagen: {}",
                        cveId, ex.getMessage());
            }
        }
    }

    private Cve upsertCve(String cveId) {
        Optional<Cve> existing = cveRepository.findByCveId(cveId);
        return existing.orElseGet(() -> cveRepository.save(Cve.builder()
                .cveId(cveId)
                .source("OSV")
                .build()));
    }
}
