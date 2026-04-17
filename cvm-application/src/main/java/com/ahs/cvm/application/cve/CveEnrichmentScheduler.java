package com.ahs.cvm.application.cve;

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Regelmaessige CVE-Anreicherung.
 *
 * <ul>
 *   <li>Stuendlich: vollstaendiger KEV-Feed.</li>
 *   <li>Alle 6 Stunden: EPSS-Scores.</li>
 * </ul>
 *
 * Kann via {@code cvm.scheduler.enabled=false} im Test deaktiviert werden.
 */
@Component
@EnableScheduling
@ConditionalOnProperty(
        prefix = "cvm.scheduler", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CveEnrichmentScheduler {

    private static final Logger log = LoggerFactory.getLogger(CveEnrichmentScheduler.class);

    private final CveEnrichmentService enrichmentService;

    public CveEnrichmentScheduler(CveEnrichmentService enrichmentService) {
        this.enrichmentService = enrichmentService;
    }

    @Scheduled(cron = "0 0 * * * *")
    public void aktualisiereKev() {
        log.info("Scheduled: KEV-Feed aktualisieren");
        int anzahl = enrichmentService.refreshAll(Optional.of("KEV"));
        log.info("Scheduled: KEV-Feed abgeschlossen, {} Eintraege verarbeitet", anzahl);
    }

    @Scheduled(cron = "0 0 */6 * * *")
    public void aktualisiereEpss() {
        log.info("Scheduled: EPSS aktualisieren");
        int anzahl = enrichmentService.refreshAll(Optional.of("EPSS"));
        log.info("Scheduled: EPSS abgeschlossen, {} Eintraege verarbeitet", anzahl);
    }
}
