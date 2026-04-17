package com.ahs.cvm.api.profile;

import jakarta.validation.constraints.NotBlank;

public record ProfileApproveRequest(@NotBlank String approverId) {}
