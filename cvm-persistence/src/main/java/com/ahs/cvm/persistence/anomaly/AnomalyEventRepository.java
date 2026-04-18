package com.ahs.cvm.persistence.anomaly;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnomalyEventRepository extends JpaRepository<AnomalyEvent, UUID> {

    List<AnomalyEvent> findByTriggeredAtAfterOrderByTriggeredAtDesc(Instant since);

    long countByTriggeredAtAfter(Instant since);

    boolean existsByAssessmentIdAndPattern(UUID assessmentId, String pattern);
}
