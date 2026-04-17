-- Scans, Komponenten und deren Vorkommen in einem Scan.
-- Component wird global ueber PURL dedupliziert; jedes Scan-Vorkommen verweist
-- dann auf den gemeinsamen Stammsatz (ComponentOccurrence).

CREATE TABLE scan (
    id UUID PRIMARY KEY,
    product_version_id UUID NOT NULL REFERENCES product_version (id) ON DELETE CASCADE,
    sbom_format TEXT NOT NULL,
    sbom_checksum TEXT NOT NULL,
    scanned_at TIMESTAMPTZ NOT NULL,
    scanner TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ,
    CONSTRAINT uq_scan_checksum UNIQUE (product_version_id, sbom_checksum)
);

CREATE INDEX idx_scan_product_version ON scan (product_version_id);

CREATE TABLE component (
    id UUID PRIMARY KEY,
    purl TEXT NOT NULL,
    name TEXT NOT NULL,
    version TEXT NOT NULL,
    type TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ,
    CONSTRAINT uq_component_purl UNIQUE (purl)
);

CREATE INDEX idx_component_name_version ON component (name, version);

CREATE TABLE component_occurrence (
    id UUID PRIMARY KEY,
    scan_id UUID NOT NULL REFERENCES scan (id) ON DELETE CASCADE,
    component_id UUID NOT NULL REFERENCES component (id) ON DELETE RESTRICT,
    direct BOOLEAN NOT NULL DEFAULT TRUE,
    bom_ref TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ,
    CONSTRAINT uq_occurrence_in_scan UNIQUE (scan_id, component_id)
);

CREATE INDEX idx_occurrence_scan ON component_occurrence (scan_id);
CREATE INDEX idx_occurrence_component ON component_occurrence (component_id);
