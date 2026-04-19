package com.ahs.cvm.api.product;

import jakarta.validation.constraints.Size;

/**
 * Eingabe-DTO fuer {@code PUT /api/v1/products/{id}} (Iteration 37,
 * CVM-81). Der {@code key} bleibt bewusst unveraendert - SBOM-Uploads
 * referenzieren Produkte ueber den Key.
 *
 * <p>Iteration 76 (CVM-313): optionale {@code repoUrl} fuer den
 * Reachability-Agenten.
 */
public record ProductUpdateRequest(
        @Size(max = 200)
        String name,
        @Size(max = 1000)
        String description,
        @Size(max = 512)
        String repoUrl) {

    /** Bestands-Konstruktor-Kompatibilitaet fuer alte Clients. */
    public ProductUpdateRequest(String name, String description) {
        this(name, description, null);
    }
}
