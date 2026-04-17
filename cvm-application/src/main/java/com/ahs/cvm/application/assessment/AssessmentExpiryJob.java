package com.ahs.cvm.application.assessment;

import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Taeglicher Scheduler (Default 03:00 Europe/Berlin), der abgelaufene
 * APPROVED-Assessments auf {@link com.ahs.cvm.domain.enums.AssessmentStatus#EXPIRED}
 * setzt. Wird ueber {@code cvm.scheduler.enabled=false} im Test deaktiviert.
 */
@Component
@ConditionalOnProperty(
        prefix = "cvm.scheduler", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AssessmentExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(AssessmentExpiryJob.class);

    private final AssessmentWriteService writeService;

    public AssessmentExpiryJob(AssessmentWriteService writeService) {
        this.writeService = writeService;
    }

    @Scheduled(cron = "${cvm.assessment.expiry-cron:0 0 3 * * *}")
    public void laufeTaeglich() {
        Instant jetzt = Instant.now();
        int anzahl = writeService.expireIfDue(jetzt);
        log.info("Assessment-Expiry abgeschlossen: {} Eintraege auf EXPIRED gesetzt.", anzahl);
    }
}
