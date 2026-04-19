# Iteration 53 - Test-Summary

## Neue Tests

- `RuleServiceTest` +3 Faelle (updateDraftHappy,
  updateDraftAbgelehntFuerActive, updateDraftUnbekannt).

## Lauf

- `./mvnw -T 1C -pl cvm-application -am test` &rarr; 347 Tests,
  BUILD SUCCESS.
- `npx ng build` &rarr; ok.
- `npx ng lint` &rarr; All files pass linting.
