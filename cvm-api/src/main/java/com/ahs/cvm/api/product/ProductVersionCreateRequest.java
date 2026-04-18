package com.ahs.cvm.api.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/**
 * Eingabe-DTO fuer {@code POST /api/v1/products/{id}/versions}.
 */
public record ProductVersionCreateRequest(
        @NotBlank
        @Size(max = 64)
        String version,
        @Size(max = 64)
        String gitCommit,
        Instant releasedAt) {}
