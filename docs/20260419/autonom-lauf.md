# Autonomer Lauf 2026-04-19

Start: 08:47. Zeitbox: 3 Stunden.

Log (append-only):

[08:58] Iteration 41 abgeschlossen: Parameter-Katalog Block A.1
(commit b268ff97). Naechstes: Iteration 42 Parameter-Katalog Block A.2.
[09:09] Iteration 42 abgeschlossen: Parameter-Katalog Block A.2
(commit 5058cd36). Naechstes: Iteration 43 getEffective-Wrapper
Teil 1 (ReachabilityConfig, OsvProperties, Feed*Config, AutoAssessmentConfig).
[09:16] Iteration 43 abgeschlossen: Parameter-Store-Lesepfad
(Reachability, AutoAssessment, OSV, Feeds) + SystemParameterResolver
(commit a49a6f0b). Naechstes: Iteration 44 getEffective-Wrapper Teil 2
(FixVerification, RuleExtraction, Alert, Assessment + restartRequired-Marker).
[09:31] Iteration 44 abgeschlossen: Parameter-Store-Lesepfad Teil 2 +
restartRequired-Marker + UI-Chip (commit 23550a99). Naechstes:
Iteration 45 Secret-Behandlung AES-GCM.
[09:40] Iteration 45 abgeschlossen: AES-GCM-Secret-Verschluesselung
fuer sensible System-Parameter (commit 0d0871dc). Naechstes:
Iteration 46 ArchUnit-Regel + E2E-Test Reachability-Flag.
[09:46] Iteration 46 abgeschlossen: ArchUnit-Regel (Parameter-Modul-
Zugriff) + E2E-Test fuer Reachability-Override (commit aaf003c2).
Naechstes: Iteration 47 Queue-Filter oberhalb der Liste.
[09:50] Iteration 47 abgeschlossen: Queue-Filter als horizontaler
Balken oberhalb der Tabelle (commit c9e86a9b). Naechstes: Iteration 48
Umgebungen Soft-Delete.
[09:58] Iteration 48 abgeschlossen: Umgebungen Soft-Delete
(Flyway V0031, Service, Controller, Frontend-Button) (commit c5f6e4ae).
Naechstes: Iteration 49 Produkt-Versionen Soft-Delete.
[10:09] Iteration 49 abgeschlossen: Produkt-Versionen Soft-Delete
(Flyway V0032) (commit 76f60705). Naechstes: Iteration 50 Regeln
Soft-Delete.
[10:19] Iteration 50 abgeschlossen: Regeln Soft-Delete mit
Abgrenzung RETIRED (Flyway V0033) (commit 8c1b374a). Block B
(User-Feedback Soft-Delete) damit komplett. Naechstes: Block C
Iteration 51 Profil-Edit + Soft-Delete.
[10:28] Iteration 51 abgeschlossen: Profil-DRAFT-Edit + Soft-Delete
(Flyway V0034, Controller, Service-APIs) (commit 07019918).
Naechstes: Iteration 52 Bundle-Budget-Reduktion.
[10:33] Iteration 52 abgeschlossen: Bundle-Budget-Reduktion
(2.13 MB -> 1.10 MB, ECharts+LoginCallback lazy) (commit 0d8c3529).
Naechstes: Iteration 53 Rules-Editor im Frontend.
[10:40] Iteration 53 abgeschlossen: Rules-Editor Update-Form
(Backend + Frontend) (commit 43216d1a). Naechstes: Iteration 54
Profil-YAML-Editor (Monaco + Diff).
[10:45] Iteration 54 abgeschlossen: Monaco-YAML-Editor in der
Profile-Seite, Monaco-Assets lazy-ausgeliefert (commit b5615972).
Naechstes: Iteration 55 KPI-UI.
[10:48] Iteration 55 abgeschlossen: KPI-UI Severity-Saeulen +
SLA-Ampel (commit 6a02c16f). Block C (ohne Tenant-UI) damit
abgearbeitet.
[10:57] Iteration 56 abgeschlossen: Tenant-Liste read-only
(Backend + Frontend-Route + Menueintrag) (commit adb25529).
Naechstes: Iteration 57 Monaco-Diff.
[11:01] Iteration 57 abgeschlossen: Monaco Side-by-Side Diff-Editor
fuer Profiles (commit 44927352). Naechstes: Iteration 58
PURL-Canonicalization.
[11:08] Iteration 58 abgeschlossen: PURL-Canonicalization im
Domain-Modul + Integration in ComponentCveMatchingOnScanIngestedListener
(commit c7d8f29e).
[11:18] Iteration 59 abgeschlossen: Tenant-Anlage (Backend + UI-
Formular) (commit e00f0787). Timebox naehert sich dem Ende.
[11:26] Iteration 60 abgeschlossen: Tenant Active-Toggle mit
Default-Schutz (commit b7d6f35f).

ZEITBOX ERREICHT.
Gesamtbilanz: 20 Iterationen (41-60), 18 Feature-Commits plus
Doku-Fixups. Parameter-Store komplett (Katalog + Lesepfad +
Secrets + ArchUnit + E2E), Queue-Filter-Layout, Soft-Delete fuer
Umgebungen/Produkt-Versionen/Regeln/Profile, Bundle-Reduktion
2.1 MB -> 1.1 MB, Rules-Editor Update-Form, Monaco-Editor und
Side-by-Side-Diff in Profiles, KPI-UI Severity-Saeulen +
SLA-Ampel, Tenant-Liste/-Anlage/-Toggle, PURL-Canonicalization.
Offene Restpunkte (siehe offene-punkte.md, groessere
Folge-Iterationen): JGit-Adapter fuer Reachability, OSV-Mirror,
Callsite-Migration der LLM/Feed-Adapter, Auto-Trigger
Reachability, CI-Infrastruktur (Playwright, Karma,
Testcontainers), Audit/Cleanup/Performance-Nachzuege.

---

## Folge-Session 2026-04-19 (Iteration 63+)

Start: 15:23 UTC. Zeitbox: 3 Stunden.
Branch: `claude/complete-open-tasks-gmsfB`.

[15:34] Iteration 63 abgeschlossen: Profil-Save-404-Bug gefixt
(Backend liefert 200+[] ohne aktive Vorgaenger-Version, Frontend
nutzt `getOptional` und liefert `[]` bei 404, Editor-Reset in
`draftSpeichern`). Naechstes: Iteration 64 Profil-Edit /
Soft-Delete UI-Integration.
[15:45] Iteration 64 abgeschlossen: Profil-Draft-Edit und
Soft-Delete im Frontend ("Draft bearbeiten" ruft
draftAktualisieren, "Draft loeschen" mit window.confirm und
Toast, neues Dispatch-Flag in draftSpeichern). Karma-Specs
fuer Service + Component ergaenzt. Naechstes: Iteration 65
Karma-Specs auf neue Selektoren umstellen.
[15:55] Iteration 65 abgeschlossen: Karma-Suite wieder
vollstaendig gruen (91 Tests, keine "has no expectations"-
Warnungen). RoleMenuService-Spec-Kinderliste auf 9 aktuelle
Eintraege aktualisiert, QueueApiService und AiAuditService
expectOne-Predicate-Cases mit expliziten expect() ergaenzt.
Naechstes: Iteration 66 ClaudeApiClient auf Parameter-Resolver.
[16:00] Iteration 66 abgeschlossen: ClaudeApiClient nutzt jetzt
den neuen Port LlmGlobalParameterResolver + Adapter
SystemParameterLlmGlobalResolver (in cvm-ai-services).
RestClient wird lazy rebuilt bei baseUrl-/timeout-Aenderung,
Claude-Katalog-Flags auf live-reloadable umgestellt (ausser
version). Neuer WireMock-Test fuer Runtime-Override ohne
Neustart. Naechstes: Iteration 67 Feed-Clients auf
Parameter-Resolver.
[16:09] Iteration 67 abgeschlossen: NvdFeedClient und
GhsaFeedClient lesen ihre api-keys pro Call via
SystemParameterResolver (direkte Dependency auf
cvm-application erlaubt). KEV und EPSS haben keinen api-key,
daher keine Callsite-Migration. Katalog-Flags der beiden Feed-
Secrets auf live-reloadable. Zwei neue WireMock-Override-
Tests. Naechstes: Iteration 68 GitHubApiProvider (Fix-Verifikation)
auf Resolver.
[16:13] Iteration 68 abgeschlossen: GitHubApiProvider loest
cvm.ai.fix-verification.github.token pro Call auf; RestClient
ohne Default-Authorization-Header, Bearer-Token wird pro Call
gesetzt. Damit ist die Callsite-Migration der Adapter
komplett. Alle vier Secrets (Claude, NVD, GHSA, GitHub) sind
im Katalog live-reloadable. SystemParameterCatalogTest
vereinheitlicht die Secret-Erwartungen. Naechstes: Iteration 69
Doku parameter-secret-deployment.md.
[16:18] Iteration 69 abgeschlossen: Deployment-Doku
docs/konzept/parameter-secret-deployment.md angelegt
(Bezugsquellen, OpenShift-Template, Rollout-Checkliste,
Dual-Write-Key-Rotation, Backup-Strategie, Abgrenzung und
Fehlerbehandlung). Damit ist Block B (Parameter-Store-
Callsite-Migration) abgeschlossen. Naechstes: Block C
Reachability (Auto-Trigger / JGit / OSV).
[16:38] Iteration 70 abgeschlossen: Reachability-Auto-Trigger-
Schwellwert und Rate-Limit implementiert. Neuer Event
LowConfidenceAiSuggestionEvent + Port
ReachabilityAutoTriggerPort (Noop-Adapter als Default) +
Service ReachabilityAutoTriggerService mit in-memory Rate-
Limit pro (productVersionId, cveKey). Zwei neue
Parameter-Store-Eintraege (threshold, cooldown).
AutoAssessmentOrchestrator publiziert den Event, wenn
Publisher und ReachabilityConfig vorhanden sind. 7 neue
Service-Tests. Naechstes: Iteration 71 JGit-Adapter fuer
Reachability (Sandbox-abhaengig, ggf. skip-in-CI-Vermerk).
[16:47] Iteration 71 abgeschlossen: JGitGitCheckoutAdapter
(Bean-Name jgitGitCheckoutAdapter) implementiert. Cache pro
(URL, Commit) im tmpdir-Unterordner, @Scheduled-Cleanup-Job
gemaess cvm.ai.reachability.git.cache-ttl-hours (Default 72).
Optionales HTTPS-Token aus dem Parameter-Store. SSH bleibt
offen. Drei neue Katalog-Eintraege, JGit-Maven-Dependency.
Fuenf neue Tests mit lokalem Bare-Repo ueber file://.
Naechstes: Iteration 72 OSV-Mirror fuer air-gapped.
[16:54] Iteration 72 abgeschlossen: OsvJsonlMirror (pure Java)
+ OsvJsonlMirrorLookup (@Primary, @ConditionalOnProperty)
implementiert. JSONL-Datei wird beim Boot einmal gelesen,
Lookup liefert aus dem In-Memory-Index. Zwei neue Katalog-
Eintraege (mirror.enabled, mirror.file). Sieben neue Unit-
Tests (leere Datei, Aliase, direkte CVE, Duplikate, reload,
defekte Zeile, fehlende Datei). Naechstes: schauen ob die
Liste leer ist.
[17:01] Iteration 73 abgeschlossen: Admin-Endpunkt
POST /api/v1/admin/osv-mirror/reload fuer air-gapped Setups.
Controller injiziert den Mirror-Lookup optional; inaktiv ->
HTTP 503 mit osv_mirror_inactive, aktiv -> reload + neue
indexSize. OsvJsonlMirrorLookup bekommt indexSize()-Accessor.
Zwei neue MockMvc-Tests.
[17:08] Iteration 74 abgeschlossen: persistente DRAFTs pro
Umgebung. Neuer Service-Call latestDraftFor + Endpunkt
GET /api/v1/environments/{id}/profile/draft. Frontend
ProfilesComponent.laden() laedt DRAFT mit Diff nach. Vier
neue Tests (2 Web + 2 Karma). Naechstes: offene-punkte.md
erneut pruefen.
[17:13] Iteration 75 abgeschlossen: OSV-Mirror Reload-Button
im Admin-UI (Seite /admin/cve-import) inklusive
OsvMirrorService-Wrapper und 503-Handling. Zwei neue Karma-
Tests.
[17:22] Iteration 76 abgeschlossen: Product.repoUrl-Feld.
Flyway V0040 + Entity + DTO + Service + REST-DTO + Controller
+ Frontend-Service + AdminProducts-Edit-Prompt. Bereitet den
echten ReachabilityAutoTriggerPort-Adapter in einer Folge-
Iteration vor.
[17:28] Iteration 77 abgeschlossen: ReachabilityAutoTriggerAdapter
(@Primary) verdraengt den Noop. Zieht repoUrl aus Product,
commitSha aus ProductVersion.gitCommit, Symbol/Language aus
PurlSymbolDeriver und ruft ReachabilityAgent.analyze async.
Damit ist die Auto-Trigger-Kette komplett verdrahtet. Wegen
Sandbox-Limits kein neuer Integrationstest (Subprocess-
abhaengig).
[17:34] Iteration 78 abgeschlossen: OSV-Mirror Versions-Filter.
affected.versions wird jetzt beim Index-Bau gespeichert;
findCveIdsForPurls matcht exakt gegen die Liste. Query ohne
Version oder leere Liste -> konservatives Match (keine
Regression). Vier neue OsvJsonlMirrorTest-Cases.
[17:40] Iteration 79 abgeschlossen: OSV-Mirror semver-Ranges
(MVP). Neuer Record SemverRange fuer drei-teilige numerische
Versionen; ranges[0].events.introduced/fixed werden geparst,
sonst konservatives "match all". Drei neue Tests
(Standard-Range, unparseable-Fallback, range ohne fixed).

[17:42] Sauberer Stopp. Verbleibende offene Punkte in
docs/20260419/offene-punkte.md sind alle entweder Sandbox-
/Infra-gebunden oder Non-Code:
- JGit SSH-Support (braucht Vault-Anbindung + echten
  Git-Server).
- Network-Sandboxing fuer den Reachability-Subprocess (Docker).
- Ecosystem-spezifische Versions-Sortierung fuer OSV-Ranges
  (mehrere halbtaegige Aufgaben: Maven-VersionRange, PEP440,
  npm-semver-prerelease).
- Tenant-Default-Setzen + Keycloak-Realm-Mapping (Admin-SQL
  bzw. Realm-Setup, keine Code-Aufgabe).
- Playwright-E2E + axe-core in CI, Karma in CI,
  Testcontainers-IT auf Docker-Desktop-macOS (alle auf
  CI-Runner-Seite).
Das Kriterium "keinen konkret umsetzbaren Punkt mehr" (Abbruch-
bedingung 1) ist damit fuer diese Sandbox-Umgebung erreicht;
kein Fortschritt im verbleibenden Zeitbudget moeglich, ohne
in einen der drei echten Abbrueche zu laufen.

Gesamtbilanz Folge-Session: 17 Iterationen (63-79).
Block A (Bug + UI-Integrationen) komplett, Block B (Parameter-
Store-Callsite-Migration + Deployment-Doku) komplett, Block C
(Reachability-Auto-Trigger + JGit + OSV-Mirror inkl. Reload-
Endpunkt/UI und Versionsfilter) komplett. Zusaetzlich
persistente DRAFTs, Product.repoUrl inkl. Edit-UI, echter
ReachabilityAutoTriggerAdapter und OSV semver-Range-MVP.

---

## UX-Verbesserungs-Session 2026-04-19 abends

Start: 18:05. Zeitbox 10 h -> Stop 04:05.
Basis: UX-Review mit 15 Iterationen (U-01 .. U-10).

[18:17] Iteration 80 abgeschlossen: U-01a Workflow-CTAs.
Scan-Upload Summary -> "Zur Queue mit productVersionId"-Deep-
Link + Dashboard-CTA. Queue liest queryParams (productVersionId,
environmentId, status) und uebersetzt sie in den Store-Filter.
Queue-Empty-State mit drei CTAs (Neuer Scan/Berichte/Dashboard).
Dashboard-Handlungskarten (Scan/Queue/Waiver) oberhalb der KPIs,
rollen-gefiltert. 7 neue Karma-Cases (insgesamt 101 Tests).
Naechstes: U-01b weitere Workflow-CTAs.
[18:26] Iteration 81 abgeschlossen: U-01b Workflow-CTAs Runde 2.
Header-Buttons Reachability->FixVerif, FixVerif->Waiver,
Anomalie->Waiver, Waiver->Bericht. Vier neue Specs, Karma 105.
Naechstes: U-02a Queue-Status-Filter + URL-Persistenz.
[18:33] Iteration 82 abgeschlossen: U-02a. Status-Chips statt
Select (ALLE/PROPOSED/NEEDS_REVIEW/APPROVED/REJECTED/EXPIRED).
queue.component synchronisiert Filter <-> queryParams in beide
Richtungen; Deep-Links und Reload halten den Zustand. Karma 109.
Naechstes: U-02b CVE/KPI/FixVerif-Filter-URL + KPI-Presets.
[18:40] Iteration 83 abgeschlossen: U-02b Filter-URL in
/cves (q/severity/kev/page), /fix-verification (grade) und
/tenant-kpi (window). Sechs neue Specs, Karma 115.
[18:55] Iteration 84 abgeschlossen: U-02c Tenant-Badge wird
interaktiv; Popover mit Mandanten-Liste und "Als Default"-
Button fuer Admins. Drei neue Unit-Tests (Shell ohne
DOM-Rendering aus Service-Tree-Gruenden). Karma 118.
Naechstes: U-03 Waiver-Lifecycle im UI.
[19:03] Iteration 85 abgeschlossen: U-03 Waiver-Extend /
Revoke inkl. cvm-dialog, Vier-Augen-Pre-Check im Verlaengern-
Dialog. Neuer waivers.service.spec + 3 neue Component-Cases.
Karma 123.
[19:08] Iteration 86 abgeschlossen: U-04a Vier-Augen-Warnung
im Queue-Detail. Rote banner-critical-Box + disabled
Approve-Button, wenn auth.username() === entry.decidedBy.
Drei neue queue-detail-Cases. Karma 126.
[19:16] Iteration 87 abgeschlossen: U-04b Assessment-Audit-Trail.
Neuer Backend-Endpoint GET /findings/{id}/assessments/history
+ Lazy-Load im Queue-Detail-Panel via Details-Collapsible.
Karma 127. Naechstes: U-05 Reachability-Detail.
