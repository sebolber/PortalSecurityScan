package com.ahs.cvm.application.scan;

/**
 * Wird ausgeloest, wenn Pflichtfelder im CycloneDX-BOM fehlen
 * ({@code bomFormat}, {@code specVersion}, Komponenten-Liste leer, ...).
 */
public class SbomSchemaException extends RuntimeException {
    public SbomSchemaException(String message) {
        super(message);
    }
}
