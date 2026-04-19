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
 * <p>Der Konverter schreibt stattdessen das Textformat
 * ({@code "[1.0,2.0,...]"}) in die Spalte. Zusaetzlich bindet die
 * Entity das Feld ueber {@code @JdbcTypeCode(SqlTypes.OTHER)}, sodass
 * der PostgreSQL-JDBC-Treiber den Wert als "unknown" sendet; pgvector
 * laesst den Text dann automatisch auf {@code vector} parsen. Ohne das
 * {@code OTHER} bindet Hibernate standardmaessig als {@code VARCHAR},
 * und pgvector verweigert den impliziten Cast auf gebundene Parameter.
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
