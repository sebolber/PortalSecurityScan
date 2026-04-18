-- Schema-Fix.
--
-- V0012 hat die sha256-Spalte als CHAR(64) (Postgres "bpchar") angelegt.
-- Die JPA-Entity GeneratedReport erwartet aber VARCHAR(64), wodurch
-- Hibernates Schema-Validation beim Backend-Start fehlschlaegt
-- ("wrong column type encountered in column [sha256] in table
-- [generated_report]; found [bpchar (Types#CHAR)], but expecting
-- [varchar(64) (Types#VARCHAR)]").
--
-- Forward-only Fix: Spalten-Typ auf VARCHAR(64) konvertieren. Daten
-- bleiben erhalten; Trailing-Spaces gibt es bei SHA-256-Hashes ohnehin
-- nicht.

ALTER TABLE generated_report
    ALTER COLUMN sha256 TYPE VARCHAR(64);
