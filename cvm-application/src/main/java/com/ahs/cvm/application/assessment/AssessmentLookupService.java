package com.ahs.cvm.application.assessment;

import com.ahs.cvm.domain.enums.AssessmentStatus;
import com.ahs.cvm.persistence.assessment.Assessment;
import com.ahs.cvm.persistence.assessment.AssessmentRepository;
import com.ahs.cvm.persistence.assessment.ImmutabilityException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lese- und Versionierungszugriff auf Assessments.
 *
 * <p>Unveraenderlichkeit: Aktualisierungen bestehender Zeilen sind verboten
 * ({@link ImmutabilityException}). Einzige Ausnahme ist das Ueberholen eines
 * Assessments &uuml;ber {@link #markiereAlsUeberholt(UUID)}.
 */
@Service
public class AssessmentLookupService {

    private final AssessmentRepository assessmentRepository;

    public AssessmentLookupService(AssessmentRepository assessmentRepository) {
        this.assessmentRepository = assessmentRepository;
    }

    @Transactional(readOnly = true)
    public Optional<Assessment> aktuellesAssessment(UUID findingId) {
        return assessmentRepository.findFirstByFindingIdOrderByVersionDesc(findingId);
    }

    @Transactional(readOnly = true)
    public List<Assessment> findeAktiveFreigaben(
            UUID cveId, UUID produktVersionId, UUID umgebungId) {
        return assessmentRepository
                .findByCveIdAndProductVersionIdAndEnvironmentIdAndStatusAndSupersededAtIsNull(
                        cveId, produktVersionId, umgebungId, AssessmentStatus.APPROVED);
    }

    @Transactional
    public Assessment speichereNeueVersion(Assessment assessment) {
        if (assessment.getId() != null && assessmentRepository.existsById(assessment.getId())) {
            throw new ImmutabilityException(
                    "Assessment %s existiert bereits. Neue Version anlegen."
                            .formatted(assessment.getId()));
        }
        return assessmentRepository.save(assessment);
    }

    @Transactional
    public Assessment markiereAlsUeberholt(UUID assessmentId) {
        Assessment bestehend = assessmentRepository
                .findById(assessmentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unbekanntes Assessment: " + assessmentId));
        bestehend.markiereAlsUeberholt(Instant.now());
        return assessmentRepository.save(bestehend);
    }
}
