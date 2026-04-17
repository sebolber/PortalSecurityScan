package com.ahs.cvm.api.profile;

import jakarta.validation.constraints.NotBlank;

public record ProfilePutRequest(
        @NotBlank String yamlSource,
        @NotBlank String proposedBy) {}
