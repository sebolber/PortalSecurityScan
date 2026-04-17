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
