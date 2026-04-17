package com.ahs.cvm.application.rules;

/**
 * Wird geworfen, wenn eine Regel-Condition syntaktisch oder semantisch
 * unzulaessig ist (unbekannter Operator, Pfad-Praefix, Typ-Mismatch).
 * Die Nachricht ist deutschsprachig und feldgenau.
 */
public class RuleConditionException extends RuntimeException {

    public RuleConditionException(String message) {
        super(message);
    }

    public RuleConditionException(String message, Throwable cause) {
        super(message, cause);
    }
}
