package com.ahs.cvm.domain.enums;

/**
 * Herkunft einer Regel. {@code MANUAL} stammt vom Menschen. {@code AI_EXTRACTED}
 * wird in Iteration 17 durch die KI-Regel-Extraktion gesetzt; Auto-Aktivierung
 * erfolgt dort nicht, die Vier-Augen-Freigabe bleibt Pflicht.
 */
public enum RuleOrigin {
    MANUAL,
    AI_EXTRACTED
}
