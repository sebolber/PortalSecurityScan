-- Iteration 62C (CVM-62): Scan, Finding und Component-Occurrence
-- bekommen `tenant_id`. Die Zuordnung leitet sich vom Scan-Produkt ab
-- (Scan -> product_version -> product.tenant_id) und propagiert weiter
-- an Findings und ComponentOccurrences. Components sind keine Sach-
-- daten (Stammdaten ueber alle Mandanten), daher ohne tenant_id.

-- scan: Mandant aus dem zugeordneten product_version ableiten
ALTER TABLE scan ADD COLUMN tenant_id UUID;
UPDATE scan s
   SET tenant_id = pv.tenant_id
  FROM product_version pv
 WHERE s.product_version_id = pv.id;
UPDATE scan
   SET tenant_id = (SELECT id FROM tenant WHERE is_default = TRUE LIMIT 1)
 WHERE tenant_id IS NULL;
ALTER TABLE scan ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE scan
    ADD CONSTRAINT fk_scan_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id);
CREATE INDEX idx_scan_tenant ON scan (tenant_id);

-- finding: Mandant aus dem zugeordneten scan ableiten
ALTER TABLE finding ADD COLUMN tenant_id UUID;
UPDATE finding f
   SET tenant_id = s.tenant_id
  FROM scan s
 WHERE f.scan_id = s.id;
UPDATE finding
   SET tenant_id = (SELECT id FROM tenant WHERE is_default = TRUE LIMIT 1)
 WHERE tenant_id IS NULL;
ALTER TABLE finding ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE finding
    ADD CONSTRAINT fk_finding_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id);
CREATE INDEX idx_finding_tenant ON finding (tenant_id);

-- component_occurrence: Mandant aus scan ableiten
ALTER TABLE component_occurrence ADD COLUMN tenant_id UUID;
UPDATE component_occurrence co
   SET tenant_id = s.tenant_id
  FROM scan s
 WHERE co.scan_id = s.id;
UPDATE component_occurrence
   SET tenant_id = (SELECT id FROM tenant WHERE is_default = TRUE LIMIT 1)
 WHERE tenant_id IS NULL;
ALTER TABLE component_occurrence ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE component_occurrence
    ADD CONSTRAINT fk_component_occurrence_tenant
    FOREIGN KEY (tenant_id) REFERENCES tenant (id);
CREATE INDEX idx_component_occurrence_tenant ON component_occurrence (tenant_id);
