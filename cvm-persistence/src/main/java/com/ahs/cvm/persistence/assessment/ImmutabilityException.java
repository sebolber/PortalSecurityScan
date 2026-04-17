package com.ahs.cvm.persistence.assessment;

/**
 * Wird ausgeloest, wenn jemand versucht, ein bereits persistiertes Assessment
 * zu aendern. Assessments sind immutable-versioniert; Aenderungen erfolgen durch
 * Einfuegen einer neuen Zeile mit erhoehter Version.
 */
public class ImmutabilityException extends RuntimeException {

    public ImmutabilityException(String message) {
        super(message);
    }
}
