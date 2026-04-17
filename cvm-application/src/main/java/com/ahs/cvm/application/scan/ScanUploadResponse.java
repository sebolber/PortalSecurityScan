package com.ahs.cvm.application.scan;

import java.util.UUID;

/**
 * Ergebnis eines akzeptierten Scan-Uploads. Die eigentliche Parsing- und
 * Persistenz-Arbeit laeuft asynchron.
 */
public record ScanUploadResponse(UUID scanId, String statusUri) {}
