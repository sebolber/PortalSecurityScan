package com.ahs.cvm.domain.enums;

/**
 * Lebenszyklus einer Regel. {@code DRAFT} ist der Eingangsstatus, aktiviert
 * wird nur per Vier-Augen-Freigabe. {@code RETIRED} markiert eine nicht mehr
 * genutzte Regel.
 */
public enum RuleStatus {
    DRAFT,
    ACTIVE,
    RETIRED
}
