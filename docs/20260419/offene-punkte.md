# Offene Punkte (kumulativ)

> Historie vor 2026-04-19 siehe `docs/20260418/offene-punkte.md`.
> Neue Eintraege werden oben ergaenzt.

## Stand 2026-04-19 abends - UX-Verbesserungsplan (laufend)

Quelle: Senior-UX-Review (siehe Chat-Verlauf 2026-04-19). Die
Iterationen 80+ implementieren die Befunde.

- ~~U-01a Workflow-CTAs Scan/Queue/Dashboard~~ - **Erledigt in
  Iteration 80** (CVM-320). Scan-Upload->Queue-Deep-Link,
  Queue-Empty-State, Dashboard-Handlungskarten.
- **U-01b Workflow-CTAs Reachability/FixVerif/Anomaly/Waiver**
  - Iteration 81. Ziel: Reachability-CTA "zurueck zur Queue",
  FixVerif "Waiver anlegen", Anomaly "Zum Finding",
  Waiver-Detail "Bericht erzeugen".
- **U-02a Queue-Status-Filter + URL-Persistenz** - Iteration 82.
  Status-Chips (ALLE/OFFEN/APPROVED/REJECTED/NEEDS_VERIFICATION),
  Filter-State via `ActivatedRoute.queryParams`.
- **U-02b CVE/KPI/FixVerif-Filter-URL + KPI-Presets** -
  Iteration 83.
- **U-02c Tenant-Badge im Shell + Default-Switch** -
  Iteration 84.
- **U-03 Waiver-Extend/Revoke-Aktionen + Expiry-Banner** -
  Iteration 85. Backend existiert; UI fehlt.
- **U-04a Vier-Augen-Warnung im Queue-Detail** - Iteration 86.
  Rote Box + Approve-Disabled, wenn Autor == Approver.
- **U-04b Assessment-Audit-Trail im Queue-Detail** -
  Iteration 87. Neuer Backend-Endpoint
  `GET /findings/{id}/assessments/history`, Reiter "Historie".
- **U-05 Reachability-Detail + Start-Dialog aus Liste** -
  Iteration 88. Slide-In mit Rationale/Confidence; Finding-
  Autocomplete; Feature-Flag-Banner bei deaktiviertem Feature.
- **U-06 Einheitliches Admin-Aktions-Menue** - Iteration 89-90.
  Dreipunkt-Menue in Rules/Products/Environments/Profiles,
  `window.prompt` -> `cvm-dialog`.
- **U-07a Breadcrumbs + globale Shortcut-Sheet** - Iteration 91.
- **U-07b Globale Suche + Bell-Notifications** - Iteration 92.
- **U-08 Dashboard-Handlungszentrale + Report-Listing** -
  Iteration 93-94. Benoetigt kleinen Backend-Endpoint
  `GET /reports`.
- **U-09 Secrets-Show/Hide-Toggle + Rotation** - Iteration 95.
- **U-10 Erstnutzer-Wizard** - Iteration 96.

## Stand 2026-04-19 - UI-Harmonisierung (Iteration 61) - laeuft

- **Komplettumbau auf reines Tailwind-Design-System** (CVM-62).
  Angular Material + CDK + material-icons werden entfernt, ersetzt
  durch Tailwind-`@layer components`, Lucide-Icons und Fira Sans
  Condensed. Siehe `docs/20260419/iteration-61-plan.md`.
  Umsetzung laeuft in den Unter-Iterationen **61A-61H**.
- Folgende bisherige Eintraege werden durch Iteration 61
  **obsolet** und gelten als geschlossen oder aufgehoben:
  - ~~Feature-Bereichs-Migration auf `Ahs*`-Komponenten~~
    (aufgehoben - `Ahs*`-Primitive werden durch `Cvm*`-Tailwind-
    Primitive ersetzt).
  - ~~Stylelint-Guard gegen Material-Hex-Farben~~ (aufgehoben -
    Material verschwindet komplett, Guard wird durch ESLint-Regel
    `no-restricted-imports` fuer `@angular/material|cdk` ersetzt).
  - ~~FontAwesome-Thin-Umstellung~~ (aufgehoben - Icon-System ist
    Lucide).
  - ~~Einzelseiten-Redesign (Queue/Dashboard/Profile/Regel-Editor)~~
    (uebernommen in Iteration 61D/E/F).
  - ~~Iteration 27 Token-Layer Feinarbeit~~ (aufgehoben - Token-Layer
    wird in 61A konsolidiert, Legacy-`--cvm-*`-Aliase entfernt).

### ~~Bug: Profil-Save meldet irrefuehrenden 404 (Diff-Endpunkt)~~

- **Erledigt in Iteration 63** (CVM-300). `ProfileController#diff`
  liefert bei `against=latest` ohne aktive Vorgaenger-Version jetzt
  HTTP 200 + `[]`; 404 bleibt nur bei unbekannter Profil-ID.
  `ProfilesService#diffGegenAktiv` nutzt `ApiClient.getOptional` und
  gibt `[]` bei 404 zurueck. `ProfilesComponent#draftSpeichern`
  setzt `fehler: null` nach erfolgreichem Save explizit. Neuer
  Spec `profiles.service.spec.ts`; 3 neue Backend-Tests in
  `ProfileControllerWebTest`.

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
  - **Zugriffs-Wrapper** (`getEffective(...)`) Teil 1 erledigt in
    Iteration 43 fuer ReachabilityConfig, AutoAssessmentConfig,
    neuer OsvEffectiveProperties, neuer FeedEffectiveProperties.
    Teil 2 erledigt in Iteration 44 fuer FixVerificationConfig,
    RuleExtractionConfig, AlertConfig, AssessmentConfig,
    AnomalyConfig. `restartRequired`-Marker am Katalog + UI-Chip
    "Neustart noetig" ebenfalls in Iteration 44.
  - **Callsite-Migration** fuer OSV/Feed-Clients
    (`OsvComponentLookup`, `NvdFeedClient`, `GhsaFeedClient`,
    `KevFeedClient`, `EpssFeedClient`) vom statischen
    `Feed*Properties` auf die neuen `*EffectiveProperties`. Erfordert
    Anpassung der Mock-Tests und bleibt als eigener Punkt offen.
  - **Secret-Behandlung** erledigt in Iteration 45: neuer
    `SystemParameterSecretCipher` (AES-GCM analog `SbomEncryption`),
    Verschluesselung auf Service-Ebene, Entschluesselung im Resolver,
    vier Secret-Eintraege (`cvm.llm.claude.api-key`,
    `cvm.feed.nvd.api-key`, `cvm.feed.ghsa.api-key`,
    `cvm.ai.fix-verification.github.token`), Bootstrap seedet keinen
    Wert fuer sensitive Eintraege.
  - `cvm.encryption.sbom-secret` bleibt bewusst ausserhalb des
    Parameter-Stores (Master-Key fuer SBOM-Verschluesselung).
  - ~~**Offen**: `cvm.encryption.parameter-secret` muss in
    Produktion per Vault/OpenShift-Secret gesetzt werden~~ -
    **Erledigt in Iteration 69** (CVM-306). Deployment-Doku in
    `docs/konzept/parameter-secret-deployment.md`: Bezugsquellen
    pro Umgebung, OpenShift-Secret-Template, Rollout-Checkliste,
    Dual-Write-Key-Rotation, Backup-Strategie, Abgrenzung zu
    anderen Secrets, Fehlerbehandlung.
  - **Offen**: Callsite-Migration der Adapter (`ClaudeApiClient`,
    `NvdFeedClient`, `GhsaFeedClient`, `GitHubApiProvider`) vom
    `@Value` auf den Resolver (+ Lazy-Bean-Build, damit DB-Aenderung
    ohne Neustart greift).
    - **Teil erledigt in Iteration 66** (CVM-303): ClaudeApiClient
      laeuft jetzt ueber neuen Port `LlmGlobalParameterResolver`
      (im `cvm-llm-gateway`-Modul) + Adapter
      `SystemParameterLlmGlobalResolver` (in `cvm-ai-services`).
      RestClient wird lazy rebuilt, wenn sich
      `cvm.llm.claude.base-url` oder `cvm.llm.claude.timeout-
      seconds` aendert. Katalog-Flags der Claude-Keys (ausser
      `version`) entsprechend auf `hotReload=true`,
      `restartRequired=false` umgestellt.
    - **Teil erledigt in Iteration 67** (CVM-304): NvdFeedClient
      und GhsaFeedClient lesen `cvm.feed.nvd.api-key` und
      `cvm.feed.ghsa.api-key` pro Call aus dem
      `SystemParameterResolver`. `GhsaFeedClient#isEnabled()`
      nutzt den aufgeloesten Token. Katalog-Flags beider Secrets
      auf live-reloadable. KEV und EPSS haben keinen api-key und
      brauchen keine Callsite-Migration.
    - **Teil erledigt in Iteration 68** (CVM-305):
      GitHubApiProvider loest `cvm.ai.fix-verification.github.token`
      pro Call auf; RestClient wird ohne Default-Authorization-
      Header gebaut, der Token wird pro Call als Bearer-Header
      gesetzt. Damit ist die Callsite-Migration der Adapter
      **komplett**. Alle vier Secrets (Claude, NVD, GHSA,
      GitHub) sind im Katalog live-reloadable.
  - **ArchUnit-Regel** erledigt in Iteration 46 (zwei Regeln:
    Repository und Secret-Cipher duerfen nur aus
    `com.ahs.cvm.application.parameter..` referenziert werden).
  - **End-to-End-Test** fuer `cvm.ai.reachability.enabled`
    erledigt in Iteration 46 (`ReachabilityRuntimeOverrideE2ETest`
    mit realen Service-Klassen + Mock-Repository). Ein zusaetzlicher
    Testcontainers-Lauf bleibt als CI-Follow-up offen
    (Sandbox-Docker nicht verfuegbar).
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

- ~~Profil-Edit / -Soft-Delete im Frontend~~ - **Erledigt in
  Iteration 64** (CVM-301). `ProfilesComponent` bietet pro DRAFT-
  Panel die Aktionen "Draft bearbeiten" (oeffnet Monaco-Editor,
  Folge-Save ruft `draftAktualisieren`) und "Draft loeschen"
  (`window.confirm` + `CvmToastService`). Karma-Specs fuer
  Service + Component ergaenzt.
- ~~**Persistente DRAFTs pro Umgebung ueber Sessions hinweg**~~
  - **Erledigt in Iteration 74** (CVM-311). Neuer Service-Call
  `ContextProfileService.latestDraftFor(envId)` und Endpunkt
  `GET /api/v1/environments/{id}/profile/draft`; die UI laedt
  ihn in `ProfilesComponent.laden()` pro Umgebung mit und baut
  den DRAFT-Panel inklusive Diff neu auf.
- ~~OSV-Mirror fuer air-gapped-Installationen~~ - **Teil erledigt
  in Iteration 72** (CVM-309). `OsvJsonlMirror` + Spring-Bean
  `OsvJsonlMirrorLookup` (`@Primary`, `@ConditionalOnProperty`).
  JSONL-Datei wird beim Boot einmal in den Speicher gelesen.
  Reload-Endpunkt `POST /api/v1/admin/osv-mirror/reload` in
  Iteration 73 (CVM-310) ergaenzt. Reload-Button im Admin-UI
  (an `/admin/cve-import`) in Iteration 75 (CVM-312) ergaenzt.
  Exakter Versions-Filter (`affected.versions`) in Iteration 78
  (CVM-315) ergaenzt. Numerischer semver-Range (X.Y.Z) fuer
  `ranges[0].events.introduced/fixed` in Iteration 79
  (CVM-316) ergaenzt. Verbleibende Follow-ups:
  ecosystem-spezifische Version-Sortierung (Maven/PEP440/
  npm-prerelease), mehrere Ranges pro Advisory,
  `last_affected`-Semantik.
- ~~PURL-Canonicalization~~ - erledigt in Iteration 58
  (`com.ahs.cvm.domain.purl.PurlCanonicalizer`, Integration in
  `ComponentCveMatchingOnScanIngestedListener`).
- ~~Bundle-Budget-Reduktion~~ - erledigt in Iteration 52
  (2.13 MB -> 1.10 MB, ECharts und LoginCallback lazy).
- ~~Rules-Editor im Frontend~~ - Update-Form erledigt in Iteration 53.
- ~~Profil-YAML-Editor (Monaco) im Frontend~~ - Editor eingebunden
  in Iteration 54. Side-by-Side Monaco-Diff erledigt in
  Iteration 57.
- Tenant-Verwaltungs-UI: Liste (Iteration 56) + Anlage (Iteration 59)
  erledigt. Aktivierung/Deaktivierung nach Anlage, Default-Setzen
  und Keycloak-Realm-Mapping bleiben Admin-SQL bzw. Realm-Setup.
- ~~KPI-UI (ECharts, Burn-Down, SLA-Ampel)~~ - Severity-Saeulen und
  SLA-Ampel ergaenzt in Iteration 55; Burn-Down war bereits vorhanden.
- ~~JGit-Adapter fuer Reachability (aktuell
  `NoopGitCheckoutAdapter`)~~ - **Teil erledigt in Iteration 71**
  (CVM-308). `JGitGitCheckoutAdapter` klont pro Commit in ein
  Cache-Verzeichnis, `@Scheduled`-Cleanup-Job entfernt abgelaufene
  Eintraege. HTTPS-Auth optional per
  `cvm.ai.reachability.git.https-token`. SSH-Credentials aus
  Vault/ssh-agent und Network-Sandboxing fuer den Subprocess
  bleiben offen. Ebenfalls offen: der
  `ReachabilityAutoTriggerPort`-Adapter, der
  `ReachabilityAgent.analyze(...)` mit dem JGit-Workdir aufruft.
- ~~Auto-Trigger der Reachability, wenn AI-Vorschlag-Confidence
  unter Schwelle~~ - **Erledigt in Iteration 70 + 77** (CVM-307,
  CVM-314). Neuer Event `LowConfidenceAiSuggestionEvent`, Port
  `ReachabilityAutoTriggerPort` + Service
  `ReachabilityAutoTriggerService` mit Schwellwert- und
  Rate-Limit-Logik. Echter Adapter
  `ReachabilityAutoTriggerAdapter` (`@Primary`) zieht `repoUrl`
  aus `Product` (Iteration 76), `commitSha` aus
  `ProductVersion.gitCommit` und Symbol/Language aus
  `PurlSymbolDeriver` und ruft `ReachabilityAgent.analyze`
  asynchron auf.
- Playwright-E2E + axe-core in CI, Karma in CI, Testcontainers-IT auf
  Docker-Desktop-macOS. *(Iteration 65 hat die Karma-Suite lokal
  wieder vollstaendig gruen bekommen - 91 Tests SUCCESS, keine "has
  no expectations"-Warnungen mehr. CI-Integration bleibt offen.)*
- Diverse Audit-/Cleanup-/Performance-Nachzuege (siehe 20260418).
