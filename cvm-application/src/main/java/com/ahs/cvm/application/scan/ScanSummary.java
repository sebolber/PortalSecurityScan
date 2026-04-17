package com.ahs.cvm.application.scan;

import java.time.Instant;
import java.util.UUID;

/**
 * Kompakte Sicht auf einen Scan fuer Status-Abfragen.
 */
public record ScanSummary(
        UUID scanId,
        UUID productVersionId,
        UUID environmentId,
        String scanner,
        String contentSha256,
        Instant scannedAt,
        int componentCount,
        int findingCount) {}
