package com.ahs.cvm.application.kpi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Persistiert einen globalen KPI-Snapshot pro Tag (Iteration 22,
 * CVM-53).
 *
 * <p>Bewusst nur ein globaler Snapshot (pv/env = null). Scope-Snapshots
 * werden bei Bedarf vom UI oder ExecutiveReport direkt per
 * {@link KpiSnapshotWriter#persistSnapshot} geholt. Sobald eine
 * produktversions-Listung feststeht, laesst sich hier iterieren.
 *
 * <p>Aktiv nur, wenn {@code cvm.kpi.snapshot.enabled=true}
 * (Default an).
 */
@Component
@ConditionalOnProperty(prefix = "cvm.kpi.snapshot",
        name = "enabled", havingValue = "true", matchIfMissing = true)
public class KpiDailySnapshotJob {

    private static final Logger log = LoggerFactory.getLogger(KpiDailySnapshotJob.class);

    private final KpiSnapshotWriter writer;

    public KpiDailySnapshotJob(KpiSnapshotWriter writer) {
        this.writer = writer;
    }

    @Scheduled(cron = "${cvm.kpi.snapshot.cron:0 0 1 * * *}")
    public void run() {
        try {
            writer.persistSnapshot(null, null);
        } catch (RuntimeException ex) {
            log.warn("KPI-Snapshot-Job fehlgeschlagen: {}", ex.getMessage());
        }
    }
}
