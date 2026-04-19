# Iteration 51 - Test-Summary

## Neue Tests

- `ContextProfileServiceTest` +4 Faelle (updateDraft Happy / ACTIVE-
  lehnt-ab, loesche Happy / ACTIVE-lehnt-ab / Idempotenz).

## Lauf

- `./mvnw -T 1C -pl cvm-application -am test -Dtest=ContextProfileServiceTest`
  &rarr; 10 Tests, BUILD SUCCESS.
- `./mvnw -T 1C -pl cvm-app,cvm-api -am test` &rarr; 147 Tests,
  BUILD SUCCESS.
- `npx ng build` &rarr; ok.
- `npx ng lint` &rarr; All files pass linting.
