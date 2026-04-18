package com.ahs.cvm.application.alert;

import com.ahs.cvm.application.assessment.AssessmentQueueService;
import com.ahs.cvm.application.assessment.AssessmentQueueService.QueueFilter;
import com.ahs.cvm.application.assessment.FindingQueueView;
import com.ahs.cvm.domain.enums.AhsSeverity;
import com.ahs.cvm.domain.enums.AlertTriggerArt;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Cron-Job (alle 5&nbsp;min): findet offene CRITICAL-Vorschlaege, die
 * laenger als T1 bzw. T2 unbearbeitet sind, und triggert die
 * entsprechenden Eskalations-Alerts.
 */
@Component
public class AlertEscalationJob {

    private static final Logger log = LoggerFactory.getLogger(AlertEscalationJob.class);

    private final AssessmentQueueService queueService;
    private final AlertEvaluator evaluator;
    private final AlertConfig config;
    private final Clock clock;

    public AlertEscalationJob(
            AssessmentQueueService queueService,
            AlertEvaluator evaluator,
            AlertConfig config,
            Clock clock) {
        this.queueService = queueService;
        this.evaluator = evaluator;
        this.config = config;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "PT5M", initialDelayString = "PT1M")
    public void pruefeEskalationen() {
        runOnce();
    }

    /** Ein einzelner Lauf, fuer Tests direkt aufrufbar. */
    public int runOnce() {
        Instant jetzt = Instant.now(clock);
        Duration t1 = config.t1();
        Duration t2 = config.t2();
        List<FindingQueueView> offen = queueService.findeOffene(
                new QueueFilter(null, null, null, null));
        int getriggert = 0;
        for (FindingQueueView v : offen) {
            if (v.severity() != AhsSeverity.CRITICAL) {
                continue;
            }
            Duration alter = Duration.between(v.createdAt(), jetzt);
            if (alter.compareTo(t2) >= 0) {
                evaluator.evaluate(buildContext(v, AlertTriggerArt.ESKALATION_T2, alter, jetzt));
                getriggert++;
            } else if (alter.compareTo(t1) >= 0) {
                evaluator.evaluate(buildContext(v, AlertTriggerArt.ESKALATION_T1, alter, jetzt));
                getriggert++;
            }
        }
        log.debug("Eskalations-Job: {} Trigger evaluiert", getriggert);
        return getriggert;
    }

    private AlertContext buildContext(
            FindingQueueView v,
            AlertTriggerArt art,
            Duration alter,
            Instant jetzt) {
        return new AlertContext(
                art,
                v.cveKey() + "|" + v.environmentId() + "|" + art.name(),
                v.severity(),
                v.cveId(),
                v.assessmentId(),
                v.productVersionId(),
                v.environmentId(),
                "Offener Vorschlag " + v.cveKey() + " seit "
                        + alter.toMinutes() + " Minuten",
                jetzt,
                Map.of(
                        "cveKey", v.cveKey() == null ? "" : v.cveKey(),
                        "alterMinuten", String.valueOf(alter.toMinutes()),
                        "umgebung", String.valueOf(v.environmentId()),
                        "produktVersion", String.valueOf(v.productVersionId()),
                        "quelle", v.source() == null ? "" : v.source().name()));
    }
}
