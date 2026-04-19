# Iteration 79 - Fortschritt: OSV-Mirror semver-Ranges (MVP)

**Jira**: CVM-316
**Datum**: 2026-04-19

## Was wurde gebaut

- Neuer Record `SemverRange` in `OsvJsonlMirror`:
  - `ALL`-Konstante fuer "keine Range-Einschraenkung".
  - `parse(String)` liefert `int[3]` oder `null`; lehnt alles
    ab, was nicht exakt drei numerische Teile hat.
  - `contains(String)` prueft
    `introduced <= version < fixed`.
- `AdvisoryEntry` erweitert um `SemverRange range`; neuer
  Konvenienz-Konstruktor faellt auf `ALL` zurueck. `matches(...)`
  kombiniert jetzt beide Filterebenen: eine leere Menge + ALL
  bedeutet weiterhin "alle Versionen".
- `rangeVonAffected(JsonNode)` im Index-Bau liest
  `ranges[0].events`. Bei `introduced` + `fixed` als 3-teilige
  numerische Versionen wird der Range aktiv; sonst bleibt es
  konservativ (ALL).

## Tests

Drei neue Cases in `OsvJsonlMirrorTest`:

- `rangesIntroducedFixed`: `introduced=1.0.0, fixed=2.0.0`
  matcht `1.0.0` + `1.9.9`, nicht `2.0.0` oder `0.9.0`.
- `nichtParseableRangeFallback`: `introduced=1.0.0-rc1` ->
  Range wird ignoriert, Query `3.0.0` matcht trotzdem.
- `rangeOhneFixed`: nur `introduced` -> Range ignoriert,
  Match konservativ.

Alle 14 Cases PASS.

## Ergebnisse

- `./mvnw -T 1C test` -> **BUILD SUCCESS** in 02:06 min.
- Alle 10 Reactor-Module gruen, ArchUnit unveraendert gruen.

## Offen

- Ecosystem-spezifische Versions-Reihenfolgen (Maven,
  PEP440, npm-prerelease), mehrere Ranges pro Advisory,
  `last_affected`-Semantik. Bleiben als Folge-Iterationen.

## Migrations / Deployment

- Keine Flyway-Migration, keine neuen Dependencies. Reines
  In-Memory-Verhalten.
