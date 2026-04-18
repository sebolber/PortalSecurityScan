package com.ahs.cvm.application.assessment;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Wird vom {@link AssessmentWriteService} publiziert, nachdem der
 * Expiry-Job eine Reihe von APPROVED-Assessments auf EXPIRED gesetzt
 * hat. Listener (z.&nbsp;B. Re-Propose-Dispatch oder Alert-Versand)
 * verarbeiten die Menge asynchron.
 */
public record AssessmentExpiredEvent(
        List<UUID> assessmentIds,
        Instant expiredAt) {

    public AssessmentExpiredEvent {
        assessmentIds = assessmentIds == null ? List.of() : List.copyOf(assessmentIds);
    }
}
