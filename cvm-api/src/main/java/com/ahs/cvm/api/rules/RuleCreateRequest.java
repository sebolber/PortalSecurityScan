package com.ahs.cvm.api.rules;

import com.ahs.cvm.domain.enums.AhsSeverity;
import com.ahs.cvm.domain.enums.RuleOrigin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record RuleCreateRequest(
        @NotBlank String ruleKey,
        @NotBlank String name,
        String description,
        @NotNull AhsSeverity proposedSeverity,
        @NotBlank String conditionJson,
        @NotBlank String rationaleTemplate,
        List<String> rationaleSourceFields,
        RuleOrigin origin,
        @NotBlank String createdBy) {}
