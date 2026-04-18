package com.ahs.cvm.ai.anomaly;

import com.ahs.cvm.persistence.anomaly.AnomalyEvent;
import com.ahs.cvm.persistence.anomaly.AnomalyEventRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Lese-API auf die Anomalie-Events (Iteration 18, CVM-43). */
@Service
public class AnomalyQueryService {

    private final AnomalyEventRepository repository;

    public AnomalyQueryService(AnomalyEventRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<AnomalyView> liste(Instant since) {
        return repository.findByTriggeredAtAfterOrderByTriggeredAtDesc(since)
                .stream().map(AnomalyView::from).toList();
    }

    @Transactional(readOnly = true)
    public long count24h(Instant since) {
        return repository.countByTriggeredAtAfter(since);
    }

    public record AnomalyView(
            UUID id, UUID assessmentId, String pattern,
            String severity, String reason, Instant triggeredAt) {
        public static AnomalyView from(AnomalyEvent e) {
            return new AnomalyView(e.getId(), e.getAssessmentId(), e.getPattern(),
                    e.getSeverity(), e.getReason(), e.getTriggeredAt());
        }
    }
}
