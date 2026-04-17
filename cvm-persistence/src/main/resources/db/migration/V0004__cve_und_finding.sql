-- CVE-Stammdaten und Findings (Verbindung CVE <-> Komponenten-Vorkommen).
-- CVE ist global (unabhaengig von Produkt), Finding ist scan-spezifisch.

CREATE TABLE cve (
    id UUID PRIMARY KEY,
    cve_id TEXT NOT NULL,
    summary TEXT,
    cvss_base_score NUMERIC(3, 1),
    cvss_vector TEXT,
    published_at TIMESTAMPTZ,
    last_modified_at TIMESTAMPTZ,
    kev_listed BOOLEAN NOT NULL DEFAULT FALSE,
    kev_listed_at TIMESTAMPTZ,
    epss_score NUMERIC(5, 4),
    epss_percentile NUMERIC(5, 4),
    source TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ,
    CONSTRAINT uq_cve_id UNIQUE (cve_id)
);

CREATE INDEX idx_cve_published_at ON cve (published_at);
CREATE INDEX idx_cve_kev ON cve (kev_listed) WHERE kev_listed = TRUE;

CREATE TABLE finding (
    id UUID PRIMARY KEY,
    scan_id UUID NOT NULL REFERENCES scan (id) ON DELETE CASCADE,
    component_occurrence_id UUID NOT NULL REFERENCES component_occurrence (id) ON DELETE CASCADE,
    cve_id UUID NOT NULL REFERENCES cve (id) ON DELETE RESTRICT,
    detected_at TIMESTAMPTZ NOT NULL,
    fixed_in_version TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ,
    CONSTRAINT uq_finding_triple UNIQUE (scan_id, component_occurrence_id, cve_id)
);

CREATE INDEX idx_finding_scan ON finding (scan_id);
CREATE INDEX idx_finding_cve ON finding (cve_id);
CREATE INDEX idx_finding_occurrence ON finding (component_occurrence_id);
