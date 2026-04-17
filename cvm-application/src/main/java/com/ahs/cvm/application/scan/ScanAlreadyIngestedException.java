package com.ahs.cvm.application.scan;

import java.util.UUID;

/**
 * Wird ausgeloest, wenn eine SBOM mit demselben content_sha256 fuer die
 * gleiche Produkt-Version und Umgebung bereits verarbeitet wurde.
 */
public class ScanAlreadyIngestedException extends RuntimeException {

    private final UUID existingScanId;

    public ScanAlreadyIngestedException(UUID existingScanId) {
        super("Scan mit identischem Inhalt wurde bereits verarbeitet: "
                + existingScanId);
        this.existingScanId = existingScanId;
    }

    public UUID getExistingScanId() {
        return existingScanId;
    }
}
