package com.ahs.cvm.persistence.ai;

import com.pgvector.PGvector;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA-Konverter fuer {@link PGvector}.
 *
 * <p>Hibernate 6 hat keinen Default-JdbcType fuer pgvector und
 * serialisiert das {@link PGvector}-Objekt sonst als Java-Serializable
 * in {@code bytea} - beim INSERT wirft PostgreSQL dann
 * {@code ERROR: column "embedding" is of type vector but expression is
 * of type bytea}.
 *
 * <p>Der Konverter schreibt das Textformat
 * ({@code "[1.0,2.0,...]"}) in die Spalte. Hibernate bindet den Wert
 * dabei als {@code VARCHAR}; pgvector hat aber keinen impliziten Cast
 * {@code varchar -> vector}. Die Entity haengt deshalb ein
 * {@code @ColumnTransformer(write = "?::vector")} an das Feld, sodass
 * der Parameter im INSERT/UPDATE explizit gecastet wird. Beim Lesen
 * liefert pgvector den Wert als Text - der Converter parst ihn zurueck.
 */
@Converter(autoApply = false)
public class PGvectorConverter implements AttributeConverter<PGvector, String> {

    @Override
    public String convertToDatabaseColumn(PGvector attribute) {
        return attribute == null ? null : attribute.toString();
    }

    @Override
    public PGvector convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            return new PGvector(dbData);
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException(
                    "Spalte enthaelt keinen gueltigen pgvector-String: " + dbData, e);
        }
    }
}
