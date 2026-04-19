-- Iteration 62D (CVM-62): Bewertungs-Daten (assessment, waiver,
-- alert_rule) bekommen `tenant_id`. Ableitung:
--   assessment  -> aus finding.tenant_id
--   waiver      -> aus cve + environment: nutzt environment.tenant_id
--   alert_rule  -> global, aktuell Default-Tenant (Admin-defined)

ALTER TABLE assessment ADD COLUMN tenant_id UUID;
UPDATE assessment a
   SET tenant_id = f.tenant_id
  FROM finding f
 WHERE a.finding_id = f.id;
UPDATE assessment
   SET tenant_id = (SELECT id FROM tenant WHERE is_default = TRUE LIMIT 1)
 WHERE tenant_id IS NULL;
ALTER TABLE assessment ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE assessment
    ADD CONSTRAINT fk_assessment_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id);
CREATE INDEX idx_assessment_tenant ON assessment (tenant_id);

-- Waiver: environment_id ist pflicht -> tenant ableiten
ALTER TABLE waiver ADD COLUMN tenant_id UUID;
UPDATE waiver w
   SET tenant_id = e.tenant_id
  FROM environment e
 WHERE w.environment_id = e.id;
UPDATE waiver
   SET tenant_id = (SELECT id FROM tenant WHERE is_default = TRUE LIMIT 1)
 WHERE tenant_id IS NULL;
ALTER TABLE waiver ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE waiver
    ADD CONSTRAINT fk_waiver_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id);
CREATE INDEX idx_waiver_tenant ON waiver (tenant_id);

-- AlertRule: aktuell global konfiguriert, aber kuenftig pro Tenant
-- verwaltet -> Default-Tenant beim Backfill, danach NOT NULL.
ALTER TABLE alert_rule ADD COLUMN tenant_id UUID;
UPDATE alert_rule
   SET tenant_id = (SELECT id FROM tenant WHERE is_default = TRUE LIMIT 1)
 WHERE tenant_id IS NULL;
ALTER TABLE alert_rule ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE alert_rule
    ADD CONSTRAINT fk_alert_rule_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id);
CREATE INDEX idx_alert_rule_tenant ON alert_rule (tenant_id);
