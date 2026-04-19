# Iteration 60 - Test-Summary

## Neue Tests

- `TenantLookupServiceTest` +3 Faelle (setActive regulaer,
  Default-Schutz, unbekannt).

## Lauf

- `./mvnw -T 1C -pl cvm-application -am test` &rarr; 355 Tests,
  BUILD SUCCESS.
- `./mvnw -T 1C -pl cvm-app,cvm-api -am test` &rarr; 147 Tests,
  BUILD SUCCESS.
- `npx ng build` &rarr; ok.
- `npx ng lint` &rarr; All files pass linting.
