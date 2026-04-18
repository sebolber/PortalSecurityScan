-- Iteration 10 -- PDF-Abschlussbericht (CVM-19).
--
-- Archiv-Tabelle fuer generierte, immutable PDF-Reports. Die
-- PDF-Rohbytes liegen als BYTEA; die Verschluesselung-at-rest via
-- Jasypt wird spaeter nachgezogen (siehe offene-punkte.md Iteration 10).

CREATE TABLE IF NOT EXISTS generated_report (
    id                  UUID PRIMARY KEY,
    product_version_id  UUID NOT NULL REFERENCES product_version(id),
    environment_id      UUID NOT NULL REFERENCES environment(id),
    report_type         TEXT NOT NULL,
    title               TEXT NOT NULL,
    gesamteinstufung    TEXT NOT NULL,
    freigeber_kommentar TEXT,
    erzeugt_von         TEXT NOT NULL,
    erzeugt_am          TIMESTAMPTZ NOT NULL,
    stichtag            TIMESTAMPTZ NOT NULL,
    sha256              CHAR(64) NOT NULL,
    pdf_bytes           BYTEA NOT NULL,
    CONSTRAINT ck_generated_report_severity
        CHECK (gesamteinstufung IN (
            'CRITICAL','HIGH','MEDIUM','LOW',
            'INFORMATIONAL','NOT_APPLICABLE')),
    CONSTRAINT ck_generated_report_type
        CHECK (report_type IN ('HARDENING'))
);

CREATE INDEX IF NOT EXISTS idx_generated_report_prod_env_erzeugt
    ON generated_report (product_version_id, environment_id, erzeugt_am DESC);

CREATE UNIQUE INDEX IF NOT EXISTS uq_generated_report_sha256
    ON generated_report (sha256);
