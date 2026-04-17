-- CVE-Anreicherungsfelder (NVD/GHSA/KEV/EPSS). Die Grundspalten stehen in
-- V0004. Hier werden CWE- und Advisory-Listen sowie Zeitstempel fuer den
-- letzten Fetch ergaenzt.

ALTER TABLE cve ADD COLUMN IF NOT EXISTS cwes JSONB;
ALTER TABLE cve ADD COLUMN IF NOT EXISTS advisories JSONB;
ALTER TABLE cve ADD COLUMN IF NOT EXISTS last_fetched_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_cve_last_fetched ON cve (last_fetched_at);

-- V0004 hat kev_listed_at bereits angelegt; ein Indikator fuer CVEs mit
-- aktuellen EPSS-Werten erleichtert die Heatmap-Abfragen.
CREATE INDEX IF NOT EXISTS idx_cve_epss_score
    ON cve (epss_score DESC NULLS LAST)
    WHERE epss_score IS NOT NULL;
