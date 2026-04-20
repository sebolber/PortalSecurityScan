package com.ahs.cvm.application.dashboard;

import com.ahs.cvm.domain.enums.AhsSeverity;
import com.ahs.cvm.persistence.assessment.Assessment;
import com.ahs.cvm.persistence.assessment.AssessmentRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Iteration 100 (CVM-342): Liefert die KPI-Zahlen fuer das
 * Dashboard. Basis sind alle aktiven (nicht superseded) Assessments
 * mit offenem Status (PROPOSED/NEEDS_REVIEW/NEEDS_VERIFICATION).
 */
@Service
public class DashboardKpiService {

    /** Ab diesem Alter gilt ein offenes CRITICAL als T2-Eskalation. */
    public static final long WEITERBETRIEB_SCHWELLE_TAGE = 14;

    private final AssessmentRepository repository;
    private final Clock clock;

    public DashboardKpiService(AssessmentRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public DashboardKpiView berechne() {
        List<Assessment> offen = repository
                .findeQueueNachStatus(null, null, null, null)
                .stream()
                .filter(this::istOffen)
                .toList();
        Map<AhsSeverity, Long> verteilung = zaehleProSeverity(offen);
        DashboardKpiView.AeltesteKritisch aelteste = aelteste(offen);
        boolean ok = aelteste == null
                || aelteste.tage() < WEITERBETRIEB_SCHWELLE_TAGE;
        return new DashboardKpiView(
                offen.size(),
                verteilung,
                aelteste,
                ok);
    }

    private boolean istOffen(Assessment a) {
        return switch (a.getStatus()) {
            case PROPOSED, NEEDS_REVIEW, NEEDS_VERIFICATION -> true;
            default -> false;
        };
    }

    private Map<AhsSeverity, Long> zaehleProSeverity(List<Assessment> offen) {
        Map<AhsSeverity, Long> map = new EnumMap<>(AhsSeverity.class);
        for (AhsSeverity s : AhsSeverity.values()) {
            map.put(s, 0L);
        }
        for (Assessment a : offen) {
            map.merge(a.getSeverity(), 1L, Long::sum);
        }
        return map;
    }

    private DashboardKpiView.AeltesteKritisch aelteste(List<Assessment> offen) {
        Instant jetzt = clock.instant();
        return offen.stream()
                .filter(a -> a.getSeverity() == AhsSeverity.CRITICAL)
                .filter(a -> a.getCreatedAt() != null)
                .min(Comparator.comparing(Assessment::getCreatedAt))
                .map(a -> new DashboardKpiView.AeltesteKritisch(
                        a.getCve().getCveId(),
                        Duration.between(a.getCreatedAt(), jetzt).toDays()))
                .orElse(null);
    }
}
