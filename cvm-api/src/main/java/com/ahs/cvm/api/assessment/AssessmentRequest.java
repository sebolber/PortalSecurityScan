package com.ahs.cvm.api.assessment;

import com.ahs.cvm.domain.enums.AhsSeverity;
import com.ahs.cvm.domain.enums.MitigationStrategy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Body fuer {@code POST /api/v1/assessments}. */
public record AssessmentRequest(
        @NotNull UUID findingId,
        @NotNull AhsSeverity ahsSeverity,
        @NotBlank String rationale,
        List<String> sourceFields,
        @NotBlank String decidedBy,
        UUID productVersionId,
        UUID environmentId,
        MitigationStrategy strategy,
        String targetVersion,
        Instant plannedFor,
        String mitigationNotes) {}
