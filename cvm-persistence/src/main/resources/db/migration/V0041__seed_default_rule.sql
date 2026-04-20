-- Seed: Standard-Regel fuer Erstinstall.
--
-- Legt eine ACTIVE-Fallback-Regel an, die jedes Finding mit einer
-- CVE-ID trifft und eine PROPOSED-Bewertung (Severity MEDIUM) in der
-- Queue erzeugt, damit nichts unklassifiziert durch die Cascade laeuft.
-- Die Regel ist durch den aeltesten createdAt-Zeitstempel (UNIX-Epoch)
-- in der createdAt-DESC-Reihenfolge der RuleEngine garantiert an
-- letzter Position; spezifischere Regeln greifen davor.
--
-- Idempotent ueber ON CONFLICT (rule_key).  Aenderungen am Seed bitte
-- in einer neuen Migration vornehmen, nicht diese Datei editieren
-- (Flyway prueft den Hash).

INSERT INTO rule (
        id,
        rule_key,
        name,
        description,
        origin,
        status,
        proposed_severity,
        condition_json,
        rationale_template,
        rationale_source_fields,
        created_by,
        activated_by,
        activated_at,
        created_at)
VALUES (
        '00000000-0000-0000-0000-0000000000d1',
        'default-alle-findings-in-queue',
        'Standard: alle SBOM-Findings in die Bewertungs-Queue',
        'Auto-Seed bei Erstinstall. Matched jedes Finding mit einer CVE-ID und legt eine PROPOSED-Bewertung mit Severity MEDIUM an, damit nichts unklassifiziert bleibt. Der Reviewer setzt Severity und Verdict im Queue-Dialog final. Greift als Fallback (aeltester createdAt ⇒ letzte Position in der DESC-Reihenfolge der RuleEngine); spezifischere Regeln werden davor ausgewertet.',
        'MANUAL',
        'ACTIVE',
        'MEDIUM',
        '{"matches":{"path":"cve.id","value":".+"}}',
        'Standardregel: Finding {cve.id} in {component.name}@{component.version} landet zur manuellen Bewertung in der Queue.',
        '["cve.id"]'::jsonb,
        'system@cvm',
        'system@cvm',
        'epoch'::timestamptz,
        'epoch'::timestamptz)
ON CONFLICT (rule_key) DO NOTHING;
