package com.ahs.cvm.ai.anomaly;

import com.ahs.cvm.application.alert.AlertContext;
import com.ahs.cvm.application.alert.AlertEvaluator;
import com.ahs.cvm.domain.enums.AhsSeverity;
import com.ahs.cvm.domain.enums.AlertTriggerArt;
import com.ahs.cvm.persistence.anomaly.AnomalyEvent;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Stuendlicher Anomalie-Check (Iteration 18, CVM-43). Findet neue
 * Anomalie-Events und delegiert sie an den {@link AlertEvaluator}
 * (Trigger {@link AlertTriggerArt#KI_ANOMALIE}).
 */
@Component
public class AnomalyCheckJob {

    private static final Logger log = LoggerFactory.getLogger(AnomalyCheckJob.class);

    private final AnomalyConfig config;
    private final AnomalyDetectionService detectionService;
    private final AlertEvaluator alertEvaluator;
    private final boolean schedulerEnabled;

    public AnomalyCheckJob(
            AnomalyConfig config,
            AnomalyDetectionService detectionService,
            AlertEvaluator alertEvaluator,
            @Value("${cvm.scheduler.enabled:false}") boolean schedulerEnabled) {
        this.config = config;
        this.detectionService = detectionService;
        this.alertEvaluator = alertEvaluator;
        this.schedulerEnabled = schedulerEnabled;
    }

    @Scheduled(cron = "${cvm.ai.anomaly.cron:0 0 * * * *}")
    @Transactional
    public void scheduledRun() {
        if (!config.enabled() || !schedulerEnabled) {
            return;
        }
        runOnce();
    }

    @Transactional
    public JobReport runOnce() {
        Instant since = Instant.now().minus(Duration.ofHours(24));
        List<AnomalyEvent> events = detectionService.check(since);
        int alertsGefeuert = 0;
        for (AnomalyEvent e : events) {
            try {
                AlertContext ctx = new AlertContext(
                        AlertTriggerArt.KI_ANOMALIE,
                        e.getPattern() + "|" + e.getAssessmentId(),
                        AhsSeverity.MEDIUM,
                        null, e.getAssessmentId(), null, null,
                        e.getReason(),
                        e.getTriggeredAt(),
                        attrs(e));
                alertEvaluator.evaluate(ctx);
                alertsGefeuert++;
            } catch (RuntimeException ex) {
                log.warn("Alert-Trigger fuer Anomalie {} fehlgeschlagen: {}",
                        e.getId(), ex.getMessage());
            }
        }
        log.info("AnomalyCheck: {} neue Events, {} Alerts getriggert.",
                events.size(), alertsGefeuert);
        return new JobReport(events.size(), alertsGefeuert);
    }

    private static Map<String, Object> attrs(AnomalyEvent e) {
        Map<String, Object> m = new HashMap<>();
        m.put("pattern", e.getPattern());
        m.put("anomalySeverity", e.getSeverity());
        m.put("reason", e.getReason());
        m.put("assessmentId", e.getAssessmentId().toString());
        return m;
    }

    public record JobReport(int events, int alertsFired) {}
}
