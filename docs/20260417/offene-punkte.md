# Offene Punkte (kumulativ)

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
