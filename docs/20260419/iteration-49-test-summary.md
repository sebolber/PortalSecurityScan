# Iteration 49 - Test-Summary

## Neue Tests

- `ProductCatalogServiceTest` +5 Faelle (loescheVersion: Happy,
  Idempotenz, Nicht-gefunden, falsches Produkt, null-Parameter).

## Lauf

- `./mvnw -T 1C -pl cvm-application -am test` &rarr; 335 Tests,
  BUILD SUCCESS.
- `./mvnw -T 1C -pl cvm-app,cvm-api -am test` &rarr; 147 Tests,
  BUILD SUCCESS.
- `npx ng build` &rarr; ok.
- `npx ng lint` &rarr; All files pass linting.
