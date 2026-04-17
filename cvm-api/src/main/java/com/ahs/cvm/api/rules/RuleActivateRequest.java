package com.ahs.cvm.api.rules;

import jakarta.validation.constraints.NotBlank;

public record RuleActivateRequest(@NotBlank String approverId) {}
