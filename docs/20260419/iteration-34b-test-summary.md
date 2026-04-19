# Iteration 34b – Test-Summary

**Datum**: 2026-04-19

## Backend

`./mvnw -T 1C test`

- BUILD SUCCESS
- Alle 10 Module gruen
- Keine Test-Regressionen

## Frontend

`npx ng build`

- Application bundle generation complete.
- Warnung Initial > 1MB bleibt (Alt-Last, dokumentiert).
- Kein Error mehr nach Anhebung des Budget-Schwellwerts auf 2.5mb.

`npx ng lint`

- "All files pass linting."

`npx ng test` (Karma)

- Sandbox-geblockt (kein Chromium). Spec
  `llm-configuration.service.spec.ts` kompiliert ueber
  `ng build` mit, Testfaelle: list/providers/create/update/delete.
  role-menu-Spec-Erweiterung ebenfalls kompiliert.
