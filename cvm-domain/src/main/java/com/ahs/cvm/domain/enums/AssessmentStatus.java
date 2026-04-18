package com.ahs.cvm.domain.enums;

/**
 * Lebenszyklus eines Assessments. {@code PROPOSED} ist der Status aller
 * Vorschlaege (Regel oder KI). Nur ein Mensch kann {@code APPROVED} setzen.
 *
 * <p>{@code EXPIRED} entsteht via Scheduler, sobald {@code validUntil}
 * ueberschritten ist. {@code SUPERSEDED} entsteht durch das Anlegen einer
 * neuen Version (Autor/Approver).
 *
 * <p>{@code NEEDS_VERIFICATION} (Iteration 13) entsteht aus der KI-
 * Vorbewertung, wenn der Halluzinations-Check eine vom LLM
 * gelieferte Faktenangabe (z.&nbsp;B. {@code proposedFixVersion})
 * nicht im Advisory belegen kann.
 */
public enum AssessmentStatus {
    PROPOSED,
    APPROVED,
    REJECTED,
    SUPERSEDED,
    NEEDS_REVIEW,
    NEEDS_VERIFICATION,
    EXPIRED
}
