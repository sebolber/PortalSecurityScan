# Iteration 46 - ArchUnit-Regel + E2E-Test fuer Parameter-Store

## Ziel

1. ArchUnit-Regel: Nur das Parameter-Modul
   (`com.ahs.cvm.application.parameter`) darf die System-Parameter-
   Repositories und den `SystemParameterSecretCipher` direkt kennen.
2. End-to-End-Test fuer `cvm.ai.reachability.enabled`, der zeigt,
   dass eine DB-Aenderung den Agent-Aufruf ohne Neustart beeinflusst.

## Vorgehen

1. Neue Klasse `ParameterModulzugriffTest` unter
   `cvm-architecture-tests`. Zwei Regeln (Repository + Cipher).
2. Neuer Test `ReachabilityRuntimeOverrideE2ETest` unter
   `cvm-ai-services/reachability`. Wiring:
   - echtes `SystemParameterResolver`
   - echter `SystemParameterSecretCipher`
   - `ReachabilityConfig` mit Boot-Default `enabled=false`
   - `SystemParameterRepository` via Mockito, damit wir den
     DB-Wert zur Laufzeit aendern koennen
   - `TenantContext` gesetzt
   - Assertions: keine DB-Zeile &rarr; `false`; DB-Zeile `true` &rarr;
     `true`; DB-Wechsel `true` &rarr; `false` wirkt sofort.

Testcontainers ist in der Sandbox blockiert (Docker fehlt). Der Test
nutzt stattdessen die realen Service-Klassen mit Mock-Repository.
Ein zusaetzlicher Testcontainers-Lauf fuer die gleiche Property bleibt
als Follow-up in `offene-punkte.md`.

## Testerwartung

- `./mvnw -T 1C -pl cvm-architecture-tests -am test` &rarr; BUILD SUCCESS.
- `./mvnw -T 1C -pl cvm-ai-services -am test` &rarr; BUILD SUCCESS.

## Jira

`CVM-96` - Parameter-Store ArchUnit + E2E-Proof.
