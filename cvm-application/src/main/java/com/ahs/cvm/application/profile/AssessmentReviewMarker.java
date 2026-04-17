package com.ahs.cvm.application.profile;

import com.ahs.cvm.persistence.assessment.AssessmentRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hoert auf {@link ContextProfileActivatedEvent} und markiert alle
 * Assessments der Umgebung als {@code NEEDS_REVIEW}, die mindestens einen
 * der geaenderten Profilpfade in {@code rationaleSourceFields} fuehren.
 */
@Component
public class AssessmentReviewMarker {

    private static final Logger log = LoggerFactory.getLogger(AssessmentReviewMarker.class);

    private final AssessmentRepository assessmentRepository;

    public AssessmentReviewMarker(AssessmentRepository assessmentRepository) {
        this.assessmentRepository = assessmentRepository;
    }

    @EventListener
    @Transactional
    public void onActivation(ContextProfileActivatedEvent event) {
        if (event.changedPaths() == null || event.changedPaths().isEmpty()) {
            log.debug(
                    "Profil {} aktiviert, aber keine geaenderten Pfade - kein NEEDS_REVIEW.",
                    event.newProfileVersionId());
            return;
        }
        String[] pfade = event.changedPaths().toArray(String[]::new);
        List<UUID> ids = assessmentRepository.findAktiveIdsByEnvironmentAndSourceFields(
                event.environmentId(), pfade);
        if (ids.isEmpty()) {
            log.info(
                    "Profil {}: keine Assessments referenzieren die geaenderten Pfade.",
                    event.newProfileVersionId());
            return;
        }
        Set<UUID> uniqueIds = new HashSet<>(ids);
        int betroffen = assessmentRepository.markiereAlsReview(
                uniqueIds, event.newProfileVersionId());
        log.info(
                "Profil {}: {} Assessments auf NEEDS_REVIEW gesetzt (Pfade={}).",
                event.newProfileVersionId(),
                betroffen,
                event.changedPaths());
    }
}
