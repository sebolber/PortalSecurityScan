# Iteration 21 – Mandantenfähigkeit + CI/CD-Gate + Trends/KPIs

**Jira**: CVM-52
**Abhängigkeit**: alle vorherigen (M3)
**Ziel**: Produktivreifes System für Rollout auf mehrere Mandanten (BKKen),
Einbindung in CI-Pipelines und Kennzahlenbereich für Lenkungsausschuss.

---

## Kontext
Konzept v0.2 Abschnitt 11.10 (CI/CD-Gate), 11.11 (Historische Trends), 11.13
(Mandantenfähigkeit), 11.16 (Fix-SLA-Tracking), Abschnitt 12.2
(Modell-Profil pro Umgebung), Abschnitt 14 Phase 4. Abschluss-Iteration
vor Produktiv-Rollout.

## Scope IN

### Teil A – Mandantenfähigkeit
1. `Tenant`-Entität + Flyway `V0014__mandantenfaehigkeit.sql`.
2. Jede fachliche Entität bekommt `tenant_id NOT NULL` (soweit noch nicht da).
3. **PostgreSQL Row-Level Security**:
   - Policies je Tabelle: `USING (tenant_id = current_setting('cvm.current_tenant')::uuid)`.
   - Anwendungsseitig wird `SET LOCAL cvm.current_tenant = '…'` pro
     Transaktion gesetzt (Spring `@Transactional`-Aspekt).
4. `TenantResolver` aus JWT-Claim (`tenant_id`), Fallback auf
   Default-Mandant bei Admin-Rollen.
5. Modell-Profil pro Mandant/Umgebung:
   - `environment.llmModelProfile` (FK auf `llm_model_profile`).
   - `llm_model_profile`: `provider` (CLAUDE_CLOUD | OLLAMA_ONPREM),
     `modelId`, `modelVersion`, `costBudgetEurMonthly`,
     `approvedForGkvData` (boolean).
6. Wechsel des Modell-Profils: Vier-Augen-Prinzip, Audit.
7. Kosten-Cap: wenn `costBudgetEurMonthly` überschritten, fallen
   KI-Funktionen in den Regel-only-Modus (Feature-Flag automatisch
   deaktiviert pro Mandant), Alert geht raus.

### Teil B – CI/CD-Gate (Merge-Request-Gate)
1. Neuer Endpunkt `POST /api/v1/pipeline/gate`:
   - Eingabe: CycloneDX-SBOM eines Pipeline-Laufs, `productVersionId`,
     `environmentId=CI`, `branchRef`, `mergeRequestId`.
   - Verarbeitung: Scan-Ingestion + Cascade (ohne KI-Cascade-Stufe aus
     Performance-Gründen konfigurierbar).
   - Ausgabe: `{gate: PASS|WARN|FAIL, newCritical: int, newHigh: int,
     reportUrl, details[]}`.
2. GitLab-CI-Template (`.gitlab-ci-cvm-gate.yml`) zum Einbinden in
   Produkt-Pipelines:
   ```yaml
   cvm-gate:
     stage: security
     script:
       - trivy fs --format cyclonedx -o sbom.json .
       - curl -X POST $CVM_URL/api/v1/pipeline/gate -F sbom=@sbom.json ...
     allow_failure: false
   ```
3. Konfiguration: Schwellen pro Produkt (z. B. „FAIL bei neuem CRITICAL",
   „WARN bei neuem HIGH ohne bestehenden Waiver").
4. Benachrichtigung zurück ins Merge-Request via GitLab-API
   (kurze Zusammenfassung als Kommentar, Link zum Bericht).

### Teil C – Trends / KPIs / Fix-SLA
1. `KpiService`:
   - offene CVEs nach Severity (aktuell + Trend 90 Tage)
   - Burn-Down offener HIGH/CRITICAL
   - MTTR je Severity
   - Fix-SLA-Quote: SLAs konfigurierbar (CRITICAL ≤ 7 Tage, HIGH ≤ 30, …),
     Verletzungen aufgelistet
   - Automatisierungsquote: Anteil der KI-Vorschläge, die ohne Änderung
     approved wurden
2. REST: `GET /api/v1/kpis?productVersionId=…&environmentId=…&window=90d`.
3. UI: Dashboard aus Iteration 07 erweitert um Trend-Charts (ECharts,
   Linie für Burn-Down, gestapelte Säulen für Severity-Verteilung,
   Ampel für SLA-Quote).
4. Export: KPIs als CSV und als Anhang im Executive-Report.

## Scope NICHT IN
- Mandanten-übergreifende Aggregationen (bewusst nicht – Datenhoheit).
- Integration in kundeneigene SIEMs (später, separates Projekt).

## Aufgaben
1. RLS-Policies sauber durchziehen; ArchUnit-Test, dass keine Repository-
   Query ohne Tenant-Kontext ausgeführt werden kann (Aspect oder
   `@TenantBound`-Marker).
2. Migrations-Strategie für bestehende Daten (Einführung von `tenant_id`):
   Default-Tenant auf alle bestehenden Datensätze.
3. Gate-Endpunkt mit striktem Rate-Limit und Auth via Pipeline-Token
   (OIDC Service-Account pro Produkt).
4. KPI-Berechnungen optimieren (Materialized Views oder Nachtjob für
   Aggregate-Tabelle `kpi_snapshot_daily`).

## Test-Schwerpunkte
- `RlsIsolationTest`: Tenant A sieht Daten von Tenant B nicht –
  selbst bei bewusst fehlerhafter Query.
- `ModelProfileTest`: Wechsel erzwingt Vier-Augen, Audit vorhanden,
  Kosten-Cap schaltet KI automatisch ab.
- `PipelineGateTest`: PASS/WARN/FAIL je nach Schwelle.
- `KpiServiceTest`: Burn-Down, MTTR, SLA-Quote deterministisch aus
  Testdaten berechnet.
- ArchUnit: kein Repository-Call ohne Tenant-Kontext.
- `@DisplayName`: `@DisplayName("RLS: Query ohne Tenant-Kontext liefert leere Ergebnismenge")`

## Definition of Done
- [ ] Mandantenfähigkeit durchgezogen, RLS aktiv.
- [ ] Modell-Profil pro Umgebung, Kosten-Cap greift.
- [ ] CI/CD-Gate lauffähig gegen Test-Pipeline.
- [ ] KPIs + Trend-Charts sichtbar.
- [ ] Fix-SLA-Quote im Dashboard.
- [ ] Coverage kritischer Pfade ≥ 90 %.
- [ ] Fortschrittsbericht.
- [ ] Commit: `feat(governance): Mandantenfaehigkeit, CI/CD-Gate, KPIs und Fix-SLA\n\nCVM-52`

## TDD-Hinweis
Der RLS-Isolationstest ist **nicht verhandelbar**. Wenn er rot wird, liegt
ein Datenleck-Risiko vor – sofort stoppen, Sebastian informieren.
**Ändere NICHT die Tests** bei Rot. Keine Ausnahme.

## Abschlussbericht
Standard, plus:
- KPI-Dashboard-Screenshot unter `docs/YYYYMMDD/iteration-21-dashboard.png`.
- Liste der offenen Punkte für den Rollout (empfehlenswert an Sebastian
  als „Go-Live-Checkliste").
