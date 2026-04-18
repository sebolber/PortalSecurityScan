package com.ahs.cvm.domain.enums;

/**
 * Status eines KI-Vorschlags. Entspricht Konzept v0.2 Abschnitt 4.4:
 * KI-Vorschlaege bleiben immer {@link #PROPOSED}, bis ein Mensch
 * entscheidet. {@link #REJECTED} wird gesetzt, wenn der Bewerter den
 * Vorschlag verwirft.
 */
public enum AiSuggestionStatus {
    PROPOSED,
    ACCEPTED,
    REJECTED
}
