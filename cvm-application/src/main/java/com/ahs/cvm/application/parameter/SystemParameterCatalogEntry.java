package com.ahs.cvm.application.parameter;

import com.ahs.cvm.domain.enums.SystemParameterType;

/**
 * Deklarative Beschreibung eines Katalog-Eintrags fuer den System-Parameter-
 * Store.
 *
 * <p>Die Eintraege werden beim Start der Anwendung pro aktivem Mandant in die
 * Tabelle {@code system_parameter} gespiegelt ({@code hotReload}- und
 * {@code adminOnly}-Flags werden ebenfalls uebernommen). Vorhandene Werte
 * bleiben unberuehrt - der Bootstrap legt nur fehlende Schluessel an.
 *
 * <p>{@code sensitive=true} bleibt in Iteration 41 ungenutzt: Secrets werden
 * in Iteration 45 separat via AES-GCM eingepflegt.
 */
public record SystemParameterCatalogEntry(
        String paramKey,
        String label,
        String description,
        String handbook,
        String category,
        String subcategory,
        SystemParameterType type,
        String defaultValue,
        boolean required,
        String validationRules,
        String options,
        String unit,
        boolean sensitive,
        boolean hotReload,
        boolean adminOnly) {

    public SystemParameterCatalogEntry {
        if (paramKey == null || paramKey.isBlank()) {
            throw new IllegalArgumentException("paramKey darf nicht leer sein");
        }
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("label darf nicht leer sein");
        }
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("category darf nicht leer sein");
        }
        if (type == null) {
            throw new IllegalArgumentException("type darf nicht leer sein");
        }
    }
}
