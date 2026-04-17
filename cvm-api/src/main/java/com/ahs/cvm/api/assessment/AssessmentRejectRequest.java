package com.ahs.cvm.api.assessment;

import jakarta.validation.constraints.NotBlank;

/** Body fuer {@code POST /api/v1/assessments/{id}/reject}. */
public record AssessmentRejectRequest(
        @NotBlank String approverId,
        @NotBlank String comment) {}
