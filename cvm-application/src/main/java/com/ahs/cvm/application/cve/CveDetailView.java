package com.ahs.cvm.application.cve;

import com.ahs.cvm.domain.enums.AhsSeverity;
import com.ahs.cvm.domain.enums.AssessmentStatus;
import com.ahs.cvm.domain.enums.ProposalSource;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Detail-View einer CVE mit verknuepften Findings und Assessments
 * (Iteration 36, CVM-80).
 *
 * <p>Der {@link CveView} bleibt reine Metadaten-Projektion; dieser
 * Detail-View zieht Findings und aktuelle Assessments nach, damit
 * ein Reviewer Kontext zum Entscheiden hat.
 */
public record CveDetailView(
        CveView cve,
        List<FindingEntry> findings,
        List<AssessmentEntry> assessments) {

    public record FindingEntry(
            UUID findingId,
            UUID scanId,
            UUID componentOccurrenceId,
            String componentKey,
            String componentVersion,
            String fixedInVersion,
            Instant detectedAt,
            UUID productVersionId,
            UUID environmentId) {}

    public record AssessmentEntry(
            UUID assessmentId,
            UUID findingId,
            int version,
            AhsSeverity severity,
            AssessmentStatus status,
            ProposalSource proposalSource,
            String rationale,
            String decidedBy,
            Instant createdAt,
            Instant validUntil) {}
}
