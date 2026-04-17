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
