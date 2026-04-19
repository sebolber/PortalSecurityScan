# Offene Punkte (kumulativ)

> Historie vor 2026-04-19 siehe `docs/20260418/offene-punkte.md`.
> Neue Eintraege werden oben ergaenzt.

## Stand 2026-04-19 - Post-Session-Nacharbeit (offen)

### Konfigurationsverwaltung

- **Generischer System-Parameter-Store** (neu, geplant). Admins
  sollen Feature-Flags, Timeouts, Rate-Limits, Batch-Groessen,
  Feed-/LLM-Default-URLs und SMTP-Alert-Parameter ueber einen
  Key-Value-Dialog unter "Einstellungen" editieren koennen. Werte
  werden in einer neuen Tabelle `system_setting (setting_key, value,
  updated_at, updated_by)` persistiert und ueberschreiben die
  `application.yaml`-Defaults zur Laufzeit. Secrets werden analog
  `SbomEncryption` AES-GCM-verschluesselt abgelegt und nie im Klartext
  ausgeliefert. Vollstaendiger Migrations-Prompt mit Parameterliste,
  Architektur, Tests und Akzeptanzkriterien liegt als Arbeitsauftrag
  bereit (Session-Chat 2026-04-19); ca. 30 Kernparameter aus allen
  `@ConfigurationProperties`- und `@Value`-Stellen im Scope. Pflicht-
  Hinweise:
  - Katalog als statische Registry im Java-Code
    (`SystemSettingCatalog` in `cvm-application/systemsetting`) -
    DB haelt nur Overrides.
  - Pro Key die Metadaten `type, defaultValue, description, category,
    secret, restartRequired` pflegen, damit das UI generisch rendert.
  - Zugriffs-Wrapper `getEffective(...)` in den bestehenden
    `*Config`-Beans (ReachabilityConfig, OsvProperties, Feed*Config,
    AutoAssessmentConfig, FixVerificationConfig, AnomalyConfig,
    RuleExtractionConfig, AlertConfig, ...). Beans, die einen
    `RestClient.Builder` im Konstruktor zementieren, entweder lazy
    bauen oder `restartRequired=true` setzen.
  - Audit-Trail-Eintrag pro `PUT/DELETE` (alter + neuer Wert; Secrets
    maskiert).
  - ArchUnit-Regel: nur `cvm-application/systemsetting` greift auf
    das Repository zu.
  - Tests zuerst (SystemSettingService-Unit, Repository-Slice,
    Controller-Web, End-to-End-Pfad fuer mindestens einen Parameter -
    z.B. `cvm.ai.reachability.enabled`).

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
