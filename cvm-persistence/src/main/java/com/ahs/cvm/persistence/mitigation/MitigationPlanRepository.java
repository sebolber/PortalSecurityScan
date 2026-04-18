package com.ahs.cvm.persistence.mitigation;

import com.ahs.cvm.domain.enums.FixVerificationGrade;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MitigationPlanRepository extends JpaRepository<MitigationPlan, UUID> {
    List<MitigationPlan> findByAssessmentId(UUID assessmentId);

    /**
     * Letzte N Mitigations absteigend nach {@code createdAt} fuer die
     * Uebersichtsseite der Fix-Verifikation (Iteration 27e, CVM-65).
     */
    List<MitigationPlan> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * Mitigations mit einem bestimmten Quality-Grade. Wird fuer den
     * Filter auf der Fix-Verifikations-Uebersichtsseite genutzt.
     */
    List<MitigationPlan> findByVerificationGradeOrderByCreatedAtDesc(
            FixVerificationGrade grade, Pageable pageable);
}
