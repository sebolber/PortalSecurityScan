package com.ahs.cvm.api.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Eingabe-DTO fuer {@code POST /api/v1/products}.
 *
 * <p>{@code key} muss kleingeschrieben, Ziffern und Bindestriche sein
 * (URL-tauglich). {@code name} ist der fachliche Anzeigename.
 */
public record ProductCreateRequest(
        @NotBlank
        @Pattern(regexp = "^[a-z0-9-]{2,64}$",
                message = "key muss ^[a-z0-9-]{2,64}$ erfuellen")
        String key,
        @NotBlank
        @Size(max = 200)
        String name,
        @Size(max = 1000)
        String description) {}
