# Iteration 41 - Test-Summary

## Unit-Tests

- `SystemParameterCatalogTest` (6 Tests) - Katalog-Schluessel,
  Dubletten-Freiheit, Secrets-Freiheit, Default-Konsistenz gegen die
  `@Value`-Fallbacks, Typ-Konsistenz, Kategorie-Abdeckung.
- `SystemParameterCatalogBootstrapTest` (5 Tests) - Leere Tabelle,
  Idempotenz bei Teil-Seed, Ueberspringen inaktiver Mandanten,
  Kein-Mandant-Szenario, Mehrere Mandanten.

## Lauf

- `./mvnw -T 1C -pl cvm-application -am test` &rarr; **BUILD SUCCESS**,
  288 Tests (+ 11 neue), 0 failures, 0 errors.
- `./mvnw -T 1C -pl cvm-app -am test` &rarr; **BUILD SUCCESS**,
  147 Web-Tests gruen, 6 Testcontainers-Tests geskippt
  (Sandbox-Docker).

## Coverage

Nicht separat erhoben; die Tests adressieren alle neuen Klassen und
alle Pfade des Bootstrap (leer/Teil-Seed/inaktiv/mehrere).

## Pitest

Nicht ausgefuehrt in dieser Iteration.
