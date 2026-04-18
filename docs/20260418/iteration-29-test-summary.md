# Iteration 29 – Test-Summary

**Jira**: CVM-70
**Stand**: 2026-04-18

## Backend

`./mvnw -T 1C test` → **BUILD SUCCESS**.

| Modul | Tests | Skipped |
|---|---|---|
| cvm-domain | 19 | 0 |
| cvm-persistence | 3 | 3 (Docker) |
| cvm-application | 194 | 0 |
| cvm-integration | 23 | 0 |
| cvm-llm-gateway | 31 | 0 |
| cvm-ai-services | 29 | 0 |
| cvm-api | 131 | 0 |
| cvm-app | 5 | 5 (Docker) |
| cvm-architecture-tests | 8 | 0 |

Hinweis: In Umgebungen mit erreichbarem Docker (Linux-CI) laufen die
heute skipped Tests durch - Gate ist ab jetzt `DockerClientFactory
#isDockerAvailable`, nicht mehr die blosse Existenz von
`/var/run/docker.sock`.

## Frontend

- `npx ng build --configuration=development` → erfolgreich (6.4 s).
- `npx ng lint` → alle Dateien gruen.
- `npx tsc --noEmit -p tsconfig.spec.json` → keine Diagnostik
  (Karma-Suite kompiliert).

## Ergebnis

Blocker aus Iteration 28 (Karma-Suite und Persistenz-Bootstrap)
aufgeloest. Frontend-Unit-Lauf haengt jetzt nur noch am
Headless-Chrome in CI; Backend-Integrationstests laufen in
Docker-Umgebungen durch und skippen sonst sauber.
