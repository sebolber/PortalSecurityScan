-- Erweitert den Scan um Umgebungsbezug, Deduplizierungs-Hash und die
-- verschluesselt abgelegte Rohdaten-SBOM (bytea). Der bisherige
-- sbom_checksum-Unique-Constraint wird durch den fachlich sauberen
-- (product_version_id, environment_id, content_sha256)-Unique ersetzt.

ALTER TABLE scan
    ADD COLUMN environment_id UUID REFERENCES environment (id) ON DELETE CASCADE;

ALTER TABLE scan
    ADD COLUMN content_sha256 TEXT;

ALTER TABLE scan
    ADD COLUMN raw_sbom BYTEA;

-- Existierende Zeilen (falls vorhanden) brauchen Pflichtfelder. In Iteration 02
-- gibt es noch keine Produktivdaten, daher harte NOT NULLs ohne Default.
UPDATE scan SET content_sha256 = sbom_checksum WHERE content_sha256 IS NULL;

ALTER TABLE scan ALTER COLUMN content_sha256 SET NOT NULL;

ALTER TABLE scan DROP CONSTRAINT IF EXISTS uq_scan_checksum;

ALTER TABLE scan
    ADD CONSTRAINT uq_scan_content
        UNIQUE (product_version_id, environment_id, content_sha256);

CREATE INDEX idx_scan_environment ON scan (environment_id);
