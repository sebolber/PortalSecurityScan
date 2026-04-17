package com.ahs.cvm.api.rules;

import com.ahs.cvm.application.rules.DryRunResult;
import com.ahs.cvm.domain.enums.AhsSeverity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DryRunResponse(
        UUID ruleId,
        Instant rangeStart,
        Instant rangeEnd,
        int totalFindings,
        int matchedFindings,
        int matchedAlreadyApproved,
        List<ConflictEntry> conflicts) {

    public record ConflictEntry(
            UUID findingId,
            UUID assessmentId,
            AhsSeverity approvedSeverity,
            AhsSeverity ruleSeverity) {}

    public static DryRunResponse from(DryRunResult r) {
        List<ConflictEntry> c = r.conflicts().stream()
                .map(x -> new ConflictEntry(
                        x.findingId(), x.assessmentId(), x.approvedSeverity(), x.ruleSeverity()))
                .toList();
        return new DryRunResponse(
                r.ruleId(),
                r.rangeStart(),
                r.rangeEnd(),
                r.totalFindings(),
                r.matchedFindings(),
                r.matchedAlreadyApproved(),
                c);
    }
}
