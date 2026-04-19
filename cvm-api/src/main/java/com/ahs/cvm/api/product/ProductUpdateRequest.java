package com.ahs.cvm.api.product;

import jakarta.validation.constraints.Size;

/**
 * Eingabe-DTO fuer {@code PUT /api/v1/products/{id}} (Iteration 37,
 * CVM-81). Der {@code key} bleibt bewusst unveraendert - SBOM-Uploads
 * referenzieren Produkte ueber den Key.
 */
public record ProductUpdateRequest(
        @Size(max = 200)
        String name,
        @Size(max = 1000)
        String description) {}
