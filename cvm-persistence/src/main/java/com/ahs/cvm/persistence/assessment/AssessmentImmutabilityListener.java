package com.ahs.cvm.persistence.assessment;

import jakarta.persistence.PreUpdate;

/**
 * Verhindert JPA-Updates an bestehenden {@link Assessment}-Zeilen.
 *
 * <p>Ausnahme: das Feld {@code supersededAt} darf gesetzt werden, wenn
 * eine neue Version dieses Assessment ersetzt. Alle anderen Aenderungen
 * werfen {@link ImmutabilityException}.
 */
public class AssessmentImmutabilityListener {

    @PreUpdate
    public void verhindereUpdate(Assessment assessment) {
        if (!assessment.isSupersedingAllowed()) {
            throw new ImmutabilityException(
                    "Assessment %s ist immutable. Neue Version anlegen statt aendern."
                            .formatted(assessment.getId()));
        }
    }
}
