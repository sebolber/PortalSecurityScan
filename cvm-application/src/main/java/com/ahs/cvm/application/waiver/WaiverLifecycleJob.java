package com.ahs.cvm.application.waiver;

import com.ahs.cvm.application.alert.AlertContext;
import com.ahs.cvm.application.alert.AlertEvaluator;
import com.ahs.cvm.domain.enums.AlertTriggerArt;
import com.ahs.cvm.domain.enums.WaiverStatus;
import com.ahs.cvm.persistence.assessment.AssessmentRepository;
import com.ahs.cvm.persistence.waiver.Waiver;
import com.ahs.cvm.persistence.waiver.WaiverRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Taeglicher Lifecycle-Job: ACTIVE -&gt; EXPIRING_SOON (30d vor
 * validUntil) -&gt; EXPIRED. Bei EXPIRING_SOON wird ein Alert
 * {@link AlertTriggerArt#ESKALATION_T1} (Weiterverwendung des
 * bestehenden T1-Triggers) ausgeloest - der Alert selbst hat einen
 * waiver-spezifischen Text. Bei EXPIRED wird das Assessment auf
 * NEEDS_REVIEW gehoben (Iteration 20, CVM-51).
 */
@Component
public class WaiverLifecycleJob {

    private static final Logger log = LoggerFactory.getLogger(WaiverLifecycleJob.class);

    private final WaiverRepository waiverRepository;
    private final AssessmentRepository assessmentRepository;
    private final AlertEvaluator alertEvaluator;
    private final Clock clock;
    private final boolean schedulerEnabled;

    public WaiverLifecycleJob(
            WaiverRepository waiverRepository,
            AssessmentRepository assessmentRepository,
            AlertEvaluator alertEvaluator,
            Clock clock,
            @Value("${cvm.scheduler.enabled:false}") boolean schedulerEnabled) {
        this.waiverRepository = waiverRepository;
        this.assessmentRepository = assessmentRepository;
        this.alertEvaluator = alertEvaluator;
        this.clock = clock;
        this.schedulerEnabled = schedulerEnabled;
    }

    @Scheduled(cron = "${cvm.waiver.cron:0 0 1 * * *}")
    @Transactional
    public void scheduledRun() {
        if (!schedulerEnabled) {
            return;
        }
        runOnce();
    }

    @Transactional
    public JobReport runOnce() {
        Instant now = Instant.now(clock);
        Instant soonCutoff = now.plus(WaiverService.expiringSoonWindow());
        int toSoon = 0, toExpired = 0, alertsFired = 0;

        // ACTIVE -> EXPIRING_SOON
        for (Waiver w : waiverRepository.findByStatus(WaiverStatus.ACTIVE)) {
            if (w.getValidUntil() != null && !w.getValidUntil().isAfter(now)) {
                // direkt zu EXPIRED (z.B. backdatiertes validUntil).
                toExpired += expire(w, now);
                continue;
            }
            if (w.getValidUntil() != null && !w.getValidUntil().isAfter(soonCutoff)) {
                w.setStatus(WaiverStatus.EXPIRING_SOON);
                waiverRepository.save(w);
                toSoon++;
                if (fireAlert(w, "EXPIRING_SOON")) {
                    alertsFired++;
                }
            }
        }

        // EXPIRING_SOON -> EXPIRED
        for (Waiver w : waiverRepository.findByStatusAndValidUntilBefore(
                WaiverStatus.EXPIRING_SOON, now)) {
            toExpired += expire(w, now);
        }

        log.info("Waiver-Lifecycle: {} -> EXPIRING_SOON, {} -> EXPIRED, {} Alerts",
                toSoon, toExpired, alertsFired);
        return new JobReport(toSoon, toExpired, alertsFired);
    }

    private int expire(Waiver w, Instant now) {
        w.setStatus(WaiverStatus.EXPIRED);
        waiverRepository.save(w);
        if (w.getAssessment() != null) {
            assessmentRepository.markiereAlsReview(
                    List.of(w.getAssessment().getId()), null);
        }
        fireAlert(w, "EXPIRED");
        return 1;
    }

    private boolean fireAlert(Waiver w, String phase) {
        try {
            alertEvaluator.evaluate(new AlertContext(
                    AlertTriggerArt.ESKALATION_T1,
                    "waiver|" + phase + "|" + w.getId(),
                    null,
                    w.getAssessment() == null || w.getAssessment().getCve() == null
                            ? null : w.getAssessment().getCve().getId(),
                    w.getAssessment() == null ? null : w.getAssessment().getId(),
                    null, null,
                    "Waiver " + w.getId() + " ist " + phase,
                    Instant.now(clock),
                    Map.of("waiverId", w.getId().toString(), "phase", phase)));
            return true;
        } catch (RuntimeException ex) {
            log.warn("Waiver-Alert fehlgeschlagen: {}", ex.getMessage());
            return false;
        }
    }

    public record JobReport(int toExpiringSoon, int toExpired, int alertsFired) {}
}
