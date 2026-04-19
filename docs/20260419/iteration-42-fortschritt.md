# Iteration 42 - Fortschritt

**Thema**: Parameter-Katalog Block A.2 (ENRICHMENT,
PIPELINE_GATE, MAIL/Alerts, SCAN, SCHEDULER, SECURITY).

**Jira**: CVM-92.

## Was gebaut wurde

- `SystemParameterCatalog` um sechs Kategorien erweitert (insgesamt
  61 Eintraege). Defaults abgeglichen mit:
  - `OsvProperties` (Enabled/Batch/Timeout/Retry/Max-Retry-After),
  - `FeedProperties.FeedConfig` (NVD/GHSA/KEV/EPSS Enabled/Requests/
    Window),
  - `PipelineGateRateLimiter`, `PipelineGateMrCommentListener`,
  - `AlertConfig` (dry-run/log/live, From, T1/T2),
  - `AssessmentConfig`, `ScanDeltaSummaryService`,
    `NlQueryService`,
  - `RuleExtractionConfig`/`RuleExtractionJob` (inkl. Cron),
  - `FixVerificationConfig`/`OpenFixWatchdog` (inkl. Cron),
  - `ProfileAssistantConfig`/`ProfileAssistSessionCleanupJob`
    (inkl. Cron),
  - `cvm.scheduler.enabled` (Default true),
  - `WebSecurityConfig.cvm.security.cors.allowed-origins`.
- Test-Erweiterung in `SystemParameterCatalogTest` (5 neue Faelle):
  Vollstaendigkeit der Keys, Default-Abgleich, Typ-Konsistenz,
  Kategorie-Abdeckung, Einhaltung der "Nicht-migrieren"-Liste.

## Was bewusst NICHT in dieser Iteration kam

- Wrapper `getEffective(...)` in den `*Config`-Beans - Iteration 43/44.
- AES-GCM-Secrets (`api-key`, `token`, `sbom-secret`) - Iteration 45.
- ArchUnit-Regel + E2E-Test - Iteration 46.

## Hinweise fuer den naechsten Start

- Keine Flyway-/Dependency-Aenderung. Kein erneutes
  `./mvnw -T 1C clean install -DskipTests` noetig.
- Bestehende `@Value`-Fallbacks koennen unveraendert bleiben: der
  Katalog ist zunaechst ein reines UI-Pflegewerkzeug; der
  Lese-Pfad (Parameter-Store gewinnt gegenueber `application.yaml`)
  folgt in Iteration 43/44.

## Vier Leitfragen (Oberflaeche)

Keine UI-Aenderung. Der bestehende `admin-parameters`-Screen listet
die neuen Eintraege nach dem Seeding automatisch, und die bereits
eingebaute Kategorie-Filterung (`?category=...`) funktioniert mit
den neuen Kategorie-Konstanten.

## Offene Punkte

- Einige Alerts-Modi (log, live) sind im `AlertConfig` noch nicht
  vollstaendig implementiert. Der Katalog bietet sie bereits als
  SELECT-Optionen an; die Implementierung folgt bei Bedarf in einer
  separaten Iteration.
