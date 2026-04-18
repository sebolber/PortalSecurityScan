# Offene Punkte (kumulativ)

## Stand 2026-04-19 nach Iteration 33 - offen

- Iteration 33 (CVM-77) liefert PURL->CVE-Matching ueber OSV.
  `CVM_OSV_ENABLED=true` aktiviert den Listener, SBOMs ohne
  `vulnerabilities`-Section fuehren dann zu echten Findings.
  Details: `docs/20260418/iteration-33-fortschritt.md`.
- Rate-Limit/Retry-After fuer OSV noch nicht umgesetzt.
- OSV-Mirror fuer air-gapped-Installationen offen (Iteration 34).
- PURL-Canonicalization ggf. noetig, falls Trefferquote in Prod
  zu gering.

> Iteration 32 am 2026-04-18 hat alle 15 Findings aus der
> UI-Exploration behoben (4 HIGH, 6 MEDIUM, 5 LOW).
> Details: `docs/20260418/iteration-32-fortschritt.md`.
> Frontend-Build gruen, Backend alle Module gruen. Karma-Run bleibt
> weiter Sandbox-geblockt (Chromium fehlt) - kein neuer Finding.

## Stand 2026-04-18 nach Iteration 32c - offen

- UI-Review 2 am 2026-04-18 hat 6 Neu-Findings ergeben (1 HIGH,
  4 MEDIUM, 1 LOW). Alle behoben in Iteration 32c
  (`docs/20260418/iteration-32c-fortschritt.md`).
- Auto-Report kategorisiert `/cves` und `/tenant-kpi` weiter als
  FEHLER (Konsolen-/API-Fehler, die Inhalt-Rendering aber nicht
  blockieren) - Naechster Schritt: Backend-Logs pruefen.
- Alte Backlog-Eintraege unveraendert (axe-core, Persistenz-IT).

## Stand 2026-04-18 nach Iteration 32b - offen

- ~~Material-Icons offline haerten~~ erledigt in 32b
  (`material-icons` + `@fontsource/fira-sans` self-hosted).
- ~~Karma-Sandbox-Chromium~~ erledigt in 32b (Puppeteer-Chromium +
  `karma.conf.cjs`). `npx ng test` liefert jetzt 68 SUCCESS in der
  Sandbox.

> Iteration 31 am 2026-04-18 hat `branding_config_history` (Flyway
> V0027, Entity + Repository), `BrandingService#rollbackForCurrentTenant`
> und die Admin-Endpunkte
> `GET /api/v1/admin/theme/history` und
> `POST /api/v1/admin/theme/rollback/{version}` ausgeliefert.
> Details: `docs/20260418/iteration-31-fortschritt.md`.

## Stand 2026-04-18 nach Iteration 31 - offen

- Frontend-UI fuer History-Liste + Rollback-Button in
  `AdminThemeComponent`.
- Persistenz-Integrationstest fuer V0027 bleibt Docker-pflichtig.

## Stand 2026-04-18 UI-Exploration - Findings aus Screenshot-Review (erledigt in Iteration 32)

Volltext: `docs/20260418/ui-exploration-findings.md`.

HIGH:
- HIGH-1 **Material-Icons** laden nicht (alle Screens zeigen
  2-3-Buchstaben-Fallbacks statt Icons).
- HIGH-2 **Top-Navigation-Overflow**: Rollen-Pills ueberlappen
  Logo + Mandanten-Switcher.
- HIGH-3 **Keycloak-CORS** blockiert `/realms/cvm-local/account`.
  Loest die 19 FEHLER im Auto-Report aus.
- HIGH-4 **NG0600** "Signal write in effect" auf `/queue`
  (`QueueStore.reload`).

MEDIUM:
- MEDIUM-1 Severity-Farben auf `/cves` werden grau statt token-farbig
  gerendert (Komponent verwendet nicht `<cvm-severity-badge>`).
- MEDIUM-2 `/profiles` hat keinen Empty-State und keinen Anlege-CTA.
- MEDIUM-3 `/admin/theme` behauptet "Rollback innerhalb 24 h" -
  Implementierung hat kein Zeit-Limit. Text oder Service anpassen.
- MEDIUM-4 `/admin/theme` hat keine Rollback-UI fuer die in
  Iteration 31 gelieferten Endpoints.
- MEDIUM-5 `/reports` verlangt UUIDs manuell statt Dropdown.
- MEDIUM-6 Seitentitel inkonsistent (H1 vs. kleiner Untertitel).

LOW: Siehe Findings-Datei (doppelte Form-Felder, fehlender
Preview-Swatch, Farbschema-Toggle-UX, Severity-Quicktoggles,
Rules-Empty-State).

> Iteration 30 am 2026-04-18 hat die Severity-Farbentscheidung
> formell unter `docs/konzept/severity-farbentscheidung.md`
> dokumentiert (Mapping, Begruendung, Kontrastnachweis,
> Aenderungspolitik). Details:
> `docs/20260418/iteration-30-fortschritt.md`.

## Stand 2026-04-18 nach Iteration 30 - offen

- Keine neuen Punkte. Backlog-Eintrag "Severity-Farbentscheidung
  formell dokumentieren" aus Iteration 27 ist damit erledigt.

> Iteration 29 am 2026-04-18 hat die Test-Bootstrap-Blocker behoben:
> Chart-Theme-Spec kompiliert wieder (TS4111), Persistenz- und
> Integrationstests skippen sauber, wenn Testcontainers die
> Docker-Umgebung nicht aushandeln kann. Details:
> `docs/20260418/iteration-29-fortschritt.md`.

## Stand 2026-04-18 nach Iteration 29 - offen

- **Karma-Lauf in CI**: Spec kompiliert jetzt, aber Headless-Chrome
  in CI bleibt offen (Umgebung).
- **Testcontainers auf Docker Desktop (macOS)**: lokaler Lauf
  skippt Slice-Tests, weil der Docker-Desktop-Socket von
  Testcontainers nicht direkt aushandelbar ist. Option:
  `TESTCONTAINERS_HOST_OVERRIDE` in Entwickler-Setup dokumentieren
  oder TCP-Daemon exponieren.

## Stand 2026-04-18 nach Iteration 28 - offen

- ~~**Karma-Suite blockiert**~~: in Iteration 29 behoben
  (Severity-Typisierung + tsc-Check).
- ~~Server-Multipart-Limit~~ erledigt: `spring.servlet.multipart.max-file-size=30MB`
  in `application.yaml` gesetzt, Client-Limit synchron bei 30 MB.
- **Produkt-/Profil-Edit und Soft-Delete** (eigene Iteration, Vier-Augen
  auf Profil-Delete).
- **Playwright-E2E `onboarding.e2e.ts`** fuer den durchgehenden Happy-Path
  Produkt → Profil → SBOM-Upload.

> Iteration 27 am 2026-04-18 hat den Token-Layer, die Branding-API
> (`/api/v1/theme`, `/api/v1/admin/theme`) und die Theme-Admin-UI
> (`/admin/theme`) ausgeliefert. Details:
> `docs/20260418/iteration-27-fortschritt.md`.

## Stand 2026-04-18 nach Iteration 27 - offen

- **Feature-Bereichs-Migration** auf `Ahs*`-Komponenten
  (Queue, Dashboard, CVEs, Komponenten, Profile, Rules, Reports,
  KI-Audit, Settings) - geplant als Iteration 27b.
- **Asset-Upload-Endpunkt** `POST /api/v1/admin/theme/assets`
  (Multipart, SVG-Sanitizer bereits vorhanden, Font-Upload mit
  woff2-Whitelist).
- **Stylelint-Guard**: harter CI-Check gegen hex-Farben und
  `font-family:` in Komponenten-SCSS - erst nach 27b aktivieren,
  sonst waere die Pipeline sofort rot.
- **FontAwesome-Thin**-Umstellung (Styleguide S. 34, Lizenz offen).
- **Mandanten-Platzhalter** (Waiver, Alert-Historie,
  Reachability, Fix-Verifikation, Anomalie-Board,
  Cross-Tenant-Dashboard) mit `<cvm-page-placeholder>` anbinden.
- ~~**Severity-Farbentscheidung** formell dokumentieren~~ erledigt
  in Iteration 30 (`docs/konzept/severity-farbentscheidung.md`).
- **FullNavigationWalkThroughTest + axe-core** in Playwright
  (Sandbox ohne Chromium).
- ~~**branding_config_history**-Tabelle fuer One-Click-Rollback~~
  erledigt in Iteration 31 (V0027, Endpunkte `history`/`rollback`).

## Stand 2026-04-18 nach Iteration 26 - offen

## Stand 2026-04-18 nach Iteration 26 - offen

- **CVE-Detailseite** mit verknuepften Findings und Assessments.
- **Criteria-basiertes Server-Paging** fuer CVE-Listen
  (Stream-Filter reicht bis wenige tausend CVEs).
- **Produkt-/Versions-Verwaltung** (Create/Update, aktuell nur
  Read - Anlage via SBOM-Import).
- **Rules-Editor im Frontend** (Create/Update Rule).
- **Profil-YAML-Editor** im Frontend (Monaco + Diff-Viewer).
- **Tenant-Verwaltungs-UI** (Liste/Create/Deaktivieren).

## Stand 2026-04-18 nach Iteration 24 - offen

- **Einzelseiten-Redesign** (Queue-Tabelle, Dashboard-Widgets,
  Profile-/Regel-Editor) - pro Feature separate Iteration.
- **Keycloak-Login-Theme** (adesso-CI-Skin auf Keycloak-Seite).
- **Bundle-Budget-Reduktion** (Initial > 1.05 MB, Alt-Last).
- **Playwright-E2E** fuer Theme-Toggle + rollenbasierte Navigation
  (Sandbox ohne Chromium).
- **i18n-EN-Texte** + echter Locale-Switch.
- **Karma-Lauf in CI** (Headless-Chrome verfuegbar machen).

## Stand 2026-04-18 nach Iteration 23 - offen

- **Security-Integrationstest** mit echtem JWT-Flow (Testcontainers
  oder Fake-Authorization-Server, der `realm_access.roles`-Claim
  liefert).
- **Tenant-Scope der Rollen** (z.&nbsp;B. `CVM_ADMIN@bkk-nord`).
  Aktuell flaches Modell.
- **Rollen-/Realm-Rollout** pro Tenant (Modell-Profil-Seed liegt
  beim Default-Tenant; BKK-spezifische Realms muessen eigene Rollen
  mitbringen).

## Stand 2026-04-18 nach Iteration 22 - Go-Live-Checkliste (Rest)

## Stand 2026-04-18 nach Iteration 22 - Go-Live-Checkliste (Rest)

**RLS-Hardening** (unveraendert offen, Testcontainers-pflichtig)
- RLS-Policies auf Sachtabellen (`assessment`, `scan`, `finding`,
  `waiver`, `ai_call_audit`, ...) mit
  `USING (tenant_id = current_setting('cvm.current_tenant')::uuid)`
  in einer eigenen Migration nachziehen.
- `@Transactional`-Aspekt, der `SET LOCAL cvm.current_tenant`
  aus dem `TenantContext` pusht.
- ArchUnit-Test: kein Repository-Call ohne Tenant-Kontext.
- `RlsIsolationTest` (Testcontainers): Tenant A sieht Tenant B
  nicht - auch bei bewusst fehlerhafter Query.

**KPI-UI**
- ECharts-Widgets im Dashboard: Burn-Down-Linie, Severity-Saeulen,
  Ampel fuer SLA-Quote.
- Executive-Report: CSV-Anhang einbinden (aktuell nur separater
  Download).
- `kpi_snapshot_daily` im `KpiService` als Burn-Down-Quelle
  lesen (Schreiben steht, Lesen noch nicht verdrahtet).

**Tenant-Rollout** (Rest)
- Migrations-Script fuer `tenant_id`-Spalte auf allen Sach-
  tabellen (mit Default-Tenant aus V0023).
- Scope-Iterator fuer den KPI-Snapshot-Job: aktuell nur ein
  globaler Snapshot. Sobald eine Listung aktiver
  (productVersion, environment)-Paare vorhanden ist, Scope-
  Snapshots im gleichen Cron anhaengen.

## Stand 2026-04-18 nach Iteration 22 - erledigt

- **Gate-Integration MR-Kommentar**: `GitProviderPort
  .postMergeRequestComment` + `PipelineGateEvaluatedEvent`
  + `PipelineGateMrCommentListener`. Aktivierung via
  `cvm.pipeline.gate.post-mr-comment=true`.
- **JWT-Tenant-Resolver scharf**: `TenantContextFilter` setzt den
  Tenant aus dem Claim `tenant_key` (Fallback `tid`) und raeumt
  den Context wieder auf.
- **Modell-Profil-Seed**: `V0023__model_profile_seed.sql` legt
  Default-Tenant und die beiden Profile (Claude Cloud, Ollama
  Fallback) idempotent an.
- **`kpi_snapshot_daily`**: Tabelle (V0024), Entity,
  Repository, `KpiSnapshotWriter` und `KpiDailySnapshotJob`
  (Cron 01:00) sind live.

## Stand 2026-04-18 nach Iteration 20
- **VEX-Import-Ingestion**: Statements werden aktuell nur
  geparst + validiert. Das Anlegen von AssessmentProposals
  (inkl. Merge-Strategie bei bestehenden Bewertungen) folgt
  in einer eigenen Iteration.
- **Waiver-UI** (Angular): Formular fuer Grant/Extend/Revoke
  + Liste EXPIRING_SOON im Admin-Bereich.
- **Waiver-Mails**: aktuell feuern Alerts ueber
  `AlertEvaluator`, aber es gibt noch keinen dedizierten
  E-Mail-Versand bei Ablauf (Design-Entscheidung: Mail nur
  via regulaeren Alert-Dispatcher).
- **SPDX-VEX-Export**: laut Plan ausserhalb des Scopes;
  falls noetig, in eigener Iteration nachziehen.
- **Goldmaster-Test** fuer VEX-Byte-Gleichheit ueber mehrere
  Laeufe im CI (aktuell nur innerhalb eines Testlaufs).

## Stand 2026-04-18 nach Iteration 19
- **Delta-Historie** im Executive-Report: Werte aktuell auf 0
  gesetzt; sobald Executive-Reports archiviert werden,
  berechnen gegen den letzten.
- **UI** fuer NL-Query-Eingabezeile und Executive-Download-Button.
- **JPA-Criteria-Filter** statt Stream-Filter bei grossen
  Assessment-Mengen.
- **Goldmaster-PDF** fuer Board-Report (aktuell nur Text-Marker-Tests).
- **Plattform-Lead-Freigabe** vor Board-PDF (Konzept-Wunsch, noch
  nicht verdrahtet).
- **HTML-Entities in openhtmltopdf-Templates**: nur numerische
  Entities (`&#183;`) verwenden, nicht named (`&middot;`).

## Stand 2026-04-18 nach Iteration 18
- **LLM-Zweitstufe fuer Anomalien** (`anomaly-review.st`): Prompt
  vorbereitet, Service-Integration folgt bei realem Datenverkehr
  (Flag `cvm.ai.anomaly.use-llm-second-stage`).
- **Anomalie-Widget im Dashboard** + Anomalie-Liste im Frontend.
- **Wizard-UI** im Profil-Bereich (3 Endpunkte stehen).
- **RAG-Embedding fuer Muster 3** (SIMILAR_TO_REJECTED) statt
  Jaccard-Heuristik.
- **Session-Cleanup-Cron**: EXPIRED/FINALIZED-Sessions aus
  `profile_assist_session` aufraeumen.
- **Profil-Draft-YAML** an das echte Profil-Schema mappen (aktuell
  reine `answers:`-Liste).

## Stand 2026-04-18 nach Iteration 17
- **RAG-Embedding-Clustering** statt rein Feature-basierter
  Cluster-Keys. Nutzt die Retrieval-Pipeline aus Iteration 12.
- **Rule-Id am Assessment**: Fuer praezise Override-Tracker-Logik
  muss das Assessment wissen, welche Rule es erzeugt hat. Aktuell
  Approximation ueber SUPERSEDED-RULE-Vorgaenger.
- **Override-Tracker als @Scheduled**: aktuell nur `evaluate()`.
  Cron-Hook folgt, sobald Heuristik stabil ist.
- **Testcontainers-Integrationstest**: 180 synthetische Assessments
  -&gt; Nightly-Lauf -&gt; Vorschlag in Queue.
- **UI-Tab "Vorschlaege"** im Admin-Bereich (Regeln-Editor aus
  Iteration 05 erweitert).

## Stand 2026-04-18 nach Iteration 16
- **Watchdog-Mapping `cveKey -> repoUrl`**: aktuell Map aus
  Admin-/Test-Input. Fuer automatischen Tageslauf braucht es einen
  persistenten Ort (Tabelle oder ProduktVersion-Metadaten).
- **Audit-Id aus `AiCallAuditService.execute(...)`**: Orchestratoren
  (AutoAssessment, FixVerification) behelfen sich mit "letzter OK-
  Audit des Use-Cases". Ein `AuditedLlmResponse(response, auditId)`
  waere sauberer.
- **Watchdog-Audit-Lightweight**: aktuell synthetischer
  `ai_call_audit`-Eintrag. Eigene `ai_subprocess_audit`-Tabelle
  oder `ai_call_audit_id` nullable waere semantisch sauberer.
- **UI-Badge fuer Grade A/B/C** in der Queue-Detail-Ansicht.
- **Cache-Eviction im FixVerificationService**: aktuell kein
  Cleanup, Speicher waechst bis JVM-Restart.
- **GitLab-Provider**: Port ist abstrakt, nur GitHub implementiert.

## Stand 2026-04-18 - Frontend-Nachzug Iterationen 10/11
- **Reports-Listing-Endpoint** im Backend fehlt: das Frontend
  zeigt aktuell nur die in der laufenden Sitzung erzeugten Reports.
  Folge-Iteration sollte `GET /api/v1/reports?productVersionId=&environmentId=`
  liefern (paginiert, neueste zuerst), damit beim Reload die Historie
  zurueckkommt.
- **Produkt-/Umgebungs-Dropdowns** in der Reports-Form: derzeit
  UUID-Eingabefelder, weil ProductCatalog-Read-Endpoint noch fehlt
  (offen aus Iteration 08).
- **Erweiterte Filter im AI-Audit**: heute nur Status + Use-Case;
  triggeredBy / Datumsbereich / Kosten-Threshold koennen ergaenzt werden.
- **Drill-Down "Audit -> Suggestion -> Source-Refs"**: Audit-Liste
  verlinkt nicht auf den dazugehoerigen `ai_suggestion`. Sobald die
  Queue-Detail-UI ausgebaut wird, hier verlinken.

## Stand 2026-04-18 nach Iteration 15
- **JGit-Adapter** + Cache pro Commit + SSH-Key aus Vault. Aktuell
  liefert `NoopGitCheckoutAdapter` nur ein Tmp-Verzeichnis.
- **UI-Trigger fuer Reachability**: Button in Queue-Detail mit
  Rate-Limit pro Bewerter.
- **Network-Sandboxing fuer Subprocess**: Konzept verlangt
  Network-Namespace/Seccomp; Implementierung lebt auf Deployment-
  Ebene (OpenShift `NetworkPolicy`). Dokumentation in
  `docs/konzept/llm-gateway-invarianten.md` ergaenzen, sobald
  Vault-Adapter da ist.
- **Auto-Trigger der Reachability** wenn AI-Vorschlag-Confidence
  unter Schwelle.
- **JGit-Cache-Wartung**: alte Working-Copies aufraeumen, sobald
  echte Checkouts laufen.

## Stand 2026-04-18 nach Iteration 14
- **Auto-Trigger der DeltaSummary**: Listener auf
  `ScanIngestedEvent` (rufe `summarize(...)` automatisch nach
  Cascade-Run). Aktuell nur on-demand via REST-GET.
- **Streaming-UI**: Angular-Komponente fuer den NDJSON-Copilot-
  Stream (zeilenweise Token-Anzeige) fehlt. Backend-Vertrag steht.
- **Slack-Webhook fuer Delta-Summary**: kurzes Snippet kann jetzt
  ueber REST abgerufen werden - automatischer Webhook-Versand
  steht aus.
- **Cost-Cap pro Umgebung**: nur globaler Rate-Limiter heute;
  Kostenobergrenze pro Tenant noch nicht.
- **"Vorschlag uebernehmen"-Endpoint** fuer Copilot-Texte: aktuell
  uebernimmt das Frontend selbst per PUT auf das Assessment.
- **DeltaSummary-Persistenz**: Summary wird derzeit nicht
  archiviert (kein `scan_delta_summary`-Eintrag). Sobald Audit
  oder Audit-Trail eine Pflicht wird, hier ergaenzen.

## Stand 2026-04-18 nach Iteration 13
- **Audit-Id-Rueckgabe vom AiCallAuditService**: aktuell liefert
  `execute(...)` nur den `LlmResponse`. Der Orchestrator findet die
  Audit-Id ueber Repository-Lookup (letzter OK-Eintrag mit
  useCase=AUTO_ASSESSMENT). Sauberer waere ein
  `AuditedLlmResponse(response, auditId)`.
- **Batch-/Bucket-Verarbeitung pro Scan**: aktuell sequenziell. Konzept-
  Vorgabe (10 parallel + Bucket4j) bleibt offen bis Performance-
  Iteration.
- **Halluzinations-Check**: nur `proposedFixVersion` geprueft. URL-,
  CWE- und Sources-Konsistenz koennte folgen.
- **Mandanten-Filter im RAG-Lookup**: aktuell ohne Filter; hier
  ergaenzen, sobald Mandanten-Schluessel im Embedding stehen.
- **`AssessmentApprovedRagListener`** indiziert nur APPROVED-Eintraege
  - die KI-Vorschlaege selbst werden noch nicht indiziert. Sobald
  Iteration 14 (Delta-Summary) kommt, sollte das aufgenommen werden.

## Stand 2026-04-18 nach Iteration 12
- **Embedding-Calls ohne Audit**: `IndexingService` ruft den
  `EmbeddingClient` derzeit direkt; wenn der Embedding-Pfad
  produktiv wird, sollte er ueber den `AiCallAuditService` mit
  `useCase=EMBEDDING` laufen.
- **`indexAll()` ohne Pageable**: voller `findAll()` ueber
  Assessments und CVEs. Streaming-Variante einbauen, sobald die
  Datenmenge ueber ein paar tausend hinaus geht.
- **Anthropic-Embedding-Adapter**: erst sobald die offizielle API
  raus aus Beta ist.
- **pgvector-Integrationstest** (Testcontainers + ivfflat) bleibt
  Docker-skipped.
- **Locale-Vorbehandlung** (Umlaute, Stop-Words) optional - aktuell
  nur Roh-String an Modell.
- **`ai_call_audit`-Eintrag fuer Re-Index-Endpunkt**: Der Admin-
  Re-Index ist bisher nicht audit-pflichtig (kein KI-Call im engen
  Sinne, aber ggf. relevant fuer Compliance-Logging).

## Stand 2026-04-18 nach Iteration 11
- **Resilience4j** um `ClaudeApiClient` und `OllamaClient` fehlt noch
  (Retry, Circuit-Breaker, Timeout-Feintuning). Sollte nachgezogen
  werden, sobald das Feature-Flag in einer Umgebung real auf
  `true` steht.
- **Rolle `AI_AUDITOR`** + Lese-Endpunkt fuer `ai_call_audit` sind
  nicht implementiert. RLS/Query-Filter folgt mit der Security-
  Iteration.
- **`modelVersion`** wird aktuell nicht vom Adapter ausgelesen (die
  Anthropic-API liefert sie nicht explizit). Audit-Spalte bleibt
  `null`.
- **Postgres-Trigger fuer Audit-Immutability**: Aktuell nur
  JPA-Listener. Fuer harte DB-seitige Sperre braucht es einen Trigger.
- **Persistenz-Integrationstest fuer V0013** (ai_call_audit-Tabelle)
  bleibt Docker-skipped bis CI Postgres anbietet.
- **Prompt-Template-Versionsverwaltung**: Aktuell liegt die Version
  im Template-Quelltext. Ein Review-/Freigabe-Workflow analog zu
  Kontextprofil fehlt.

## Stand 2026-04-18 nach Iteration 10
- **Query-Effizienz**: `HardeningReportDataLoader` faellt fuer die
  Liste aller aktiven Assessments einer (ProduktVersion, Umgebung)
  auf `findAll()` + Stream-Filter zurueck, weil das
  `AssessmentRepository` noch keine passende Methode hat
  (`findActiveByProductVersionAndEnvironment`). Fuer mittlere
  Datenmengen OK, fuer 5-stellige CVE-Listen ineffizient. Sollte
  parallel zu Iteration 13 (KI-Clustering) eine dedizierte Query
  bekommen.
- **Encryption-at-rest fuer PDF-Bytes**: Spalte ist `BYTEA`, Integritaet
  via SHA-256-Hash abgesichert. Jasypt-Binding folgt, sobald der
  Vault-Key in OpenShift hinterlegt ist.
- **Goldmaster-PDF als Artefakt**: Iteration 10 verifiziert
  Determinismus ueber Live-Vergleich. Ein reales Golden-PDF unter
  `cvm-application/src/test/resources/reports/hardening-golden.pdf`
  waere schoener, sobald die Layout-Abstimmung mit dem adesso-CI
  abgeschlossen ist (aktuell Platzhalter-Logo / Default-Font).
- **Async-Generierung**: Reports rendern in < 100 ms. Sollte ein
  Mandant grosse Scopes (> 10&nbsp;000 CVEs) exportieren, braucht es
  einen Async-Job inkl. Status-Polling-Endpoint.
- **Security-Rollen** (`CVM_REPORTER`, `CVM_APPROVER`) fuer
  `/api/v1/reports/*` sind noch nicht verdrahtet; aktueller Guard ist
  `authenticated()` ueber die Default-WebSecurity. Folgt mit den
  Role-Iterationen ab 11.
- **Persistenz-Integrationstest fuer V0012**: Docker-skipped bis CI
  Postgres anbietet.
- **VEX-Anhang**: Bleibt Platzhalter bis Iteration 20 (VEX + Waiver).
- **Gesamteinstufungs-Vorstufe**: Der Konzept-Prompt beschreibt einen
  Checkbox-Endpunkt vor der PDF-Erzeugung. Umgesetzt direkt als
  Request-Feld `gesamteinstufung`; UI-Schritt (Vorauswahl + manuelle
  Bestaetigung) folgt mit der Frontend-Erweiterung.

## Stand 2026-04-18 nach Iteration 09
- **AssessmentExpiredEvent** noch nicht eingefuehrt. Re-Vorschlag-
  Pfad sollte parallel zu `AlertEscalationJob` laufen, damit
  abgelaufene Approves wieder als PROPOSED in der Queue landen.
- **AlertRule-YAML-Bootstrap** fehlt; aktuell nur REST-Anlage.
  Ein YAML-Loader (analog zu Profile/Rule) folgt mit Iteration 21.
- **Persistenz-Integrationstest** fuer V0011 bleibt Docker-skipped.
- **Manueller MailHog-Smoke**: `cvm.alerts.mode=real` mit
  `CVM_SMTP_HOST=localhost`, `_PORT=1025` in einem Hands-On-Lauf
  pruefen, sobald Docker lokal verfuegbar ist.
- **ESKALATION_PENDING_REVIEW**: separater Trigger fuer
  Vier-Augen-Pending-Faelle ist nicht enthalten. Iteration 13/14
  kann das ergaenzen, sobald Cluster-Cases auflaufen.
- **Banner i18n**: Banner-Text aktuell hart deutsch.
- **AlertConfig.dryRun()** ist global; kein Per-Rule-Override.
  Reicht fuer Iteration 09; Per-Rule-Switch waere ein Feature.

## Stand 2026-04-17 nach Iteration 08
- **Produkt-/Umgebungs-Dropdowns**: In der Queue-Seite sind Produkt-
  version und Umgebung aktuell UUID-Freitextfelder. Sobald die
  Produkt-/Versions-Read-Endpunkte existieren (geplant in Iteration 19),
  werden Dropdowns mit LocalStorage-Persistenz aktiviert.
- **Bulk-Approve-Endpoint**: UI-seitig ist die Checkbox-Batch-Auswahl
  vorbereitet. Ein Server-seitiger Bulk-Endpoint ist nicht im Scope
  von Iteration 08 (laut Prompt "UI vorbereitet"); Iteration 13
  (KI-Clustering) macht einen Bulk-Approve fachlich relevant.
- **Playwright-E2E fuer die Queue**: Sandbox hat kein Chromium. Die
  Szenarien aus 08-Bewertungs-Queue-UI.md (Happy-Path, Override,
  Reject, Vier-Augen, Konflikt) sind zu implementieren, sobald das
  E2E-Setup steht (siehe schon bestehender Backlog-Punkt).
- **`ng test`-Lauf**: Karma-Specs sind geschrieben und kompilieren;
  die Ausfuehrung haengt an Headless-Chrome in CI.
- **Bundle-Budget-Warnung** (Initial > 1.05 MB) besteht weiter.
  Kommt aus keycloak-js + echarts; Option ist weiteres Lazy-Loading
  des Dashboards oder ein separater Login-Chunk. Nicht dringend.

## Stand 2026-04-17 nach Iteration 00

### Blockierend fuer spaetere Iterationen
- Keine. Alle DoD-Punkte aus Iteration 00 sind abgebildet, soweit sie in
  einer Sandbox ohne Docker verifizierbar sind.

### Nachzureichen durch Sebastian
- **SonarCloud**: `sonar.projectKey` und `sonar.organization` in
  `pom.xml` (Root) durch reale Werte ersetzen.
- **Fachkonzept v0.2**: Den Platzhalter unter
  `docs/konzept/CVE-Relevance-Manager-Konzept-v0.2.md` durch den
  Originaltext ersetzen.
- **Prompt-Korrektur**: `CREATE EXTENSION "pgvector"` in
  `00-Initialisierung.md` (Abschnitt 3.3) auf `"vector"` aendern.

### Mittelfristig
- **Checkstyle-Konfiguration** ist als Plugin hinterlegt, aber ohne
  `checkstyle.xml`. Ergaenzen, sobald die Codebasis nicht mehr trivial ist.
- **Pre-Commit-Hooks**: `.pre-commit-config.yaml` fehlt; bei Einfuehrung
  auch Spotless und ESLint an den Hook hangen.
- **ArchUnit**: `archRule.failOnEmptyShould=false` in Iteration 01
  zurueckrollen, sobald Domain-Klassen existieren.

### Langfristig (Iteration 21)
- Echtes Container-Image-Build plus Sign/Push im GitLab-CI-Stage
  `package` (aktuell Stub).
- Trivy-Scan des eigenen Images in Stage `package` verdrahten.

### Stand nach Iteration 07
- **Karma-Tests** koennen lokal nur mit Headless-Chrome laufen
  (Sandbox-Build: `No binary for ChromeHeadless`). CI muss
  `karma-chrome-launcher` + `ChromeHeadlessNoSandbox` mitbringen.
  Specs (`RoleMenuServiceTest`, `AppConfigServiceTest`,
  `AuthInterceptorTest`, `ApiClientTest`) sind geschrieben und
  kompilieren sauber via `ng build`.
- **Playwright-E2E-Smoke** (Login-Redirect, Dashboard, Logout) noch
  nicht eingerichtet. Spec aus 07-Frontend-Shell.md nennt es
  "nice-to-have"; sollte mit Iteration 08 nachgereicht werden.
- **`@typescript-eslint/parser`** in Iteration 00 vergessen worden.
  Iteration 07 hat ihn als devDependency hinzugefuegt; package.json
  jetzt komplett, `ng lint` ist gruen.
- **Tailwind-Severity-Plugin**: Severity-Farben sind aktuell hart in
  `severity-badge.component.ts` (Tailwind-Klassen-Map). Sobald die
  Queue-Tabelle die gleichen Farben braucht, lohnt sich ein Tailwind-
  Plugin (`severity-bg-{level}`).
- **Login-Modus**: aktuell `check-sso` (kein Hard-Redirect bei
  Anwendung-Start). Falls PortalCore-typisch `login-required`
  gewuenscht ist, muss `keycloak-init.ts` umgestellt werden.
- **Produkt-/Umgebungs-Auswahl im Header**: aktuell statische Optionen
  ohne LocalStorage-Persistenz; Iteration 08 sollte echte Produkt-Daten
  aus dem Backend laden und die Auswahl persistieren.
- **i18n**: Locale-Service vorbereitet auf `en`, aber englische Texte
  fehlen.

### Stand nach Iteration 06
- **Security**: Rollen `CVE_APPROVER`, `CVE_REVIEWER` noch nicht
  verdrahtet. Endpunkte unter `/api/v1/assessments` und
  `/api/v1/findings` laufen aktuell mit `authenticated()`. Vier-Augen ist
  fachlich (UserId-Vergleich) bereits durchgesetzt; rollenbasierte
  Pruefung folgt zusammen mit den Profile-/Rule-Rollen.
- **Mitigation-Plan-Status**: Auto-Anlage als `PLANNED`, Owner =
  Approver. Status `OPEN` waere fachlich sauberer; bleibt UI-Aufgabe in
  Iteration 08.
- **Severity-/Umgebungs-spezifische `validUntil`-Konfiguration**: aktuell
  ein einziger Default `cvm.assessment.default-valid-months=12`. Sobald
  Konzept v0.2 (6.2 Punkt 5) konkretisiert wird, kann pro
  Severity/Umgebung gesteuert werden.
- **AssessmentExpiredEvent**: nicht publiziert. Sollte spaetestens in
  Iteration 09 (SMTP) ergaenzt werden, damit Re-Vorschlaege auf der UI
  sichtbar sind.
- **REUSE und AssessmentApprovedEvent**: REUSE-Fortschreibung publiziert
  derzeit kein Event, da fachlich keine neue Bewertung. Falls Alerts
  trotzdem sinnvoll sind &rarr; Iteration 09 entscheiden.
- **JsonNode-Profile in der Cascade**: Listener parst die YAML-Quelle
  jedes Mal frisch, ohne Schema-Validierung. Profile-at-point-in-time
  greift erst, wenn Iteration 19 die Profil-Zeitreihen einbezieht.
- **Persistenz-Integration-Test fuer V0010** (`valid_until`,
  `reviewed_by`, EXPIRED-Constraint): in CI noch zu ergaenzen, sobald
  Docker verfuegbar.
- **Pitest fuer Severity-Mapping und Cascade-Logik**: Konzept-Vorgabe
  ist 100 % Mutation Survival; bleibt offen bis ein eigener Mutation-
  Run-Schritt ergaenzt wird.
- **CVE_APPROVER**-Rolle: Eintragung in Keycloak-Realm + Zuordnung im
  WebSecurityConfig folgt mit Iteration 11.

### Stand nach Iteration 05
- **Condition-JSON ist TEXT**, nicht JSONB. Fuer Server-seitige
  JSON-Queries (z.&nbsp;B. "welche Regel referenziert Pfad X") muesste
  die Spalte spaeter auf JSONB migriert werden. Aktuell kein Bedarf.
- **Dry-Run-Profilhistorie**: nimmt das heute aktive Profil je Umgebung.
  Sobald Profil-Zeitreihen relevant werden (&rarr; z.&nbsp;B. fuer
  Executive-Reports in Iteration 19), Profil-at-point-in-time anbinden.
- **Regel-Immutable-Versionierung**: derzeit genuegt der Status-Zyklus
  DRAFT &rarr; ACTIVE &rarr; RETIRED. Wenn Regeln inhaltlich geaendert
  werden sollen, sollte analog zu Assessment/Profil eine Zeile je
  Version angelegt werden. Offen bis Iteration 17 (KI-Regel-Extraktion).
- **Security**: Rolle `CVM_ADMIN` bewacht POST/activate/dry-run. Feinere
  Aufteilung (z.&nbsp;B. `CVM_RULE_AUTHOR`, `CVM_RULE_APPROVER`) noch
  offen.

### Stand nach Iteration 04
- **Security** fuer die Profile-Endpunkte nachziehen: Rolle
  `CVM_PROFILE_AUTHOR` fuer PUT, `CVM_PROFILE_APPROVER` fuer
  POST `/approve`. Aktuell geschuetzt nur durch den Default-
  `authenticated()`-Guard.
- **Integrationstest** fuer die Flyway-Migration V0008 und den
  End-to-End-Flow (propose &rarr; approve &rarr; NEEDS_REVIEW) mit
  Docker ergaenzen, sobald die CI Docker hat.
- **Profil-Schema v2**: sobald Iteration 05 (Regel-Engine) weitere Felder
  braucht (z.&nbsp;B. `customer_segment`, `data_classification`), Schema
  migrieren und `schemaVersion` hochziehen.
- Entscheidung: wer darf einen Draft **verwerfen**? Aktuell existiert
  kein REJECTED-State &mdash; einfacher Cleanup ueber DB-Delete, da DRAFTs
  nicht referenziert werden.
- Der `AssessmentImmutabilityListener`-Bypass laeuft via
  `@Modifying`-Query. Iteration 06 kann ein explizites Audit-Event
  ergaenzen, sobald der Bewertungs-Workflow steht.

### Stand nach Iteration 03
- Resilience4j (Retry/Circuit-Breaker) nachziehen, vorzugsweise parallel
  zu Iteration 11 (LLM-Gateway).
- GHSA-Token in Vault hinterlegen, damit der Feed in Prod aktiv wird.
- KEV-CSV-Variante nur bei Bedarf nachruesten.
- Scheduled-Jobs benoetigen Cluster-Koordination (z.B. ShedLock), sobald
  mehr als eine Instanz laeuft. Jetzt nicht, aber Backlog merken.

### Stand nach Iteration 02
- Performance-Smoke-Test mit 10 000 Komponenten nachreichen
  (`@Tag("perf")`), sobald Docker-Ressourcen in CI verfuegbar.
- Dedup-Scope: Triple `(product_version_id, environment_id,
  content_sha256)` bestaetigen.
- `cvm.encryption.sbom-secret` in Vault einbinden (aktuell Default in
  `application.yaml`).
- `ScanIngestedEvent`-Listener in Iteration 05/06 verdrahten.

### Stand nach Iteration 01
- Entscheidung: Soll Assessment ueber Record/Value-Object den
  Immutability-Vertrag stark typisieren statt ueber `updatable=false`
  plus Listener?
- Entscheidung: `@angular/flex-layout` im Frontend entfernen
  (Material-Grid reicht)?
- ArchUnit: `failOnEmptyShould=true` aktivieren, sobald
  `com.ahs.cvm.ai`, `com.ahs.cvm.llm`, `com.ahs.cvm.api` befuellt sind
  (spaetestens Iteration 11).
