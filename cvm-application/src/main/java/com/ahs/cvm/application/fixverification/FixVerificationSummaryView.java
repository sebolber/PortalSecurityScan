package com.ahs.cvm.application.fixverification;

import com.ahs.cvm.domain.enums.FixEvidenceType;
import com.ahs.cvm.domain.enums.FixVerificationGrade;
import com.ahs.cvm.domain.enums.MitigationStatus;
import com.ahs.cvm.domain.enums.MitigationStrategy;
import com.ahs.cvm.persistence.mitigation.MitigationPlan;
import java.time.Instant;
import java.util.UUID;

/**
 * Lese-Projektion eines Mitigation-Plans mit Fokus auf den
 * Fix-Verifikations-Status (Iteration 27e, CVM-65). Wird auf
 * {@code GET /api/v1/fix-verification} zurueckgegeben.
 */
public record FixVerificationSummaryView(
        UUID id,
        UUID assessmentId,
        MitigationStatus status,
        MitigationStrategy strategy,
        String targetVersion,
        String owner,
        Instant plannedFor,
        Instant implementedAt,
        FixVerificationGrade verificationGrade,
        FixEvidenceType verificationEvidenceType,
        Instant verifiedAt,
        Instant createdAt) {

    public static FixVerificationSummaryView from(MitigationPlan plan) {
        return new FixVerificationSummaryView(
                plan.getId(),
                plan.getAssessment() == null ? null : plan.getAssessment().getId(),
                plan.getStatus(),
                plan.getStrategy(),
                plan.getTargetVersion(),
                plan.getOwner(),
                plan.getPlannedFor(),
                plan.getImplementedAt(),
                plan.getVerificationGrade(),
                plan.getVerificationEvidenceType(),
                plan.getVerifiedAt(),
                plan.getCreatedAt());
    }
}
