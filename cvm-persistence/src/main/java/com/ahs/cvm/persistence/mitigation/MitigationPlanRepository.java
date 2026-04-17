package com.ahs.cvm.persistence.mitigation;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MitigationPlanRepository extends JpaRepository<MitigationPlan, UUID> {
    List<MitigationPlan> findByAssessmentId(UUID assessmentId);
}
