# Iteration 72 - Fortschritt: OSV-Mirror fuer air-gapped

**Jira**: CVM-309
**Datum**: 2026-04-19

## Was wurde gebaut

### Klassen

- `OsvJsonlMirror` (pure Java, `cvm-integration.osv`):
  - Nimmt einen `Path` zur JSONL-Datei.
  - `reload()` liest die Datei zeilenweise, parst jedes JSON,
    extrahiert CVE-IDs (aus `id` und `aliases`) und die PURL-
    Liste aus `affected[*].package.purl`.
  - Baut eine `purl -> List<cveId>`-Map in einem
    `AtomicReference` und antwortet daraus auf
    `findCveIdsForPurls(List)`.
  - Defekte Zeilen werden ignoriert; fehlende Datei fuehrt zu
    einem leeren Index plus Warn-Log.
- `OsvJsonlMirrorLookup` (Spring-Bean,
  `implements ComponentVulnerabilityLookup`, `@Primary` +
  `@ConditionalOnProperty("cvm.enrichment.osv.mirror.enabled")`).
  Laedt die Datei im `@PostConstruct` einmal in den Speicher und
  verdraengt bei Aktivierung den Netz-basierten
  `OsvComponentLookup`.

### Katalog

Zwei neue Eintraege im System-Parameter-Store:

- `cvm.enrichment.osv.mirror.enabled` (BOOLEAN, Default `false`,
  restartRequired=true).
- `cvm.enrichment.osv.mirror.file` (STRING, Default leer,
  restartRequired=true).

### Tests

`OsvJsonlMirrorTest` (7 Tests):

- Leere Datei -> keine Treffer.
- Advisory mit `aliases=CVE-...` + `affected.package.purl`.
- Advisory mit direkter CVE-ID im `id`-Feld.
- Mehrere Advisories pro PURL -> Set ohne Duplikate.
- `reload()` spiegelt Datei-Aenderungen.
- Defekte JSON-Zeile wird uebersprungen.
- Fehlende Datei -> leerer Index, keine Exception.

## Ergebnisse

- `./mvnw -T 1C test` -> **BUILD SUCCESS** in 02:32 min.
- Alle 10 Reactor-Module gruen.
- ArchUnit unveraendert gruen.

## Offen / Folge-Punkte

- **Kein Versionsbereich-Matching**: ein Advisory listet alle
  seine CVEs fuer jede erwaehnte PURL ungeachtet der OSV
  `affected.ranges`. Fuer den air-gapped-MVP akzeptabel; ein
  Folge-Schritt kann den `SemverRange`-Filter ergaenzen.
- **Kein CLI-Refresh-Job**: die JSONL-Datei wird aktuell vom
  extern (Sidekick) gepflegt; `reload()` steht fuer einen
  spaeteren Admin-Endpunkt bereit.

## Migrations / Deployment

- Keine Flyway-Migration.
- Keine neuen Dependencies (JSONL-Parser reuseet Jackson).
- Zwei neue Katalog-Eintraege, Bootstrap zieht sie beim
  naechsten Start.
