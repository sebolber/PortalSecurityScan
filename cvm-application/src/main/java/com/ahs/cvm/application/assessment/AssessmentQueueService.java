package com.ahs.cvm.application.assessment;

import com.ahs.cvm.domain.enums.AssessmentStatus;
import com.ahs.cvm.domain.enums.ProposalSource;
import com.ahs.cvm.persistence.assessment.AssessmentRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Queue-Service: liefert offene Bewertungsvorschlaege (Status
 * {@code PROPOSED} oder {@code NEEDS_REVIEW}) je Umgebung. Die Sortierung
 * erfolgt nach Severity (aufsteigend, d.&nbsp;h. CRITICAL zuerst) und
 * Entstehungszeit (FIFO).
 */
@Service
public class AssessmentQueueService {

    private final AssessmentRepository assessmentRepository;

    public AssessmentQueueService(AssessmentRepository assessmentRepository) {
        this.assessmentRepository = assessmentRepository;
    }

    /**
     * Iteration 99 (CVM-341): Queue-Liste. {@code status=null} liefert
     * alle aktuellen Assessments (ALLE-Chip im UI), sonst genau den
     * angegebenen Status. Superseded-Versionen bleiben raus.
     */
    @Transactional(readOnly = true)
    public List<FindingQueueView> findeOffene(QueueFilter filter) {
        return assessmentRepository
                .findeQueueNachStatus(
                        filter.status(),
                        filter.environmentId(),
                        filter.productVersionId(),
                        filter.source())
                .stream()
                .map(FindingQueueView::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<FindingQueueView> findeEintrag(UUID assessmentId) {
        return assessmentRepository.findById(assessmentId).map(FindingQueueView::from);
    }

    /**
     * Iteration 87 (CVM-327): liefert die komplette Assessment-
     * Historie fuer ein Finding (inkl. superseded Versionen). Wird
     * im Queue-Detail als "Historie"-Reiter gezeigt.
     */
    @Transactional(readOnly = true)
    public List<FindingQueueView> findHistorieByFinding(UUID findingId) {
        return assessmentRepository
                .findByFindingIdOrderByVersionAsc(findingId)
                .stream()
                .map(FindingQueueView::from)
                .toList();
    }

    public record QueueFilter(
            AssessmentStatus status,
            UUID environmentId,
            UUID productVersionId,
            ProposalSource source) {}
}
