package com.ahs.cvm.application.waiver;

import com.ahs.cvm.domain.enums.MitigationStrategy;
import com.ahs.cvm.domain.enums.WaiverStatus;
import com.ahs.cvm.persistence.assessment.Assessment;
import com.ahs.cvm.persistence.assessment.AssessmentRepository;
import com.ahs.cvm.persistence.mitigation.MitigationPlan;
import com.ahs.cvm.persistence.mitigation.MitigationPlanRepository;
import com.ahs.cvm.persistence.waiver.Waiver;
import com.ahs.cvm.persistence.waiver.WaiverRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Waiver-CRUD mit Vier-Augen-Pruefung (Iteration 20, CVM-51).
 *
 * <p>Ein Waiver darf nur fuer Assessments mit Mitigation-Strategie
 * {@link MitigationStrategy#ACCEPT_RISK} oder
 * {@link MitigationStrategy#WORKAROUND} angelegt werden. Andere
 * Strategien ({@code UPGRADE}, {@code PATCH}, ...) brauchen keinen
 * Waiver, weil sie den Fix aktiv planen.
 */
@Service
public class WaiverService {

    private static final Logger log = LoggerFactory.getLogger(WaiverService.class);
    private static final Set<MitigationStrategy> WAIVER_FAEHIG = Set.of(
            MitigationStrategy.ACCEPT_RISK, MitigationStrategy.WORKAROUND);

    private final WaiverRepository repository;
    private final AssessmentRepository assessmentRepository;
    private final MitigationPlanRepository mitigationRepository;
    private final Clock clock;

    public WaiverService(
            WaiverRepository repository,
            AssessmentRepository assessmentRepository,
            MitigationPlanRepository mitigationRepository,
            Clock clock) {
        this.repository = repository;
        this.assessmentRepository = assessmentRepository;
        this.mitigationRepository = mitigationRepository;
        this.clock = clock;
    }

    @Transactional
    public WaiverView grant(GrantCommand cmd) {
        requireNotBlank(cmd.grantedBy(), "grantedBy");
        requireNotBlank(cmd.reason(), "reason");
        if (cmd.validUntil() == null) {
            throw new IllegalArgumentException("validUntil darf nicht null sein.");
        }
        if (!cmd.validUntil().isAfter(Instant.now(clock))) {
            throw new IllegalArgumentException("validUntil muss in der Zukunft liegen.");
        }
        Assessment assessment = assessmentRepository.findById(cmd.assessmentId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Assessment nicht gefunden: " + cmd.assessmentId()));

        List<MitigationPlan> plans = mitigationRepository.findByAssessmentId(
                assessment.getId());
        if (plans.isEmpty() || plans.stream().noneMatch(
                p -> WAIVER_FAEHIG.contains(p.getStrategy()))) {
            throw new WaiverNotApplicableException(
                    "Waiver nur fuer ACCEPT_RISK oder WORKAROUND - Assessment "
                            + assessment.getId() + " hat keine passende Strategie.");
        }

        if (Objects.equals(cmd.grantedBy(), assessment.getDecidedBy())) {
            throw new VierAugenViolationException(
                    "Vier-Augen-Verstoss: grantedBy == decidedBy (" + cmd.grantedBy() + ")");
        }

        int reviewInterval = cmd.reviewIntervalDays() == null
                || cmd.reviewIntervalDays() <= 0 ? 90 : cmd.reviewIntervalDays();

        Waiver waiver = repository.save(Waiver.builder()
                .assessment(assessment)
                .reason(cmd.reason())
                .grantedBy(cmd.grantedBy())
                .validUntil(cmd.validUntil())
                .reviewIntervalDays(reviewInterval)
                .status(WaiverStatus.ACTIVE)
                .build());
        log.info("Waiver {} angelegt fuer Assessment {} bis {}",
                waiver.getId(), assessment.getId(), cmd.validUntil());
        return WaiverView.from(waiver);
    }

    @Transactional
    public WaiverView extend(UUID waiverId, Instant neuesValidUntil, String extendedBy) {
        requireNotBlank(extendedBy, "extendedBy");
        if (neuesValidUntil == null) {
            throw new IllegalArgumentException("neuesValidUntil darf nicht null sein.");
        }
        Waiver w = repository.findById(waiverId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Waiver nicht gefunden: " + waiverId));
        if (w.getStatus() == WaiverStatus.REVOKED) {
            throw new IllegalStateException("Waiver ist REVOKED.");
        }
        if (!neuesValidUntil.isAfter(Instant.now(clock))) {
            throw new IllegalArgumentException("neuesValidUntil muss in der Zukunft liegen.");
        }
        if (Objects.equals(extendedBy, w.getGrantedBy())) {
            throw new VierAugenViolationException(
                    "Vier-Augen-Verstoss: extendedBy == grantedBy (" + extendedBy + ")");
        }
        w.setValidUntil(neuesValidUntil);
        w.setStatus(WaiverStatus.ACTIVE);
        w.setExtendedBy(extendedBy);
        w.setExtendedAt(Instant.now(clock));
        return WaiverView.from(repository.save(w));
    }

    @Transactional
    public WaiverView revoke(UUID waiverId, String revokedBy, String reason) {
        requireNotBlank(revokedBy, "revokedBy");
        requireNotBlank(reason, "reason");
        Waiver w = repository.findById(waiverId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Waiver nicht gefunden: " + waiverId));
        w.setStatus(WaiverStatus.REVOKED);
        w.setRevokedBy(revokedBy);
        w.setRevokedAt(Instant.now(clock));
        w.setReason(w.getReason() + "\n\n[REVOKED] " + reason);
        return WaiverView.from(repository.save(w));
    }

    @Transactional(readOnly = true)
    public List<WaiverView> byStatus(WaiverStatus status) {
        return repository.findByStatus(status).stream()
                .map(WaiverView::from).toList();
    }

    private static void requireNotBlank(String v, String name) {
        if (v == null || v.isBlank()) {
            throw new IllegalArgumentException(name + " darf nicht leer sein.");
        }
    }

    public record GrantCommand(
            UUID assessmentId,
            String reason,
            String grantedBy,
            Instant validUntil,
            Integer reviewIntervalDays) {}

    public static class VierAugenViolationException extends RuntimeException {
        public VierAugenViolationException(String message) {
            super(message);
        }
    }

    /** Konstante: Restlaufzeit ab der {@code EXPIRING_SOON} greift. */
    public static Duration expiringSoonWindow() {
        return Duration.ofDays(30);
    }
}
