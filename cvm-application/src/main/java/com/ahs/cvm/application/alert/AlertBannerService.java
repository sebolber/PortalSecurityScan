package com.ahs.cvm.application.alert;

import com.ahs.cvm.application.assessment.AssessmentQueueService;
import com.ahs.cvm.application.assessment.AssessmentQueueService.QueueFilter;
import com.ahs.cvm.application.assessment.FindingQueueView;
import com.ahs.cvm.domain.enums.AhsSeverity;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Liefert den T2-Banner-Status fuer das Frontend. Banner geht an,
 * sobald mindestens ein offener CRITICAL-Vorschlag aelter als die
 * T2-Schwelle ist.
 */
@Service
public class AlertBannerService {

    private final AssessmentQueueService queueService;
    private final AlertConfig config;
    private final Clock clock;

    public AlertBannerService(
            AssessmentQueueService queueService,
            AlertConfig config,
            Clock clock) {
        this.queueService = queueService;
        this.config = config;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public BannerStatus aktuellerStatus() {
        Instant jetzt = Instant.now(clock);
        Duration t2 = config.t2();
        List<FindingQueueView> offen = queueService.findeOffene(
                new QueueFilter(null, null, null, null));
        long ueberfaellig = offen.stream()
                .filter(a -> a.severity() == AhsSeverity.CRITICAL)
                .filter(a -> Duration.between(a.createdAt(), jetzt).compareTo(t2) >= 0)
                .count();
        return new BannerStatus(ueberfaellig > 0, (int) ueberfaellig, t2.toMinutes());
    }

    /**
     * Frontend-Schnittstelle. {@code visible} entscheidet, ob das rote
     * Banner gezeigt wird. {@code count} ist die Anzahl ueberfaelliger
     * CRITICAL-Vorschlaege; {@code t2Minutes} dient als Hilfstext.
     */
    public record BannerStatus(boolean visible, int count, long t2Minutes) {}
}
