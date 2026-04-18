package com.ahs.cvm.application.kpi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.ahs.cvm.application.kpi.KpiService.BurnDownPoint;
import com.ahs.cvm.application.kpi.KpiService.KpiResult;
import com.ahs.cvm.application.kpi.KpiService.SlaBucket;
import com.ahs.cvm.domain.enums.AhsSeverity;
import com.ahs.cvm.persistence.kpi.KpiSnapshotDaily;
import com.ahs.cvm.persistence.kpi.KpiSnapshotDailyRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class KpiSnapshotWriterTest {

    private static final Instant NOW = Instant.parse("2026-04-18T12:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 4, 18);

    private KpiService kpiService;
    private KpiSnapshotDailyRepository repository;
    private KpiSnapshotWriter writer;

    @BeforeEach
    void setUp() {
        kpiService = mock(KpiService.class);
        repository = mock(KpiSnapshotDailyRepository.class);
        writer = new KpiSnapshotWriter(kpiService, repository,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("persistiert neuen Snapshot mit offenen Severities und Automation-Rate")
    void neuerSnapshot() {
        given(kpiService.compute(any(), any(), any()))
                .willReturn(stub(Map.of(
                        AhsSeverity.CRITICAL, 2L,
                        AhsSeverity.HIGH, 3L,
                        AhsSeverity.MEDIUM, 5L,
                        AhsSeverity.LOW, 1L,
                        AhsSeverity.INFORMATIONAL, 0L), 0.75));
        given(repository.findByScope(TODAY, null, null)).willReturn(Optional.empty());
        given(repository.save(any(KpiSnapshotDaily.class)))
                .willAnswer(inv -> inv.getArgument(0));

        KpiSnapshotDaily result = writer.persistSnapshot(null, null);

        assertThat(result.getOpenCritical()).isEqualTo(2);
        assertThat(result.getOpenHigh()).isEqualTo(3);
        assertThat(result.getOpenMedium()).isEqualTo(5);
        assertThat(result.getOpenLow()).isEqualTo(1);
        assertThat(result.getOpenInformational()).isZero();
        assertThat(result.getAutomationRate())
                .isEqualByComparingTo(new BigDecimal("0.7500"));
        assertThat(result.getSnapshotDay()).isEqualTo(TODAY);
    }

    @Test
    @DisplayName("aktualisiert bestehenden Snapshot an gleichem Tag/Scope")
    void idempotent() {
        UUID pv = UUID.randomUUID();
        UUID env = UUID.randomUUID();
        KpiSnapshotDaily bestehend = KpiSnapshotDaily.builder()
                .id(UUID.randomUUID())
                .snapshotDay(TODAY)
                .productVersionId(pv)
                .environmentId(env)
                .openCritical(1).openHigh(1)
                .build();
        given(repository.findByScope(TODAY, pv, env))
                .willReturn(Optional.of(bestehend));
        given(kpiService.compute(pv, env, java.time.Duration.ofDays(1)))
                .willReturn(stub(Map.of(AhsSeverity.CRITICAL, 5L,
                        AhsSeverity.HIGH, 0L,
                        AhsSeverity.MEDIUM, 0L,
                        AhsSeverity.LOW, 0L,
                        AhsSeverity.INFORMATIONAL, 0L), 0.0));
        given(repository.save(any(KpiSnapshotDaily.class)))
                .willAnswer(inv -> inv.getArgument(0));

        KpiSnapshotDaily result = writer.persistSnapshot(pv, env);

        assertThat(result.getId()).isEqualTo(bestehend.getId());
        assertThat(result.getOpenCritical()).isEqualTo(5);
        assertThat(result.getOpenHigh()).isZero();
    }

    private KpiResult stub(Map<AhsSeverity, Long> open, double automationRate) {
        Map<AhsSeverity, Long> offene = new EnumMap<>(AhsSeverity.class);
        offene.putAll(open);
        Map<AhsSeverity, Long> mttr = new EnumMap<>(AhsSeverity.class);
        Map<AhsSeverity, SlaBucket> sla = new EnumMap<>(AhsSeverity.class);
        for (AhsSeverity s : AhsSeverity.values()) {
            mttr.putIfAbsent(s, 0L);
            sla.putIfAbsent(s, new SlaBucket(0, 0));
        }
        List<BurnDownPoint> burn = List.of(new BurnDownPoint(TODAY, 0L));
        return new KpiResult(offene, burn, mttr, sla, automationRate, NOW);
    }
}
