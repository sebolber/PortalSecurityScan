# Iteration 42 - Test-Summary

## Unit-Tests

- `SystemParameterCatalogTest` (11 Tests, davon 5 neu): Block-A.2-
  Keys, Block-A.2-Defaults, Block-A.2-Typen, Block-A.2-Kategorien,
  Nicht-migrieren-Liste eingehalten.
- `SystemParameterCatalogBootstrapTest` (5 Tests) - unveraendert,
  skaliert mit den neuen Katalog-Eintraegen.

## Lauf

- `./mvnw -T 1C -pl cvm-application -am test` &rarr; BUILD SUCCESS,
  293 Tests, 0 Failures, 0 Errors.

## Coverage / Pitest

Nicht separat erhoben.
