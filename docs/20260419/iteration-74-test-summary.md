# Iteration 74 - Test-Summary

**Jira**: CVM-311
**Datum**: 2026-04-19

## Backend

`./mvnw -T 1C test` -> **BUILD SUCCESS** in 02:34 min.

Fokus:

- `ProfileControllerWebTest` 10 Tests PASS, darunter
  - `aktuellerDraftLiefert200`
  - `aktuellerDraftLiefert404`

## Frontend

- Karma (`**/profiles/**/*.spec.ts`): 11 Tests PASS.
  - 7 Service-Cases (3 diff + 2 put/delete + 2 aktuellerDraft).
  - 4 Component-Cases aus Iteration 64.
- `npx ng lint` -> "All files pass linting."
- `npx ng build` -> Application bundle generation complete.

## ArchUnit / Architektur

- Keine Struktur-Aenderungen; ArchUnit-Regeln unveraendert gruen.
