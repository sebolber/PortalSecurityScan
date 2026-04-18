package com.ahs.cvm.ai.fixverify;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Taeglicher Cache-Aufraeumer fuer den Release-Notes-/Commit-Cache
 * des {@link FixVerificationService} (Go-Live-Nachzug zu Iteration
 * 16, CVM-41).
 *
 * <p>Ohne diesen Job wuechse der In-Memory-Cache bis zum
 * JVM-Restart. Die TTL-Pruefung beim Lese-Pfad ist davon unberuehrt
 * - der Job raeumt nur {@em dauerhaft} kalt gewordene Eintraege weg.
 */
@Component
@ConditionalOnProperty(
        prefix = "cvm.scheduler", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class FixVerificationCacheEvictionJob {

    private static final Logger log = LoggerFactory.getLogger(
            FixVerificationCacheEvictionJob.class);

    private final FixVerificationService service;

    public FixVerificationCacheEvictionJob(FixVerificationService service) {
        this.service = service;
    }

    @Scheduled(cron = "${cvm.fixverify.cache-eviction-cron:0 30 3 * * *}")
    public void laufeTaeglich() {
        int entfernt = service.purgeExpiredCache();
        log.debug("FixVerification-Cache-Eviction: {} Eintraege entfernt.", entfernt);
    }
}
