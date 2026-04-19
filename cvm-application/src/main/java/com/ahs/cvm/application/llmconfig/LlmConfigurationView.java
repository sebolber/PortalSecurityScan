package com.ahs.cvm.application.llmconfig;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Projektion einer {@code LlmConfiguration} fuers Frontend / API
 * (Iteration 34, CVM-78).
 *
 * <p>Der {@code secretRef} wird <strong>nicht</strong> im Klartext
 * ausgeliefert. Stattdessen liefert
 * {@link #secretSet}, ob ueberhaupt ein Secret hinterlegt ist, und
 * {@link #secretHint} gibt die letzten 4 Zeichen zurueck, damit man
 * in der UI sehen kann, welcher Key vermutlich vorliegt.
 */
public record LlmConfigurationView(
        UUID id,
        UUID tenantId,
        String name,
        String description,
        String provider,
        String model,
        String baseUrl,
        boolean secretSet,
        String secretHint,
        Integer maxTokens,
        BigDecimal temperature,
        boolean active,
        Instant createdAt,
        Instant updatedAt,
        String updatedBy) {}
