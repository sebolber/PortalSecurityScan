package com.ahs.cvm.application.profile;

/**
 * Vier-Augen-Prinzip verletzt: der Approver ist identisch mit dem Autor eines
 * Profil-Drafts. Siehe {@code CLAUDE.md} Abschnitt 9.
 */
public class FourEyesViolationException extends RuntimeException {

    public FourEyesViolationException(String message) {
        super(message);
    }
}
