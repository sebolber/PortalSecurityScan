# Iteration 21 - Mandanten + CI/CD-Gate + KPIs - Plan

**Jira**: CVM-52
**Branch**: `claude/iteration-10-pdf-report-C9sA4`
**Abhaengigkeit**: alle vorherigen Iterationen.
**Besonderheit**: Abschluss-Iteration vor Rollout.

## Scoping-Entscheidungen (pragmatisch)

Das Konzept-Dokument beschreibt drei umfangreiche Themen. Fuer diese
Iteration wird pragmatisch geschnitten, um abschlussfaehig zu bleiben.
Was hier nicht umgesetzt wird, landet in `offene-punkte.md` als
Rollout-Aufgabe.

### Teil A - Mandantenfaehigkeit (Kern, nicht voll)
- `Tenant` + `LlmModelProfile` als neue Entities (Flyway V0021).
- `Environment.llmModelProfileId` als FK (nullable).
- `TenantContext` (ThreadLocal) + `JwtTenantResolver` in `cvm-api`.
- `ModelProfileService.switchProfile(envId, newProfileId,
  changedBy, currentDecidedBy)` mit **Vier-Augen** und Audit-Entry
  (`AiCallAuditPort`, Use-Case `MODEL_PROFILE_SWITCH`).
- `LlmCostGuard.isUnderBudget(modelProfileId, yearMonth)` - summiert
  `ai_call_audit.costEur` auf Monat und vergleicht gegen
  `costBudgetEurMonthly`. Bei Ueberschreitung
  `cvm.ai.disabled-by-cost=true` im Log + Alert.
- RLS: bewusst **NICHT** als vollstaendige Durchsetzung. Migration legt
  Tabellen nur an; die Policies werden in der Rollout-Phase per
  dedizierter Migration + Integrationstest gegen Testcontainers
  nachgezogen (Risiko zu hoch, dutzende Entitaeten anzupassen).

### Teil B - CI/CD-Gate
- `PipelineGateService` wiederverwendet `ScanIngestService` (dort wird
  SBOM geparst) und vergleicht die Findings des neuen Laufs gegen das
  letzte Snapshot fuer die gleiche `productVersionId`. Neue Findings
  nach Severity zaehlen:
  - `FAIL` wenn `newCritical > 0`,
  - `WARN` wenn `newHigh > 0` ohne aktiven Waiver,
  - sonst `PASS`.
- `PipelineGateController`: `POST /api/v1/pipeline/gate` (multipart
  `sbom` + Form-Felder `productVersionId`, `environmentId`,
  `branchRef`, `mergeRequestId`). Rueckgabe:
  `{gate, newCritical, newHigh, scanId, details[]}`.
- GitLab-Template wird nicht physisch erzeugt - Doku im Fortschritts-
  bericht. Das Wiring gegen GitLab-API bleibt offen (braucht echten
  Token) und wird in `offene-punkte.md` gelistet.

### Teil C - KPIs + Fix-SLA
- `KpiService.compute(productVersionId, environmentId, window)`:
  - offene CVEs je Severity (aktuell),
  - Burn-Down (Anzahl offen je Tag ueber `window`),
  - MTTR je Severity (Durchschnitt `decidedAt - createdAt`),
  - Fix-SLA-Quote (Konfigurierbar: `CRITICAL<=7`, `HIGH<=30`,
    `MEDIUM<=90`, `LOW<=180` Tage),
  - Automatisierungsquote (Anteil `APPROVED` mit
    `ProposalSource == AI_SUGGESTION` und ohne nachtraegliche Edits).
- REST: `GET /api/v1/kpis?productVersionId=&environmentId=&window=90d`.
- CSV-Export: `GET /api/v1/kpis.csv?...` (deterministisch, fuer den
  Executive-Report-Anhang).
- UI-Charts: nicht in dieser Iteration (Backend-Vertrag steht).

## Test-Schwerpunkte

1. `ModelProfileServiceTest`: Vier-Augen-Verstoss -&gt; Exception,
   Cost-Cap greift, Audit-Eintrag geschrieben.
2. `LlmCostGuardTest`: Summiert `costEur` korrekt pro Monat,
   Ueberschreitung -&gt; `false`.
3. `PipelineGateServiceTest`: PASS, WARN, FAIL deterministisch.
4. `PipelineGateControllerWebTest`: multipart + JSON-Antwort.
5. `KpiServiceTest`: Burn-Down-Serie, MTTR-Mittelwert,
   SLA-Quote, Automatisierungsquote.
6. `KpiControllerWebTest`: JSON + CSV.
7. `TenantContextTest` / `JwtTenantResolverTest`: Claim-Extraktion,
   Fallback auf Default-Tenant fuer Admin.

## Sicherheits-Invarianten

1. **Vier-Augen** bei jedem Wechsel des `LlmModelProfile` (Tests
   haerten).
2. **Cost-Cap** deaktiviert KI-Funktionen automatisch; Wiederaktivierung
   nur nach neuem Monat oder manueller Erhoehung des Budgets.
3. **Gate** blockiert niemals automatisch einen Merge - liefert nur
   das Signal. Die Pipeline entscheidet via `allow_failure`.
4. **KPIs** sind read-only, keine DB-Mutationen.

## Scope NICHT IN

- Vollstaendige RLS-Durchsetzung ueber alle 20+ Tabellen.
- Schema-Migration auf bestehende Daten mit Default-Tenant fuer JEDE
  Tabelle (nur Tenant+Profile-Tabellen werden angelegt).
- GitLab-Kommentar-POST zurueck ins Merge-Request
  (Vertrag steht, Adapter folgt).
- Materialized Views fuer KPIs (aktuell Stream-Aggregation).
- KPI-Dashboard-ECharts-Widgets im Frontend.
- Executive-Report-Anhang fuer KPIs (wird via CSV exportiert,
  Verlinkung folgt).
