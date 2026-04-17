package com.ahs.cvm.domain.enums;

/**
 * Lebenszyklus eines Assessments. {@code PROPOSED} ist der Status aller
 * Vorschlaege (Regel oder KI). Nur ein Mensch kann {@code APPROVED} setzen.
 *
 * <p>{@code EXPIRED} entsteht via Scheduler, sobald {@code validUntil}
 * ueberschritten ist. {@code SUPERSEDED} entsteht durch das Anlegen einer
 * neuen Version (Autor/Approver).
 */
public enum AssessmentStatus {
    PROPOSED,
    APPROVED,
    REJECTED,
    SUPERSEDED,
    NEEDS_REVIEW,
    EXPIRED
}
