package com.ahs.cvm.application.assessment;

import com.ahs.cvm.domain.enums.AhsSeverity;
import java.time.Instant;
import java.util.UUID;

/**
 * Domain-Event: ein Assessment wurde im Status {@code APPROVED}
 * freigegeben. Iteration 09 (SMTP-Alerts) haengt einen Alert-Listener an;
 * in Iteration 06 existiert nur ein Logger-Listener.
 */
public record AssessmentApprovedEvent(
        UUID assessmentId,
        UUID findingId,
        UUID cveId,
        UUID environmentId,
        UUID productVersionId,
        AhsSeverity severity,
        String approverId,
        Instant approvedAt) {}
