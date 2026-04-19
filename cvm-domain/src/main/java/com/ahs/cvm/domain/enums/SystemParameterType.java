package com.ahs.cvm.domain.enums;

/**
 * Typisierung fuer Systemparameter (Vorlage: PortalCore
 * {@code de.portalcore.enums.ParameterType}).
 *
 * <p>Der Service nutzt den Typ, um Wertformat und passende
 * Validierungsregeln zu waehlen. Das Frontend rendert das
 * Eingabefeld passend (z.B. Checkbox fuer {@code BOOLEAN},
 * verstecktes Passwortfeld fuer {@code PASSWORD}).
 */
public enum SystemParameterType {
    STRING,
    INTEGER,
    DECIMAL,
    BOOLEAN,
    EMAIL,
    URL,
    JSON,
    PASSWORD,
    SELECT,
    MULTISELECT,
    DATE,
    TIMESTAMP,
    TEXTAREA,
    HOST,
    IP
}
