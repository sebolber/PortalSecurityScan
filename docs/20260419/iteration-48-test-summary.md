# Iteration 48 - Test-Summary

## Neue Tests

- `EnvironmentQueryServiceTest` +5 Faelle (listAll-Filter,
  loesche_happy, Idempotenz, not_found, null).

## Lauf

- `./mvnw -T 1C -pl cvm-application -am test` &rarr; 330 Tests,
  BUILD SUCCESS.
- `./mvnw -T 1C -pl cvm-app,cvm-api -am test` &rarr; 147 Tests,
  BUILD SUCCESS.
- `npx ng build` &rarr; ok.
- `npx ng lint` &rarr; All files pass linting.
