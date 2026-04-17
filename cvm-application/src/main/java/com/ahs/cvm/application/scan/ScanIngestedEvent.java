package com.ahs.cvm.application.scan;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain-Event: eine SBOM wurde erfolgreich verarbeitet und alle Findings
 * persistiert. Wird vom {@link ScanIngestService} asynchron publiziert.
 *
 * <p>In Iteration 02 gibt es noch keinen Listener. Iteration 05 (Regel-Engine)
 * und 06 (Workflow) abonnieren das Event.
 */
public record ScanIngestedEvent(
        UUID scanId,
        UUID productVersionId,
        UUID environmentId,
        int componentCount,
        int findingCount,
        Instant ingestedAt) {}
