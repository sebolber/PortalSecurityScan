package com.ahs.cvm.persistence.report;

import com.ahs.cvm.persistence.assessment.ImmutabilityException;
import jakarta.persistence.PreUpdate;

/**
 * Verhindert Updates an archivierten PDF-Reports. Ein neuer Stand wird
 * ueber einen zweiten Report-Datensatz abgebildet; der alte bleibt
 * bestehen (Audit-Anforderung).
 */
public class GeneratedReportImmutabilityListener {

    @PreUpdate
    public void verhindereUpdate(GeneratedReport report) {
        throw new ImmutabilityException(
                "GeneratedReport %s ist immutable. Neuen Report erzeugen statt aendern."
                        .formatted(report.getId()));
    }
}
