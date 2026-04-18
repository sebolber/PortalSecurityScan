# Go-Live-Abarbeitung - Offene Punkte Tier 1/2

**Stand**: 2026-04-18 (nach Iteration 21).

Dieser Report fasst die in dieser Session abgearbeiteten Punkte
aus `offene-punkte.md` zusammen. Nicht angefasste Punkte bleiben
dort stehen.

## Abgeschlossen

### Tier 1 (Go-Live-Essentials)

1. **Cost-Cap zentral im Audit-Service**
   - Neuer Port `CostBudgetPort` in `cvm-llm-gateway`.
   - Fallback-Adapter `UnlimitedCostBudgetAdapter` (greift, solange
     kein Tenant-/Profil-Kontext gesetzt ist).
   - Echter Adapter `LlmCostGuardAdapter` in `cvm-ai-services`, der
     `environmentId -&gt; Environment -&gt; llmModelProfileId` aufloest
     und dann an `LlmCostGuard` delegiert.
   - `AiCallAuditService.execute(...)` pruegt VOR dem Call; bei
     Budget-Ueberschreitung wird das Audit als `DISABLED` mit
     "Monatsbudget aufgebraucht" finalisiert, der LLM-Call faellt aus.
   - Neuer Test `AiCallAuditServiceTest#kostenCapGreift`.

2. **Rate-Limit am Pipeline-Gate**
   - `PipelineGateRateLimiter` auf Bucket4j-Basis, pro
     `productVersionId`, 20 Calls/min (konfigurierbar via
     `cvm.pipeline.gate.per-minute`).
   - `PipelineGateService.evaluate(...)` ruft den Limiter als ersten
     Schritt.
   - Neue `GateRateLimitException` -&gt; HTTP 429 via Handler.
   - Neuer Test `PipelineGateServiceTest#rateLimit`.

3. **GitLab-CI-Gate-Template `.gitlab-ci-cvm-gate.yml`**
   - Drop-in-Include fuer Produkt-Pipelines, nutzt Trivy zur
     SBOM-Erzeugung und postet an `/api/v1/pipeline/gate`.
   - Liest Antwort, mappt Gate-Ergebnis auf Exit-Code
     (PASS=0, WARN=0|1 je nach `CVM_GATE_ALLOW_WARN`, FAIL=2).

### Tier 2 (hochwertige Aufraeumarbeiten)

4. **`AssessmentExpiredEvent` + Publikation**
   - Record `AssessmentExpiredEvent(assessmentIds, expiredAt)`.
   - `AssessmentWriteService.expireIfDue` publiziert das Event
     nach dem Batch-Update.
   - Zwei neue Tests.

5. **`findActiveByProductVersionAndEnvironment`-Query**
   - JPQL-Query im `AssessmentRepository` mit Index-freundlichem
     `productVersion.id`/`environment.id`/`supersededAt`-Filter.
   - `HardeningReportDataLoader` nutzt die neue Query statt
     `findAll().stream().filter(...)`.

6. **Reports-Listing-Endpoint `GET /api/v1/reports?...`**
   - Pagenierte Sicht, optional gefiltert nach `productVersionId`
     und `environmentId`, sortiert absteigend nach `erzeugtAm`.
   - PDF-Bytes werden weggelassen - Download via
     `GET /api/v1/reports/{id}`.
   - Neue Repository-Methoden, neuer Service-Pfad `list(...)`,
     neuer Web-Test.

7. **FixVerification Cache-Eviction (Cron)**
   - `FixVerificationService.purgeExpiredCache()` entfernt Eintraege
     aelter als `2 * cacheTtlMinutes`.
   - Neuer `FixVerificationCacheEvictionJob` (Cron taeglich 03:30).
   - ConcurrentHashMap-Speicher waechst nicht mehr bis
     JVM-Restart.

8. **Delta-Summary-Persistenz**
   - Flyway `V0022__scan_delta_summary.sql`.
   - `ScanDeltaSummaryEntity` + Repository in `cvm-persistence`.
   - `ScanDeltaSummaryService` persistiert jede erzeugte Summary
     (Initial-, Below-Threshold-, LLM-Variante) auditfaehig.
   - Neuer Test `initialRunPersistiert`.

9. **ProfileAssistSession-Cleanup-Cron**
   - Repository-Methoden `findCleanupKandidaten(Instant)` +
     `deleteByIds(List)`.
   - Neuer `ProfileAssistSessionCleanupJob` (Cron 02:15).
   - `cvm.ai.profile-assist.cleanup-days` (Default 7 Tage).
   - Neuer Test mit 2 Szenarien.

## Nicht in dieser Session

Diese Punkte aus `offene-punkte.md` sind bewusst offen geblieben
(abhaengig von Infrastruktur, konzeptioneller Klaerung oder zu
invasiv fuer eine Aufraeum-Session):

- Volle RLS-Durchsetzung ueber alle Tabellen.
- GitLab-MR-Kommentar zurueck aus dem Gate.
- KPI-ECharts-UI + NL-Query-UI + Executive-Download-Button.
- `AuditedLlmResponse(response, auditId)`-Refactor (quer durch alle
  KI-Services).
- Vault-Binding (Jasypt, Embeddings, SSH-Key).
- Playwright-E2E-Suite.
- Postgres-Trigger fuer Audit-Immutability.
- Testcontainers-Integrationstests (CI-Docker).
- Anthropic-Embedding-Adapter (API noch Beta).

## Build-Status

`./mvnw -T 1C test` -&gt; BUILD SUCCESS.
- cvm-application: +4 Tests (Cost-Cap, Rate-Limit, Expiry-Event).
- cvm-ai-services: +3 Tests (Delta-Persistenz, Session-Cleanup).
- cvm-api: +1 Test (Reports-Listing).
- cvm-llm-gateway: +1 Test (Cost-Cap im Audit-Service).
- Keine bestehenden Tests rot.
