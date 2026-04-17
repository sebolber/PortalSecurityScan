package com.ahs.cvm.domain.enums;

/**
 * Lebenszyklus eines Assessments. {@code PROPOSED} ist der Status aller
 * Vorschlaege (Regel oder KI). Nur ein Mensch kann {@code APPROVED} setzen.
 */
public enum AssessmentStatus {
    PROPOSED,
    APPROVED,
    REJECTED,
    SUPERSEDED,
    NEEDS_REVIEW
}
