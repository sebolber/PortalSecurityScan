# Iteration 72 - Plan: OSV-Mirror fuer air-gapped

**Jira**: CVM-309

## Ziel

In abgeschotteten Umgebungen darf `OsvComponentLookup` keine
Netz-Calls machen. Stattdessen soll ein lokales OSV-JSONL-File
(eine Advisory pro Zeile, Format wie die OSV.dev-Exports) als
Quelle dienen. Das Feature ist optional und wird per
Feature-Flag aktiviert.

## Umfang

- **Pure-Java-Klasse** `OsvJsonlMirror`
  (`cvm-integration`, package `com.ahs.cvm.integration.osv`):
  - Konstruktor nimmt einen `Path` zur JSONL-Datei entgegen.
  - `load()` liest das File zeilenweise, parst jedes JSON, baut
    eine Map `purl -> Set<cveId>` auf.
  - `findCveIdsForPurls(List<String>)` liefert pro PURL die
    gefundenen CVE-IDs.
  - `reload()` baut die Map neu auf (fuer den CLI-Refresh, der
    in einer Folge-Iteration folgt).
- **Spring-Bean** `OsvJsonlMirrorLookup implements
  ComponentVulnerabilityLookup` mit
  `@ConditionalOnProperty("cvm.enrichment.osv.mirror.enabled")`,
  `@Primary`. Laedt die Datei beim Start einmal in den Speicher.
- **Katalog-Eintraege**:
  - `cvm.enrichment.osv.mirror.enabled` (BOOLEAN, Default
    `false`, restartRequired=true).
  - `cvm.enrichment.osv.mirror.file` (STRING, Default leer,
    restartRequired=true).

## Tests (TDD)

Unit-Tests auf `OsvJsonlMirror`:

- Leere Datei -> keine Treffer.
- Advisory mit `affected.package.purl` + `aliases=["CVE-..."]`
  -> PURL liefert die CVE.
- Advisory mit direkter CVE-ID in `id` (selten, aber spec-
  konform) -> ebenfalls erkannt.
- Mehrere Advisories pro PURL -> Set ohne Duplikate.
- `reload()` spiegelt geaenderte Datei wider.
- Defekte JSON-Zeile -> ignoriert, der Rest wird geladen.

## Nicht-Umfang

- Keine Versionsbereich-Pruefung (OSV `affected.ranges`). Der
  MVP liefert immer alle CVEs, die fuer einen PURL gelistet
  sind - in air-gapped-Setups akzeptabel, solange die Basis-
  Liste gepflegt wird.
- Kein CLI-Job fuer den OSV-Refresh; das Mirror-File wird
  extern geliefert (OSV-Dump per Sidekick-Prozess).

## Abnahme

- `./mvnw -T 1C test` -> BUILD SUCCESS.
- Neue Unit-Tests gruen.
- `SystemParameterCatalogTest` bleibt gruen.
