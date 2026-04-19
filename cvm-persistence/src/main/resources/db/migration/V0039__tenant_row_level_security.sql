-- Iteration 62E (CVM-62): Postgres-Row-Level-Security als zweite
-- Verteidigungslinie gegen Cross-Tenant-Reads. Selbst wenn ein Service
-- versehentlich den Tenant-Filter vergisst, greift die Policy.
--
-- Die Policy liest eine Session-Variable `cvm.current_tenant`, die der
-- `TenantSessionAspect` (cvm-application) beim Beginn einer Transaktion
-- via `SET LOCAL` setzt. Ohne gesetzten Wert erlaubt die Policy keinen
-- Zugriff (fail closed).
--
-- Superuser-Rollen (z.B. Flyway, Datenbank-Backup) umgehen RLS. In
-- Produktion sollte die Anwendung NICHT als Superuser connecten.

-- Helfer: gibt die aktuelle Tenant-UUID zurueck, NULL wenn nicht gesetzt.
CREATE OR REPLACE FUNCTION cvm_current_tenant() RETURNS uuid AS $$
BEGIN
    RETURN NULLIF(current_setting('cvm.current_tenant', true), '')::uuid;
EXCEPTION WHEN others THEN
    RETURN NULL;
END;
$$ LANGUAGE plpgsql STABLE;

-- Liste der mandanten-gescopten Tabellen. Components sind bewusst NICHT
-- dabei (Stammdaten, CVEs sind global).
DO $$
DECLARE
    tbl text;
    tables text[] := ARRAY[
        'product',
        'product_version',
        'environment',
        'scan',
        'component_occurrence',
        'finding',
        'assessment',
        'waiver',
        'alert_rule'
    ];
BEGIN
    FOREACH tbl IN ARRAY tables LOOP
        EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', tbl);
        EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY', tbl);
        EXECUTE format(
            'DROP POLICY IF EXISTS tenant_isolation ON %I', tbl);
        EXECUTE format(
            'CREATE POLICY tenant_isolation ON %I '
            'USING (tenant_id = cvm_current_tenant()) '
            'WITH CHECK (tenant_id = cvm_current_tenant())', tbl);
    END LOOP;
END $$;
