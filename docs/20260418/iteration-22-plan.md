# Iteration 22 - Go-Live-Rollout Plan

**Jira**: CVM-53
**Branch**: `claude/iteration-22-continuation-GSK7w`
**Datum**: 2026-04-18

## Auftrag

Nach Iteration 21 und der Go-Live-Abarbeitung (siehe
`docs/20260417/go-live-abarbeitung.md`) bleiben auf der
Go-Live-Checkliste (`docs/20260417/offene-punkte.md`,
Stand nach Iteration 21) noch Punkte offen, die fuer den
Produktiv-Rollout benoetigt werden. Iteration 22 arbeitet
davon die pragmatisch umsetzbaren Bloecke ab.

## Scope dieser Iteration

### In Scope

1. **JWT-Tenant-Resolver** (`cvm-api`)
   - `TenantContextFilter` liest das Claim `tenant_key`
     (Fallback `tid`) aus dem JWT, mappt ueber `TenantRepository`
     auf eine `Tenant`-Id und setzt `TenantContext.set(...)`.
   - Fallback auf den Default-Tenant, wenn der Claim fehlt.
   - Filter raeumt den Context in `finally` auf.
   - Unit-Test mit Mock-JWT + MockHttpServletRequest.

2. **Modell-Profil-Seed pro Mandant** (`cvm-persistence`)
   - Flyway-Migration `V0023__model_profile_seed.sql`.
   - Legt zwei Profile an:
     - `CLAUDE_CLOUD_DEFAULT` (Claude-Cloud, Sonnet-4.6,
       Monatsbudget 100 EUR, GKV-freigegeben).
     - `OLLAMA_ONPREM_FALLBACK` (Ollama, llama3:8b,
       Monatsbudget 0, GKV-freigegeben).
   - Idempotent ueber `ON CONFLICT (profile_key) DO NOTHING`.
   - Seed des Default-Tenants (`id = fixed UUID`,
     `tenant_key = 'default'`), falls noch nicht vorhanden.

3. **Gate-Integration: MR-Kommentar zurueck** (`cvm-integration`
   + `cvm-application`)
   - `GitProviderPort` erweitert um
     `postMergeRequestComment(repoUrl, mergeRequestId, body)`.
   - `FakeGitProvider` speichert den Kommentar in einer
     Liste (fuer Tests).
   - `GitHubApiProvider` implementiert `POST
     /repos/{slug}/issues/{mrId}/comments` (PRs teilen
     sich das Issue-Comment-Endpoint mit Issues auf GitHub).
   - `PipelineGateService` ruft den Port nach der
     Auswertung optional auf, wenn `mergeRequestId` und
     `repoUrl` gesetzt sind. Fehler beim Post duerfen den
     Gate nicht brechen (nur Warnung loggen).
   - Tests: `PipelineGateServiceTest#postetMrKommentar` +
     `FakeGitProviderTest`.

4. **KPI-Daily-Snapshot** (`cvm-persistence` + `cvm-application`)
   - Flyway-Migration `V0024__kpi_snapshot_daily.sql` mit
     Tabelle `kpi_snapshot_daily`
     (`id, snapshot_day, product_version_id, environment_id,
     open_critical, open_high, open_medium, open_low,
     open_informational, automation_rate, created_at`).
   - `KpiSnapshotDaily`-Entity + Repository.
   - `KpiSnapshotWriter.persistSnapshot(pv, env)` ruft
     `KpiService.compute` und schreibt die aktuellen
     offenen Zahlen + Automatisierungsquote weg.
   - `KpiDailySnapshotJob` (Cron 01:00) iteriert ueber alle
     (productVersion, environment)-Paare mit aktiven
     Assessments und persistiert je einen Snapshot.
   - Unit-Test `KpiSnapshotWriterTest`.

### Nicht in dieser Iteration

- **RLS-Policies** auf allen Sachtabellen
  (braucht Testcontainers/Postgres).
- **Executive-Report CSV-Anhang** im PDF
  (separate Layout-/Attachment-Iteration, zu invasiv).
- **KPI-UI (ECharts)** im Angular-Frontend
  (Frontend-Iteration separat).
- **`tenant_id`-Spalte auf allen Sachtabellen**
  (Rollout-Migration in eigener Phase, mit RLS gebuendelt).

## Test-Strategie

Je Feature mindestens ein Unit-Test, fachlich deutsche
`@DisplayName`:

- `TenantContextFilterTest`: liest claim, faellt auf default
  zurueck, raeumt auf.
- `PipelineGateServiceTest#postetMrKommentar`: FakeGitProvider
  erhaelt genau einen Post-Call mit PASS/WARN/FAIL im Body.
- `PipelineGateServiceTest#mrKommentarFehlerBrichtGateNicht`:
  Post wirft, Gate liefert trotzdem den Result.
- `KpiSnapshotWriterTest#persistierenMitOffenenSeverities`:
  zwei offene CRITICAL + ein APPROVED -> Snapshot mit 2/0/0/0/0.
- `FakeGitProviderTest#kommentarWirdAbgelegt`.

## Abschlusskriterien

- `./mvnw -T 1C test` -> BUILD SUCCESS.
- `docs/20260418/iteration-22-fortschritt.md`,
  `docs/20260418/iteration-22-test-summary.md` und
  `docs/20260418/offene-punkte.md` (kumulativ) aktualisiert.
- Commit mit Conventional-Commit-Titel + Jira-Key `CVM-53`.
- Push auf `claude/iteration-22-continuation-GSK7w`.
