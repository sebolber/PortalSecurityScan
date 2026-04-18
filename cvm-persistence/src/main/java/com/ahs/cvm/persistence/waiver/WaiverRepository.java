package com.ahs.cvm.persistence.waiver;

import com.ahs.cvm.domain.enums.WaiverStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WaiverRepository extends JpaRepository<Waiver, UUID> {

    List<Waiver> findByStatus(WaiverStatus status);

    List<Waiver> findByStatusAndValidUntilBefore(WaiverStatus status, Instant cutoff);

    List<Waiver> findByAssessmentId(UUID assessmentId);
}
