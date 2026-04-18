package com.ahs.cvm.persistence.ai;

import com.ahs.cvm.persistence.assessment.ImmutabilityException;
import jakarta.persistence.PreUpdate;

/**
 * Verhindert inhaltliche Aenderungen an {@link AiCallAudit}. Einzig
 * erlaubter Update-Pfad ist die Finalisierung (PENDING -&gt; anderer
 * Status). Alle anderen Spalten sind auf {@code updatable=false}
 * gemappt, der Listener ist die letzte Verteidigungslinie.
 */
public class AiCallAuditImmutabilityListener {

    @PreUpdate
    public void verhindereUpdate(AiCallAudit audit) {
        if (!audit.isFinalizingAllowed()) {
            throw new ImmutabilityException(
                    "AiCallAudit %s ist immutable. Nur die Finalisierung vom PENDING-Status ist erlaubt."
                            .formatted(audit.getId()));
        }
    }
}
