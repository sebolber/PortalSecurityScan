# Iteration 21 - Mandanten + CI/CD-Gate + KPIs - Fortschritt

**Jira**: CVM-52
**Branch**: `claude/iteration-10-pdf-report-C9sA4`
**Abgeschlossen**: 2026-04-18 (Abschluss-Iteration).

## Umgesetzt

### Teil A - Mandantenfaehigkeit + Modell-Profil

- Flyway `V0021__tenant_und_modelprofile.sql`:
  - `tenant` (UUID, key, name, active, default),
  - `llm_model_profile` (Provider `CLAUDE_CLOUD`/`OLLAMA_ONPREM`,
    `model_id`, `cost_budget_eur_monthly`, `approved_for_gkv_data`),
  - `environment.llm_model_profile_id` FK,
  - `model_profile_change_log` (auditierbarer Wechsel-Log).
- Persistenz: `Tenant`, `TenantRepository`, `LlmModelProfile`,
  `LlmModelProfileRepository`, `ModelProfileChangeLog`,
  `ModelProfileChangeLogRepository`.
- `AiCallAuditRepository.sumCostEurForModelAndRange(modelId, from, to)`.
- `TenantContext` (ThreadLocal) fuer zukuenftige RLS-Durchsetzung.
- `ModelProfileService.switchProfile` mit Vier-Augen
  (`changedBy != fourEyesConfirmer`) + Log-Eintrag + JPA-Update.
- `LlmCostGuard.isUnderBudget(profileId)` summiert
  `ai_call_audit.costEur` im laufenden Monat gegen das Budget.
- REST: `POST /api/v1/environments/{id}/model-profile/switch`,
  `GET .../history`.

### Teil B - CI/CD-Gate

- `PipelineGateService.evaluate(GateRequest)`:
  - parst SBOM via `CycloneDxParser`,
  - sammelt Severities direkt aus den CycloneDX-Ratings,
  - fuellt Luecken aus dem CVE-Katalog (`Cve.cvssBaseScore` -&gt;
    Severity-Mapping),
  - vergleicht mit bestehenden Findings fuer die
    `productVersionId`,
  - liefert `GateResult(gate, newCritical, newHigh, evaluatedAt,
    details[])`.
- Entscheidung: `newCritical > 0 -> FAIL`,
  `newHigh > 0 -> WARN`, sonst `PASS`.
- REST: `POST /api/v1/pipeline/gate` (multipart `sbom`
  + Form-Felder), Header `X-Gate-Decision`.
- **Keine Vollingestion**: der Gate parst nur. Voll-Ingestion
  bleibt dem regulaeren Scan-Upload vorbehalten, um Dedup- und
  Historien-Logik nicht zu stoeren.

### Teil C - KPIs + Fix-SLA

- `KpiService.compute(productVersionId, environmentId, window)`:
  - offene CVEs je Severity (EnumMap),
  - Burn-Down-Serie taeglich ueber das Fenster
    (offen am Stichtag = created &lt; stichtag UND
    (decided == null ODER decided &gt;= stichtag)),
  - MTTR je Severity (Durchschnitt `decidedAt - createdAt` in Tagen),
  - Fix-SLA-Quote je Severity mit Defaults
    `CRITICAL<=7, HIGH<=30, MEDIUM<=90, LOW<=180`,
  - Automatisierungsquote
    (`APPROVED mit AI_SUGGESTION / alle AI_SUGGESTION`).
- REST: `GET /api/v1/kpis` (JSON),
  `GET /api/v1/kpis/export` (CSV fuer Executive-Anhang).
- Window-Parser: `90d`, `48h`, reine Zahl = Tage.

## Sicherheits-Invarianten

1. **Vier-Augen bei Modell-Profil-Wechsel**
   (`changedBy != fourEyesConfirmer`) - Tests haerten.
2. **Cost-Cap** deaktiviert KI-Funktionen automatisch, sobald das
   Monatsbudget erreicht ist. Das Guard-Objekt ist read-only und
   gibt nur den Boolean zurueck; der Schalter greift in der
   KI-Service-Schicht (Flag-Abfrage in `isUnderBudget`).
3. **Gate blockt nicht automatisch**: die Pipeline entscheidet.
4. **KPIs sind read-only**, keine DB-Mutationen.

## Tests

- `LlmCostGuardTest` (6/6).
- `ModelProfileServiceTest` (5/5).
- `KpiServiceTest` (6/6).
- `PipelineGateServiceTest` (6/6).
- `ModelProfileControllerWebTest` (5/5).
- `KpiControllerWebTest` (3/3).
- `PipelineGateControllerWebTest` (3/3).
- Voller Build `./mvnw -T 1C test` -&gt; BUILD SUCCESS.

## NICHT umgesetzt (bewusst)

- **Vollstaendige RLS-Durchsetzung** ueber alle Sachtabellen. Die
  Tenant-Infrastruktur steht; Policies werden in der Rollout-Phase
  in separater Migration nachgezogen (siehe offene-punkte.md).
- **Schema-Migration bestehender Daten** mit Default-Tenant-Spalten
  auf jeder Tabelle - nur die Metatabellen (tenant, profile, log)
  sind neu. Vorhandene `environment.tenant`-String-Spalte bleibt bis
  zur RLS-Aktivierung weiterhin gueltig.
- **GitLab-CI-Template-Datei** im Repo - Beispiel ist im Plan
  dokumentiert; Wiring folgt beim Rollout.
- **GitLab-MR-Kommentar** aus dem Gate zurueck an GitLab - braucht
  einen dedizierten Pipeline-Token; offen.
- **Materialized Views** / `kpi_snapshot_daily` - aktuell
  Stream-Aggregation. Optimierung auf Verdacht ausgelassen.
- **KPI-Dashboard-ECharts-Widgets** im Frontend - Backend-Vertrag
  steht, UI-Nachzug separat.
