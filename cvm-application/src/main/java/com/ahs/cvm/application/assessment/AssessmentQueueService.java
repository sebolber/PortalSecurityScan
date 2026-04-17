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

    @Transactional(readOnly = true)
    public List<FindingQueueView> findeOffene(QueueFilter filter) {
        if (filter.status() != null
                && filter.status() != AssessmentStatus.PROPOSED
                && filter.status() != AssessmentStatus.NEEDS_REVIEW) {
            return List.of();
        }
        return assessmentRepository
                .findeOffeneQueue(
                        filter.environmentId(),
                        filter.productVersionId(),
                        filter.source())
                .stream()
                .filter(a -> filter.status() == null || a.getStatus() == filter.status())
                .map(FindingQueueView::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<FindingQueueView> findeEintrag(UUID assessmentId) {
        return assessmentRepository.findById(assessmentId).map(FindingQueueView::from);
    }

    public record QueueFilter(
            AssessmentStatus status,
            UUID environmentId,
            UUID productVersionId,
            ProposalSource source) {}
}
