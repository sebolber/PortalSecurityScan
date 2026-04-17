package com.ahs.cvm.domain.enums;

/**
 * Lebenszyklus einer Kontextprofil-Version.
 *
 * <p>{@code DRAFT} ist der Eingangsstatus. {@code ACTIVE} markiert die aktuell
 * gueltige Version einer Umgebung (hoechstens eine). {@code SUPERSEDED} ist
 * die Archiv-Markierung fuer alte Versionen.
 */
public enum ProfileState {
    DRAFT,
    ACTIVE,
    SUPERSEDED
}
