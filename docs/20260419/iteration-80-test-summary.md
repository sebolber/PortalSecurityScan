# Iteration 80 - Test-Summary

**Jira**: CVM-320

## Frontend

- `npx ng lint` -> "All files pass linting."
- `npx ng build` -> Bundle OK.
- Karma gesamt: 101 Tests SUCCESS.
  - `queue.component.spec.ts` 3 Cases (1 alt + 2 neu).
  - `dashboard.component.spec.ts` 2 Cases (neu).
  - `scan-upload.component.spec.ts` 2 Cases (neu).

## Backend

- `./mvnw -T 1C test` -> **BUILD SUCCESS** in 02:00 min (keine
  Backend-Aenderungen; sicherheitshalber voller Lauf).
- Alle 10 Reactor-Module gruen.
