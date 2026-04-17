package com.ahs.cvm.api.assessment;

import com.ahs.cvm.domain.enums.MitigationStrategy;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

/** Body fuer {@code POST /api/v1/assessments/{id}/approve}. */
public record AssessmentApproveRequest(
        @NotBlank String approverId,
        MitigationStrategy strategy,
        String targetVersion,
        Instant plannedFor,
        String mitigationNotes) {}
