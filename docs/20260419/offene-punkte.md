# Offene Punkte (kumulativ)

> Historie vor 2026-04-19 siehe `docs/20260418/offene-punkte.md`.
> Neue Eintraege werden oben ergaenzt.

## Stand 2026-04-19 - Post-Session-Nacharbeit (offen)

### Konfigurationsverwaltung

- **System-Parameter-Store parallel auf main gebaut** (Classes
  `SystemParameterService`, `SystemParameterValidator`,
  `SystemParameterView`, `SystemParameterCommands`,
  `SystemParameterAuditLogView`, REST-Controller, Angular-Komponente
  `admin-parameters`). Dieser Eintrag ist damit in Teilen erledigt.
  Noch offen (ehemaliger Migrations-Prompt aus der Session
  2026-04-19):
  - **Katalog-Befuellung Block A.1** (AI_LLM-Fallbacks,
    AI_REACHABILITY, RAG, ANOMALY, COPILOT) - erledigt in
    Iteration 41 (Bootstrap `SystemParameterCatalogBootstrap` +
    statischer Katalog `SystemParameterCatalog`). Defaults sind 1:1
    mit den `@Value`-Fallbacks abgeglichen.
  - **Katalog-Befuellung Block A.2** (ENRICHMENT, PIPELINE_GATE,
    MAIL/Alerts, SCAN, SCHEDULER, SECURITY) - erledigt in
    Iteration 42. Secrets bleiben weiterhin ausgespart bis
    Iteration 45.
  - **Zugriffs-Wrapper** (`getEffective(...)`) in den bestehenden
    `*Config`-Beans: ReachabilityConfig, OsvProperties, Feed*Config,
    AutoAssessmentConfig, FixVerificationConfig, AnomalyConfig,
    RuleExtractionConfig, AlertConfig, AssessmentConfig. Beans, die
    einen `RestClient.Builder` im Konstruktor zementieren, entweder
    lazy bauen oder explizit `restartRequired=true` kennzeichnen und
    das UI ein "Neustart noetig"-Badge anzeigen lassen.
  - **Secret-Behandlung** (AES-GCM, Hint statt Klartext, analog
    `SbomEncryption`) fuer Keys wie `cvm.llm.claude.api-key`,
    `cvm.ai.fix-verification.github.token`,
    `cvm.feed.nvd.api-key`, `cvm.feed.ghsa.api-key`,
    `cvm.encryption.sbom-secret`.
  - **ArchUnit-Regel**: nur das Parameter-Modul greift aufs
    Repository.
  - **End-to-End-Test** fuer mindestens einen Parameter
    (`cvm.ai.reachability.enabled`), der nachweist, dass eine
    DB-Aenderung den Agent-Aufruf ohne Restart beeinflusst.
  - **Nicht migrieren** (dokumentieren, damit nachher niemand
    versucht): `spring.datasource.*`, `spring.jpa.*`,
    `spring.flyway.*`, `spring.security.oauth2.resourceserver.jwt.*`,
    `spring.mail.*`, `spring.servlet.multipart.*`, `server.port`,
    `management.endpoints.*`, `logging.level.*`,
    `cvm.llm.pricing.*`, `cvm.enrichment.osv.base-url`,
    `cvm.feed.*.base-url`.

- **LLM-Konfiguration bleibt eigenstaendig** (Entscheidung,
  dokumentiert damit die Migration nicht versehentlich spaeter
  aufgemacht wird).
  - Eine LLM-Konfiguration ist kein skalarer Parameter, sondern eine
    strukturierte Entity mit eigenem Lebenszyklus: Mandanten-Scope
    (mehrere Eintraege pro Tenant), Pflicht-Invariante "genau eine
    aktiv", Entity-Validierung (Provider-Whitelist, Azure verlangt
    explizite `baseUrl`, Temperatur-Range [0,1], `maxTokens > 0`,
    eindeutiger Name pro Mandant), ein **eigenes Secret pro Zeile**
    (AES-GCM, Hint-Anzeige statt Klartext), "Test-Verbindung"-Button,
    Aktivierungs-Semantik via `deaktiviereAndereAktive`.
  - Eine Ueberfuehrung in Key-Value-Paare wuerde diese Semantik
    zerstoeren (`cvm.llm.configs[0].name`-Antipattern, keine atomare
    "genau eine aktive"-Operation, keine zeilenweise Validierung) und
    die bestehende UI fachlich verschlechtern.
  - Aktionen:
    - `llm_configuration`-Tabelle, `LlmConfigurationService`,
      `/api/v1/admin/llm-configurations` und die Angular-Seite
      `admin-llm-configurations` unveraendert lassen.
    - Der neue System-Parameter-Store nimmt nur die **globalen
      Fallback-/Infrastruktur-Schalter** auf (`cvm.llm.enabled`,
      `cvm.llm.default-model`, `cvm.llm.injection.mode`,
      `cvm.llm.claude.*`, `cvm.llm.ollama.*`, `cvm.llm.openai.default-model`,
      `cvm.llm.rate-limit.*`, `cvm.llm.embedding.*`). Diese Werte
      wirken nur, wenn fuer einen Mandanten keine aktive
      `LlmConfiguration` hinterlegt ist oder wenn der jeweilige
      Adapter beim Boot mit Defaults gebaut wird.
  - Gleiche Logik (bleibt Entity, nicht Key-Value) gilt analog fuer:
    `ModelProfile`, `Rule`, `ContextProfile`, `BrandingConfig`,
    `Tenant`, `Waiver`, `AlertRule`, `Product`, `ProductVersion`,
    `Environment`.

## Stand 2026-04-19 - Session-Nachzuege (bereits umgesetzt)

- ~~**pgvector-Insert bricht mit "column embedding is of type vector
  but expression is of type character varying"** beim Assessment-
  Approve ab~~ - erledigt (`@ColumnTransformer(write = "?::vector")`
  statt `@JdbcTypeCode`). Testcontainers-Regressionstest unter
  `AiEmbeddingPgvectorTest`.
- ~~**CVE-Liste 500 "text ~~ bytea"**~~ - erledigt, Null-String
  vermieden (`:searchLower = ''` statt `IS NULL`).
- ~~**Mitigation-Strategie als Freitext** liess ungueltige Enum-Werte
  durch~~ - erledigt, Select mit den sechs MitigationStrategy-Werten.
- ~~**LLM-Konfiguration ohne Test-Button**~~ - erledigt
  (`LlmConnectionTester`-Port + HTTP-Impl in `cvm-integration`, zwei
  Endpoints `POST /test` und `POST /{id}/test`, Frontend-Button im
  Formular + pro Tabellenzeile, WireMock-Tests).
- ~~**Reachability ohne Start-Button im UI**~~ - erledigt, Dialog
  `ReachabilityStartDialogComponent` aus dem Queue-Detailpanel
  heraus.
- ~~**Reachability erwartet frei eingetipptes Symbol**~~ - erledigt,
  `PurlSymbolDeriver` (pure) + `GET /api/v1/findings/{id}/reachability/
  suggestion`; Dialog befuellt `vulnerableSymbol` und `language`
  vor.
- ~~**start.sh verlangt zwingend einen Branch**~~ - erledigt,
  Branch-Argument ist jetzt optional (nur git-Update wenn
  uebergeben).

## Stand 2026-04-19 - weiter offen (uebernommen aus 20260418)

Siehe `docs/20260418/offene-punkte.md`, insbesondere:

- Profil-Edit / -Soft-Delete im Frontend.
- OSV-Mirror fuer air-gapped-Installationen.
- PURL-Canonicalization, falls Trefferquote in Prod zu niedrig.
- Bundle-Budget-Reduktion.
- Rules-Editor im Frontend.
- Profil-YAML-Editor (Monaco) im Frontend.
- Tenant-Verwaltungs-UI.
- KPI-UI (ECharts, Burn-Down, SLA-Ampel).
- JGit-Adapter fuer Reachability (aktuell `NoopGitCheckoutAdapter`),
  SSH-Key aus Vault, Network-Sandboxing fuer den Subprocess.
- Auto-Trigger der Reachability, wenn AI-Vorschlag-Confidence unter
  Schwelle.
- Playwright-E2E + axe-core in CI, Karma in CI, Testcontainers-IT auf
  Docker-Desktop-macOS.
- Diverse Audit-/Cleanup-/Performance-Nachzuege (siehe 20260418).
