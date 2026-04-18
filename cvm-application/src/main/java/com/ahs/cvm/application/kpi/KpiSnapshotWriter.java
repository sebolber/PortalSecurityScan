package com.ahs.cvm.application.kpi;

import com.ahs.cvm.application.kpi.KpiService.KpiResult;
import com.ahs.cvm.domain.enums.AhsSeverity;
import com.ahs.cvm.persistence.kpi.KpiSnapshotDaily;
import com.ahs.cvm.persistence.kpi.KpiSnapshotDailyRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Schreibt den taeglichen KPI-Snapshot fuer den uebergebenen Scope
 * (Iteration 22, CVM-53).
 *
 * <p>Quelle ist {@link KpiService#compute}. Persistiert werden die
 * offenen Severities und die Automatisierungsquote. Das Schreiben ist
 * idempotent pro Tag/Scope: existiert bereits ein Snapshot, wird er
 * aktualisiert (Rehydrierung).
 */
@Service
public class KpiSnapshotWriter {

    private static final Logger log = LoggerFactory.getLogger(KpiSnapshotWriter.class);

    private final KpiService kpiService;
    private final KpiSnapshotDailyRepository repository;
    private final Clock clock;

    public KpiSnapshotWriter(
            KpiService kpiService,
            KpiSnapshotDailyRepository repository,
            Clock clock) {
        this.kpiService = kpiService;
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public KpiSnapshotDaily persistSnapshot(UUID productVersionId, UUID environmentId) {
        KpiResult result = kpiService.compute(productVersionId, environmentId,
                Duration.ofDays(1));
        LocalDate today = LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC);

        KpiSnapshotDaily snapshot = repository
                .findByScope(today, productVersionId, environmentId)
                .orElseGet(() -> KpiSnapshotDaily.builder()
                        .snapshotDay(today)
                        .productVersionId(productVersionId)
                        .environmentId(environmentId)
                        .build());
        snapshot.setOpenCritical(intWert(result, AhsSeverity.CRITICAL));
        snapshot.setOpenHigh(intWert(result, AhsSeverity.HIGH));
        snapshot.setOpenMedium(intWert(result, AhsSeverity.MEDIUM));
        snapshot.setOpenLow(intWert(result, AhsSeverity.LOW));
        snapshot.setOpenInformational(intWert(result, AhsSeverity.INFORMATIONAL));
        snapshot.setAutomationRate(BigDecimal.valueOf(result.automationRate())
                .setScale(4, RoundingMode.HALF_UP));

        KpiSnapshotDaily gespeichert = repository.save(snapshot);
        log.info("KPI-Snapshot {} pv={} env={} crit={} high={}",
                today, productVersionId, environmentId,
                gespeichert.getOpenCritical(), gespeichert.getOpenHigh());
        return gespeichert;
    }

    private static int intWert(KpiResult result, AhsSeverity s) {
        Long v = result.openBySeverity().get(s);
        return v == null ? 0 : v.intValue();
    }
}
