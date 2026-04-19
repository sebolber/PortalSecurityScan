# Iteration 76 - Test-Summary

**Jira**: CVM-313
**Datum**: 2026-04-19

## Backend

`./mvnw -T 1C test` -> **BUILD SUCCESS** in 02:10 min.

| Modul                  | Ergebnis |
| ---------------------- | -------- |
| cvm-domain             | SUCCESS  |
| cvm-persistence        | SUCCESS  |
| cvm-application        | SUCCESS  |
| cvm-integration        | SUCCESS  |
| cvm-llm-gateway        | SUCCESS  |
| cvm-ai-services        | SUCCESS  |
| cvm-api                | SUCCESS  |
| cvm-app                | SUCCESS  |
| cvm-architecture-tests | SUCCESS  |

- `ProductsControllerWebTest` alle 9 Cases PASS (3 `ProductView`-
  Konstruktor-Aufrufe um den neuen `repoUrl`-Parameter erweitert).

## Frontend

- `npx ng lint` -> "All files pass linting."
- `npx ng build` -> Bundle OK.
- Karma (full suite): 95 Tests PASS.
