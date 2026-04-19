# Iteration 46 - Fortschritt

**Thema**: ArchUnit-Regel + End-to-End-Test fuer den Parameter-Store-
Lesepfad (CVM-96).

## Was gebaut wurde

- `cvm-architecture-tests/...ParameterModulzugriffTest`:
  zwei ArchUnit-Regeln
  - Nur `com.ahs.cvm.application.parameter..` darf
    `SystemParameterRepository` und
    `SystemParameterAuditLogRepository` referenzieren.
  - Nur `com.ahs.cvm.application.parameter..` darf den
    `SystemParameterSecretCipher` verwenden.
- `cvm-ai-services/...ReachabilityRuntimeOverrideE2ETest`:
  verdrahtet die realen `SystemParameterResolver`,
  `SystemParameterSecretCipher` und `ReachabilityConfig` gegen ein
  Mock-Repository. Zeigt, dass ein DB-Wechsel der
  `cvm.ai.reachability.enabled`-Zeile beim naechsten
  `enabledEffective()`-Aufruf sofort wirkt - ohne die Bean neu zu
  bauen.

## Build

- `./mvnw -T 1C -pl cvm-architecture-tests -am test` &rarr;
  10 Tests (7 Modulgrenzen + 2 Parameter-Zugriff + 1
  Spring-Bean-Konstruktor), BUILD SUCCESS.
- `./mvnw -T 1C -pl cvm-ai-services -am test` &rarr; BUILD SUCCESS.

## Hinweise fuer den naechsten Start

- Keine Flyway-/Dependency-Aenderung.
- Testcontainers-basiertes E2E (mit echter Postgres-DB) bleibt als
  CI-Follow-up offen, weil in der Sandbox kein Docker verfuegbar
  ist. Der jetzige Test nutzt die realen Service-Klassen gegen ein
  Mockito-Repository - fachlich identisches Verhalten, nur ohne
  Docker-Round-Trip.

## Vier Leitfragen (Oberflaeche)

Keine UI-Aenderung in dieser Iteration.
