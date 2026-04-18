package com.ahs.cvm.llm;

/**
 * Wird geworfen, wenn der Output-Validator die Antwort des Modells
 * verwirft (Schema-Verletzung, ungueltige Severity, Anweisungs-Muster
 * im Rationale). Das Audit wird mit {@code status=INVALID_OUTPUT}
 * finalisiert.
 */
public class InvalidLlmOutputException extends RuntimeException {

    public InvalidLlmOutputException(String reason) {
        super("LLM-Output abgelehnt: " + reason);
    }
}
