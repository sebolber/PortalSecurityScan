# Iteration 39 – Fortschritt

**Thema**: CVE-Listen Server-Paging statt Stream-Filter
**Jira**: CVM-83
**Datum**: 2026-04-19

## Umgesetzt

- `CveRepository.searchPage(searchLower, minScore, maxScore,
  informational, onlyKev, pageable)` als JPA-Query mit
  optionalen Filtern. Die Severity-Filter werden als numerische
  CVSS-Grenzen uebergeben, der Mapping-Schritt bleibt im Service.
- `CveQueryService.findPage(...)` nutzt jetzt das Repository-Paging
  statt `findAll()` + Stream-Filter. Der Java-Speicherbedarf faellt
  damit linear mit der Seitengroesse statt linear mit der CVE-
  Gesamtmenge.
- Hilfsmethoden `untergrenze`/`obergrenze` fuer das Severity-zu-
  Score-Mapping. Die alten Stream-Helfer (`matchesSearch`,
  `matchesSeverity`, `ableiten`) sind entfernt, weil sie niemand
  mehr ruft.

## Test-Status

- `./mvnw -T 1C test`: BUILD SUCCESS. 260 Tests, keine
  Regressionen.
- ArchUnit: gruen.

## Offene Punkte

- Severity-Sortierung serverseitig (aktuell stabil nach
  publishedAt DESC; Sort by derived severity braucht entweder
  eine computed Column oder ein Sort-Mapping in der Query).
- Echte Performance-Messung gegen 20000 CVEs (nur in Docker-CI
  sinnvoll).
