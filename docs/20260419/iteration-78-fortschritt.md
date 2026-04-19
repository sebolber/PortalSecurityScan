# Iteration 78 - Fortschritt: OSV-Mirror Versions-Filter

**Jira**: CVM-315
**Datum**: 2026-04-19

## Was wurde gebaut

- `OsvJsonlMirror`-Index umgebaut:
  - Struktur: `Map<basePurl, List<AdvisoryEntry>>`, wobei jeder
    Eintrag `{ cveIds, versions }` enthaelt.
  - `basePurl` = PURL ohne `@version`- und `?qualifier`-Suffix.
  - `versions` = `Set<String>` aus `affected[*].versions`. Leer
    bedeutet "alle Versionen betroffen" (Bestands-Verhalten).
- `PurlParts.parse(purl)` zerlegt eine Query-PURL in
  `(base, version)`. Qualifier (`?foo=bar`) werden ignoriert.
- `findCveIdsForPurls(List)` prueft pro Eintrag:
  - Leere `versions`-Menge -> match.
  - Query ohne Version -> match (konservativ, damit Scans ohne
    Version-Info keine Findings verlieren).
  - Sonst: exakter Match gegen die Liste.
- Das In-Memory-Datenmodell bleibt thread-sicher via
  `AtomicReference`.

## Tests

Vier neue Cases in `OsvJsonlMirrorTest`:

1. Advisory ohne `versions` -> Query mit Version matcht.
2. Advisory mit `versions=["1.0.0"]` -> nur Query `@1.0.0`
   matcht, `@1.1.0` nicht.
3. Advisory mit `versions=["1.0.0","1.0.1"]` -> beide matchen,
   `2.0.0` nicht.
4. Query ohne Version -> matcht trotz `versions`-Liste.

Bestehende sieben Cases weiter gruen.

## Ergebnisse

- `./mvnw -T 1C test` -> **BUILD SUCCESS** in 02:23 min.
- `OsvJsonlMirrorTest` 11 Tests PASS.
- ArchUnit, SystemParameterCatalogTest unveraendert gruen.

## Offen / bewusst verschoben

- Keine `ranges`/`events`-Semantik (semver
  introduced/fixed/last_affected). Dafuer braucht es
  ecosystem-spezifische Version-Sortierung (Maven-DependencyRange,
  semver-npm, PEP440, etc.). Bleibt als Folge-Iteration.
