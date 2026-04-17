package com.ahs.cvm.application.rules;

/**
 * Vier-Augen-Prinzip fuer Regel-Aktivierung verletzt: Approver ist identisch
 * mit Autor. Siehe {@code CLAUDE.md} Abschnitt 9.
 */
public class RuleFourEyesViolationException extends RuntimeException {

    public RuleFourEyesViolationException(String message) {
        super(message);
    }
}
