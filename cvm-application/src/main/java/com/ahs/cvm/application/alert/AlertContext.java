package com.ahs.cvm.application.alert;

import com.ahs.cvm.domain.enums.AhsSeverity;
import com.ahs.cvm.domain.enums.AlertTriggerArt;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Trigger-Kontext fuer den {@link AlertEvaluator}. Aenderungen am
 * Bewertungs-Workflow loesen einen {@code AlertContext} aus, der die
 * Regel-Auswertung steuert.
 *
 * <p>Der {@code triggerKey} ist eine fachliche Identitaet fuer Cooldown-
 * Tracking, z.&nbsp;B. {@code "CVE-2025-12345|prod-test"}.
 *
 * <p>{@code attributes} bietet eine offene Map fuer Template-Daten
 * (z.&nbsp;B. {@code cveId}, {@code componentName}, {@code productName}).
 */
public record AlertContext(
        AlertTriggerArt triggerArt,
        String triggerKey,
        AhsSeverity severity,
        UUID cveId,
        UUID assessmentId,
        UUID productVersionId,
        UUID environmentId,
        String summary,
        Instant occurredAt,
        Map<String, Object> attributes) {

    public AlertContext {
        if (triggerArt == null) {
            throw new IllegalArgumentException("triggerArt darf nicht null sein");
        }
        if (triggerKey == null || triggerKey.isBlank()) {
            throw new IllegalArgumentException("triggerKey darf nicht leer sein");
        }
        if (occurredAt == null) {
            throw new IllegalArgumentException("occurredAt darf nicht null sein");
        }
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}
