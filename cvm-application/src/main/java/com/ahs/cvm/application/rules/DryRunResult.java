package com.ahs.cvm.application.rules;

import com.ahs.cvm.domain.enums.AhsSeverity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Ergebnis eines Dry-Run-Laufs gegen historische Findings. Wird in
 * {@code rule_dry_run_result} persistiert und im Admin-UI (Iteration 08)
 * zurueckgeliefert.
 */
public record DryRunResult(
        UUID ruleId,
        Instant rangeStart,
        Instant rangeEnd,
        int totalFindings,
        int matchedFindings,
        int matchedAlreadyApproved,
        List<Conflict> conflicts) {

    public record Conflict(
            UUID findingId,
            UUID assessmentId,
            AhsSeverity approvedSeverity,
            AhsSeverity ruleSeverity) {}
}
