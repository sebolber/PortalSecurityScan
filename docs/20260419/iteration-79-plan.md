# Iteration 79 - Plan: OSV-Mirror semver-Ranges (MVP)

**Jira**: CVM-316

## Ziel

Iteration 78 hat den exakten `affected.versions`-Filter ergaenzt.
OSV liefert haeufiger `affected.ranges[*].events` mit
`introduced` und `fixed`. Diese Iteration unterstuetzt den
einfachsten Fall:

- Genau ein `ranges`-Eintrag.
- Beide Events (`introduced`, `fixed`) sind numerische 3-Tupel
  im Format `X.Y.Z`.
- Query-PURL-Version ebenfalls `X.Y.Z` und numerisch.

Ist eine der Bedingungen verletzt, greift konservatives
"match all" wie bisher.

## Umfang

- Neuer Record `SemverRange` (als inneres Record von
  `OsvJsonlMirror`):
  - `ALL`: kein Range-Filter aktiv.
  - `parse(String)` liefert `int[3]` oder `null`.
  - `contains(String)` prueft `introduced <= version < fixed`.
- `AdvisoryEntry` wird um `range` erweitert (default `ALL`).
  `matches(queryVersion)` konsultiert Range und Versions-Liste.
- `rangeVonAffected(JsonNode)` im Index-Bau liest
  `ranges[0].events` und baut bei ParseErfolg ein `SemverRange`,
  sonst `ALL`.

## Tests

Drei neue Cases in `OsvJsonlMirrorTest`:

1. Standard `introduced=1.0.0, fixed=2.0.0` - drei Queries
   (`1.0.0`, `1.9.9`, `2.0.0`, `0.9.0`) mit korrektem
   Ergebnis.
2. Nicht-parseable Semver (`1.0.0-rc1`) -> "match all"-
   Fallback.
3. Range ohne `fixed`-Event -> "match all".

## Nicht-Umfang

- Ecosystem-spezifische Versionen (Maven, PEP440, npm-prerelease).
- Mehrere Ranges pro Advisory.
- `last_affected` als Ersatz fuer `fixed`.

## Abnahme

- `./mvnw -T 1C test` gruen, alle 14 OsvJsonlMirrorTest-Cases
  bestanden.
