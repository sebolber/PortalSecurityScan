package com.ahs.cvm.application.assessment;

import com.ahs.cvm.domain.enums.AhsSeverity;
import com.ahs.cvm.domain.enums.AssessmentStatus;
import com.ahs.cvm.domain.enums.ProposalSource;
import com.ahs.cvm.persistence.assessment.Assessment;
import java.time.Instant;
import java.util.UUID;

/**
 * Read-Model fuer Queue-Eintraege. Haelt die Arch-Regel
 * {@code api -> persistence} aus dem Controller fern.
 */
public record FindingQueueView(
        UUID assessmentId,
        UUID findingId,
        UUID cveId,
        String cveKey,
        UUID productVersionId,
        UUID environmentId,
        AhsSeverity severity,
        AssessmentStatus status,
        ProposalSource source,
        String rationale,
        String decidedBy,
        Instant createdAt,
        Integer version) {

    public static FindingQueueView from(Assessment a) {
        return new FindingQueueView(
                a.getId(),
                a.getFinding().getId(),
                a.getCve().getId(),
                a.getCve().getCveId(),
                a.getProductVersion().getId(),
                a.getEnvironment().getId(),
                a.getSeverity(),
                a.getStatus(),
                a.getProposalSource(),
                a.getRationale(),
                a.getDecidedBy(),
                a.getCreatedAt(),
                a.getVersion());
    }
}
