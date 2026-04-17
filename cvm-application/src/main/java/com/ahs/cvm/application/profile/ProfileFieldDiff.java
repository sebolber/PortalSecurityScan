package com.ahs.cvm.application.profile;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Ein einzelner Unterschied zwischen zwei Profil-Versionen.
 *
 * <p>{@link ChangeType#CREATED} bedeutet: {@code altWert} ist {@code null}.
 * {@link ChangeType#REMOVED} bedeutet: {@code neuWert} ist {@code null}.
 * {@link ChangeType#CHANGED} bedeutet: beide Werte sind gesetzt und ungleich.
 */
public record ProfileFieldDiff(
        String path, JsonNode altWert, JsonNode neuWert, ChangeType changeType) {

    public enum ChangeType {
        CREATED,
        REMOVED,
        CHANGED
    }
}
