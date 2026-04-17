package com.ahs.cvm.api.assessment;

import com.ahs.cvm.application.assessment.FindingQueueView;
import com.ahs.cvm.domain.enums.AhsSeverity;
import com.ahs.cvm.domain.enums.AssessmentStatus;
import com.ahs.cvm.domain.enums.ProposalSource;
import java.time.Instant;
import java.util.UUID;

/** Antwort-DTO fuer Assessment-Aktionen und Queue-Eintraege. */
public record AssessmentResponse(
        UUID id,
        UUID findingId,
        UUID cveId,
        String cveKey,
        AhsSeverity severity,
        AssessmentStatus status,
        ProposalSource source,
        String rationale,
        String decidedBy,
        Integer version,
        Instant createdAt) {

    public static AssessmentResponse from(FindingQueueView view) {
        return new AssessmentResponse(
                view.assessmentId(),
                view.findingId(),
                view.cveId(),
                view.cveKey(),
                view.severity(),
                view.status(),
                view.source(),
                view.rationale(),
                view.decidedBy(),
                view.version(),
                view.createdAt());
    }
}
