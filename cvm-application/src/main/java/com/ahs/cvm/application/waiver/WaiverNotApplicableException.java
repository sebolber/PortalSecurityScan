package com.ahs.cvm.application.waiver;

/**
 * Wird geworfen, wenn ein Waiver fuer ein Assessment angelegt werden
 * soll, dessen Mitigation-Strategie nicht Waiver-faehig ist
 * (Iteration 20, CVM-51).
 */
public class WaiverNotApplicableException extends RuntimeException {

    public WaiverNotApplicableException(String message) {
        super(message);
    }
}
