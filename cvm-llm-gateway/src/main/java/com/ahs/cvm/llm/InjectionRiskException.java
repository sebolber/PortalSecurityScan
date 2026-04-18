package com.ahs.cvm.llm;

import java.util.List;

/**
 * Wird geworfen, wenn der Injection-Detektor im Block-Modus
 * ({@code cvm.llm.injection.mode=block}) anschlaegt. Im Warn-Modus
 * wird der Call ausgefuehrt, der Audit-Eintrag aber mit
 * {@code injectionRisk=true} markiert.
 */
public class InjectionRiskException extends RuntimeException {

    private final List<String> marker;

    public InjectionRiskException(List<String> marker) {
        super("Injection-Risiko erkannt: " + String.join(", ", marker));
        this.marker = List.copyOf(marker);
    }

    public List<String> marker() {
        return marker;
    }
}
