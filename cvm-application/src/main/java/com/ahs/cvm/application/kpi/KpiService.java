package com.ahs.cvm.application.kpi;

import com.ahs.cvm.domain.enums.AhsSeverity;
import com.ahs.cvm.domain.enums.AssessmentStatus;
import com.ahs.cvm.domain.enums.ProposalSource;
import com.ahs.cvm.persistence.assessment.Assessment;
import com.ahs.cvm.persistence.assessment.AssessmentRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aggregiert Kennzahlen (KPIs) aus den Assessments (Iteration 21,
 * CVM-52).
 *
 * <p>Liefert fuenf Bloecke:
 * <ol>
 *   <li>offene CVEs je Severity (aktuell),</li>
 *   <li>Burn-Down-Serie (taeglich offene CVEs im Fenster),</li>
 *   <li>MTTR je Severity (Durchschnitt decidedAt - createdAt),</li>
 *   <li>Fix-SLA-Quote je Severity (Anteil innerhalb SLA),</li>
 *   <li>Automatisierungsquote (Anteil KI-Vorschlaege ohne Aenderung
 *       approved).</li>
 * </ol>
 *
 * <p>Berechnet wird aus einem Stream-Filter; fuer grosse Datenmengen
 * ist eine Materialized-View-Strategie in den offenen Punkten
 * vermerkt.
 */
@Service
public class KpiService {

    private static final Map<AhsSeverity, Integer> SLA_TAGE = Map.of(
            AhsSeverity.CRITICAL, 7,
            AhsSeverity.HIGH, 30,
            AhsSeverity.MEDIUM, 90,
            AhsSeverity.LOW, 180);

    private final AssessmentRepository assessmentRepository;
    private final Clock clock;

    public KpiService(AssessmentRepository assessmentRepository, Clock clock) {
        this.assessmentRepository = assessmentRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public KpiResult compute(UUID productVersionId, UUID environmentId,
            Duration window) {
        Duration w = window == null || window.isZero() || window.isNegative()
                ? Duration.ofDays(90) : window;
        Instant now = Instant.now(clock);
        Instant fensterStart = now.minus(w);

        List<Assessment> all = assessmentRepository.findAll().stream()
                .filter(a -> matches(a, productVersionId, environmentId))
                .toList();

        Map<AhsSeverity, Long> offenBySeverity = zaehleOffen(all);
        List<BurnDownPoint> burnDown = baueBurnDown(all, fensterStart, now);
        Map<AhsSeverity, Long> mttrTage = berechneMttr(all, fensterStart);
        Map<AhsSeverity, SlaBucket> sla = berechneSla(all, fensterStart);
        double automationRate = berechneAutomationRate(all, fensterStart);

        return new KpiResult(offenBySeverity, burnDown, mttrTage, sla,
                automationRate, now);
    }

    private static boolean matches(Assessment a, UUID pv, UUID env) {
        if (pv != null && (a.getProductVersion() == null
                || !Objects.equals(a.getProductVersion().getId(), pv))) {
            return false;
        }
        if (env != null && (a.getEnvironment() == null
                || !Objects.equals(a.getEnvironment().getId(), env))) {
            return false;
        }
        return true;
    }

    private static boolean istOffen(Assessment a) {
        return a.getSupersededAt() == null
                && (a.getStatus() == AssessmentStatus.PROPOSED
                        || a.getStatus() == AssessmentStatus.NEEDS_REVIEW
                        || a.getStatus() == AssessmentStatus.NEEDS_VERIFICATION);
    }

    private static boolean istAbgeschlossen(Assessment a) {
        return a.getStatus() == AssessmentStatus.APPROVED
                || a.getStatus() == AssessmentStatus.REJECTED;
    }

    private Map<AhsSeverity, Long> zaehleOffen(List<Assessment> all) {
        Map<AhsSeverity, Long> m = new EnumMap<>(AhsSeverity.class);
        for (AhsSeverity s : AhsSeverity.values()) {
            m.put(s, 0L);
        }
        for (Assessment a : all) {
            if (istOffen(a)) {
                m.merge(a.getSeverity(), 1L, Long::sum);
            }
        }
        return m;
    }

    private List<BurnDownPoint> baueBurnDown(List<Assessment> all,
            Instant from, Instant to) {
        List<BurnDownPoint> points = new ArrayList<>();
        LocalDate d = LocalDate.ofInstant(from, ZoneOffset.UTC);
        LocalDate end = LocalDate.ofInstant(to, ZoneOffset.UTC);
        while (!d.isAfter(end)) {
            Instant stichtag = d.plusDays(1).atStartOfDay()
                    .toInstant(ZoneOffset.UTC);
            long offenAmTag = all.stream()
                    .filter(a -> a.getCreatedAt() != null
                            && a.getCreatedAt().isBefore(stichtag))
                    .filter(a -> a.getDecidedAt() == null
                            || !a.getDecidedAt().isBefore(stichtag))
                    .count();
            points.add(new BurnDownPoint(d, offenAmTag));
            d = d.plusDays(1);
        }
        return points;
    }

    private Map<AhsSeverity, Long> berechneMttr(List<Assessment> all,
            Instant fensterStart) {
        Map<AhsSeverity, long[]> tmp = new EnumMap<>(AhsSeverity.class);
        for (Assessment a : all) {
            if (!istAbgeschlossen(a) || a.getDecidedAt() == null
                    || a.getCreatedAt() == null) {
                continue;
            }
            if (a.getDecidedAt().isBefore(fensterStart)) {
                continue;
            }
            long tage = Math.max(0,
                    ChronoUnit.DAYS.between(a.getCreatedAt(), a.getDecidedAt()));
            tmp.computeIfAbsent(a.getSeverity(), s -> new long[2]);
            long[] pair = tmp.get(a.getSeverity());
            pair[0] += tage;
            pair[1] += 1;
        }
        Map<AhsSeverity, Long> mttr = new EnumMap<>(AhsSeverity.class);
        for (AhsSeverity s : AhsSeverity.values()) {
            long[] pair = tmp.get(s);
            mttr.put(s, pair == null || pair[1] == 0 ? 0L : pair[0] / pair[1]);
        }
        return mttr;
    }

    private Map<AhsSeverity, SlaBucket> berechneSla(List<Assessment> all,
            Instant fensterStart) {
        Map<AhsSeverity, SlaBucket> out = new EnumMap<>(AhsSeverity.class);
        for (AhsSeverity s : AhsSeverity.values()) {
            out.put(s, new SlaBucket(0, 0));
        }
        for (Assessment a : all) {
            if (!SLA_TAGE.containsKey(a.getSeverity())) {
                continue;
            }
            if (!istAbgeschlossen(a) || a.getDecidedAt() == null
                    || a.getCreatedAt() == null) {
                continue;
            }
            if (a.getDecidedAt().isBefore(fensterStart)) {
                continue;
            }
            int limit = SLA_TAGE.get(a.getSeverity());
            long tage = ChronoUnit.DAYS.between(a.getCreatedAt(), a.getDecidedAt());
            SlaBucket b = out.get(a.getSeverity());
            int inSla = b.inSla() + (tage <= limit ? 1 : 0);
            int gesamt = b.gesamt() + 1;
            out.put(a.getSeverity(), new SlaBucket(inSla, gesamt));
        }
        return out;
    }

    private double berechneAutomationRate(List<Assessment> all, Instant fensterStart) {
        long aiApproved = 0;
        long aiGesamt = 0;
        for (Assessment a : all) {
            if (a.getProposalSource() != ProposalSource.AI_SUGGESTION) {
                continue;
            }
            if (a.getCreatedAt() == null || a.getCreatedAt().isBefore(fensterStart)) {
                continue;
            }
            aiGesamt++;
            if (a.getStatus() == AssessmentStatus.APPROVED) {
                aiApproved++;
            }
        }
        return aiGesamt == 0 ? 0.0 : (double) aiApproved / aiGesamt;
    }

    public record KpiResult(
            Map<AhsSeverity, Long> openBySeverity,
            List<BurnDownPoint> burnDown,
            Map<AhsSeverity, Long> mttrDaysBySeverity,
            Map<AhsSeverity, SlaBucket> slaBySeverity,
            double automationRate,
            Instant calculatedAt) {}

    public record BurnDownPoint(LocalDate day, long open) {}

    public record SlaBucket(int inSla, int gesamt) {
        public double quote() {
            return gesamt == 0 ? 1.0 : (double) inSla / gesamt;
        }
    }
}
