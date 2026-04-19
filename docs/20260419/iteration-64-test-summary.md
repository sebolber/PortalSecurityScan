# Iteration 64 - Test-Summary

**Jira**: CVM-301
**Datum**: 2026-04-19

## Frontend

- `npx ng lint` -> "All files pass linting."
- `npx ng build` -> Application bundle generation complete (13.4 s).
- Karma (`**/profiles/**/*.spec.ts`): 9 Tests PASS.
  - 5 Service-Tests: diffGegenAktiv (3) + draftAktualisieren + loesche.
  - 4 Component-Tests: Bearbeiten-Dispatch, Loeschen-Abbruch,
    Loeschen-Erfolg, editorUmschalten-Reset.

## Backend

- `./mvnw -T 1C -pl cvm-api -am test` -> **BUILD SUCCESS**,
  150 Tests, 0 Failures. (Keine Backend-Aenderungen in dieser
  Iteration.)
