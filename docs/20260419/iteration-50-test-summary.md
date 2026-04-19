# Iteration 50 - Test-Summary

## Neue Tests

- `RuleServiceTest` +4 Faelle (loesche Happy, Idempotenz,
  unbekannt, null).

## Angepasste Tests

- `RuleEngineTest`: alle drei `given(...)` mocken jetzt die
  `findByStatusAndDeletedAtIsNullOrderByCreatedAtDesc`-Methode
  (per sed-Umstellung).

## Lauf

- `./mvnw -T 1C -pl cvm-application -am test` &rarr; 339 Tests,
  BUILD SUCCESS.
- `./mvnw -T 1C -pl cvm-app,cvm-api -am test` &rarr; 147 Tests,
  BUILD SUCCESS.
- `npx ng build` &rarr; ok.
- `npx ng lint` &rarr; All files pass linting.
