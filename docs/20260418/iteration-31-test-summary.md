# Iteration 31 – Test-Summary

**Jira**: CVM-72
**Stand**: 2026-04-18

## Backend

`./mvnw -T 1C test` → **BUILD SUCCESS** (alle Module gruen).

Neu hinzugekommen:

- `BrandingServiceTest`: +4 Tests (nun 10 gesamt).
  - "Update: schreibt vorherigen Stand vor Speichern in History"
  - "Rollback: stellt vergangene Version ueber regulaeres Update wieder her"
  - "Rollback: unbekannte Version wirft UnknownBrandingVersion"
  - "History: liefert begrenzte absteigende Liste pro Mandant"
- `BrandingControllerWebTest`: +3 Tests (nun 8 gesamt).
  - "GET /api/v1/admin/theme/history: liefert History-Liste"
  - "POST /api/v1/admin/theme/rollback/{version}: Happy-Path liefert neue Version"
  - "POST /api/v1/admin/theme/rollback/{version}: unbekannte Version liefert 404"

## Architektur

`ModulgrenzenTest` + `SpringBeanKonstruktorTest` → 8/8 gruen.
Neue `BrandingConfigHistory`-Entity lebt in `cvm-persistence`,
API-Mapping ueber `BrandingHistoryEntry` in `cvm-application` -
keine `api -&gt; persistence`-Durchgriffe.

## Frontend

Keine Aenderungen in dieser Iteration.
