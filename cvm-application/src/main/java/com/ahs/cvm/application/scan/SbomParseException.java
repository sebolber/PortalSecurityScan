package com.ahs.cvm.application.scan;

/**
 * Wird ausgeloest, wenn die SBOM zwar gelesen, aber nicht als CycloneDX
 * interpretiert werden kann (Invalides JSON, unbekannte Feldtypen,...).
 */
public class SbomParseException extends RuntimeException {
    public SbomParseException(String message, Throwable cause) {
        super(message, cause);
    }

    public SbomParseException(String message) {
        super(message);
    }
}
