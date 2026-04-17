package com.ahs.cvm.application.cve;

import com.ahs.cvm.application.scan.ScanIngestedEvent;
import com.ahs.cvm.persistence.finding.FindingRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Loest Anreicherung fuer alle neuen CVEs eines gerade ingestierten Scans aus.
 * Laeuft asynchron nach erfolgreichem Commit des Scan-Events.
 */
@Component
public class CveEnrichmentOnScanIngestedListener {

    private static final Logger log = LoggerFactory.getLogger(CveEnrichmentOnScanIngestedListener.class);

    private final FindingRepository findingRepository;
    private final CveEnrichmentService enrichmentService;

    public CveEnrichmentOnScanIngestedListener(
            FindingRepository findingRepository, CveEnrichmentService enrichmentService) {
        this.findingRepository = findingRepository;
        this.enrichmentService = enrichmentService;
    }

    @Async("sbom-ingest")
    @TransactionalEventListener
    public void onScanIngested(ScanIngestedEvent event) {
        List<String> cveIds = findingRepository.findCveIdsByScanId(event.scanId());
        log.info("Starte CVE-Anreicherung fuer Scan {} ({} CVEs)",
                event.scanId(), cveIds.size());
        for (String cveId : cveIds) {
            try {
                enrichmentService.enrich(cveId);
            } catch (RuntimeException e) {
                log.warn("Anreicherung fuer {} gescheitert: {}", cveId, e.getMessage());
            }
        }
    }
}
