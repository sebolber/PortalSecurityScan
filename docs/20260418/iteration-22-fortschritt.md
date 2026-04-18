# Iteration 22 - Go-Live-Rollout - Fortschritt

**Jira**: CVM-53
**Branch**: `claude/iteration-22-continuation-GSK7w`
**Datum**: 2026-04-18

## Umgesetzt

### 1. JWT-Tenant-Resolver (cvm-api + cvm-application)

- **`TenantLookupService`** (`cvm-application/tenant`) kapselt den
  Lesezugriff auf `TenantRepository`, damit `cvm-api` nicht gegen
  die ArchUnit-Regel "api -> persistence verboten" verstoesst.
- **`TenantContextFilter`** (`cvm-api/tenant`) ist ein
  `OncePerRequestFilter` mit `Ordered.LOWEST_PRECEDENCE - 10`
  (laeuft NACH dem Spring-Security-OAuth2-Filter):
  - liest aus dem `JwtAuthenticationToken` zuerst den Claim
    `tenant_key`, dann den Fallback `tid`,
  - mappt den Key ueber den Lookup auf eine Tenant-UUID,
  - schreibt sie in `TenantContext.set(...)`,
  - raeumt den Context im `finally`-Block auf - auch wenn der
    Chain eine Exception wirft.
- Unbekannter Tenant-Key -> Default-Tenant (tolerant, damit
  Rollout nicht bricht).
- Filter wird ueber `@Component` + `scanBasePackages` aus
  `CvmApplication` automatisch registriert.

### 2. Modell-Profil-Seed (cvm-persistence, Flyway V0023)

Neue Migration `V0023__model_profile_seed.sql`:

- Default-Tenant `default` (`00000000-0000-0000-0000-000000000001`).
- Modellprofile:
  - `CLAUDE_CLOUD_DEFAULT` (`CLAUDE_CLOUD`, `claude-sonnet-4-6`,
    Budget 100 EUR/Monat, GKV-freigegeben),
  - `OLLAMA_ONPREM_FALLBACK` (`OLLAMA_ONPREM`, `llama3:8b`,
    Budget 0 EUR, GKV-freigegeben).
- Idempotent via `ON CONFLICT (profile_key|tenant_key) DO NOTHING`
  - Flyway rechnet den Hash beim Re-Run korrekt.

### 3. Gate-Integration: MR-Kommentar zurueck
   (cvm-integration + cvm-application)

- **`GitProviderPort.postMergeRequestComment(...)`** neu (Default
  `return false`, damit altes Verhalten bestehen bleibt).
- **`FakeGitProvider`** haelt `MergeRequestComment`-Liste vor
  und kann Fehler via `simulatePostFailure(true)` simulieren.
- **`GitHubApiProvider`** postet per `POST /repos/{slug}/issues/
  {mrId}/comments` (GitHub nutzt fuer PR-Kommentare den
  Issues-Endpunkt).
- **`PipelineGateService`** erhaelt einen
  `ApplicationEventPublisher` und publiziert nach jeder
  Auswertung ein **`PipelineGateEvaluatedEvent(productVersionId,
  repoUrl, mergeRequestId, result)`**.
- **`GateRequest`** bekommt ein optionales Feld `repoUrl`; ein
  zweiter Konstruktor erhaelt die alte 5-Parameter-Signatur
  (Backwards Compat fuer Bestandstests).
- **`PipelineGateController`** akzeptiert `repoUrl` als
  Multipart-Form-Feld und reicht ihn durch.
- **`PipelineGateMrCommentListener`** (`cvm-integration/git`) hoert
  auf das Event und postet bei aktivem Feature-Flag
  (`cvm.pipeline.gate.post-mr-comment=true`) einen Markdown-
  Kommentar mit PASS/WARN/FAIL-Icon und den Zaehlern. Provider-
  Fehler loggt er als Warnung und brechen das Gate nicht.

### 4. KPI-Tages-Snapshot (cvm-persistence + cvm-application,
   Flyway V0024)

- **V0024__kpi_snapshot_daily.sql**: Tabelle
  `kpi_snapshot_daily (id, snapshot_day, product_version_id,
  environment_id, open_critical/high/medium/low/informational,
  automation_rate, created_at)` mit partiellen Unique-Indexen
  pro Scope (pv+env, nur pv, nur env, global).
- **`KpiSnapshotDaily`**-Entity + **`KpiSnapshotDailyRepository`**
  mit `findByScope(day, pv, env)`.
- **`KpiSnapshotWriter.persistSnapshot(pv, env)`** ruft
  `KpiService.compute(...)`, schreibt (oder aktualisiert, wenn
  Tages-Snapshot fuer den Scope bereits existiert) eine Zeile.
  Idempotent pro Tag/Scope.
- **`KpiDailySnapshotJob`** (Cron `0 0 1 * * *`) schreibt einen
  globalen Snapshot (pv=null, env=null). Scope-Snapshots koennen
  ueber den Writer direkt angezogen werden.
- Feature-Flag `cvm.kpi.snapshot.enabled` (Default `true`).

## Architektur-Invarianten

- `cvm-api -> cvm-persistence` bleibt hart verboten - der
  Lookup-Service in `cvm-application` kapselt den Zugriff.
- Das Event `PipelineGateEvaluatedEvent` lebt in
  `cvm-application`, der Listener im darueberliegenden
  `cvm-integration`. Damit bleibt die Richtung
  `integration -> application` erhalten.
- `@EnableScheduling` ist global (ueber
  `CveEnrichmentScheduler` aktiviert); der neue KPI-Job nutzt
  denselben Scheduler.

## Tests

Zuwachs dieser Iteration:

- `TenantContextFilterTest` (5 Tests):
  - tenant_key-Claim greift,
  - fehlt der Claim -> Default,
  - unbekannter Key -> Default,
  - Exception im Chain raeumt Context auf,
  - ohne Authentifizierung -> Default.
- `PipelineGateServiceTest#publiziertEvent` (1 neuer Test,
  Gesamt 8).
- `PipelineGateMrCommentListenerTest` (5 Tests):
  Kommentar posten, disabled, ohne Kontext, Provider-Fehler,
  PASS-Rendering ohne Aktionsempfehlung.
- `KpiSnapshotWriterTest` (2 Tests): Neuer Snapshot,
  idempotente Aktualisierung.

`./mvnw -T 1C test` -> **BUILD SUCCESS**, 471 Tests, 0 Fehler,
5 Docker-skipped (unveraendert).

## NICHT in dieser Iteration

- **Vollstaendige RLS-Policies** auf Sachtabellen (braucht
  Testcontainers/Postgres fuer echten Isolations-Test;
  Planung in Rollout-Phase).
- **`tenant_id`-Spalte auf allen Sachtabellen** - bleibt
  bewusst offen bis der Default-Tenant aus V0023 in Produktion
  angekommen ist.
- **KPI-ECharts-Widgets im Angular-Frontend**
  (eigene Frontend-Iteration).
- **Executive-Report CSV-Anhang im PDF** (PDF-Layout-Aufwand,
  bleibt in den offenen Punkten).
- **`kpi_snapshot_daily`-Aggregation im KpiService** - aktuell
  persistiert der Writer, die Historie-Abfrage (Burn-Down aus
  Snapshot statt Stream) bleibt naechste Iteration.
