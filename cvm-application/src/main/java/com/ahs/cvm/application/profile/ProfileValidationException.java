package com.ahs.cvm.application.profile;

/**
 * Wird geworfen, wenn YAML-Syntax oder Profil-Schema verletzt werden. Die
 * Nachricht ist deutschsprachig und feldgenau.
 */
public class ProfileValidationException extends RuntimeException {

    public ProfileValidationException(String message) {
        super(message);
    }

    public ProfileValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
