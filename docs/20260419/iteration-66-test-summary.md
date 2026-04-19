# Iteration 66 - Test-Summary

**Jira**: CVM-303
**Datum**: 2026-04-19

## Backend

Kommando: `./mvnw -T 1C test`

Reactor-Ergebnis: **BUILD SUCCESS** in 02:18 min.

| Modul                     | Ergebnis |
| ------------------------- | -------- |
| cvm-domain                | SUCCESS  |
| cvm-persistence           | SUCCESS  |
| cvm-application           | SUCCESS  |
| cvm-integration           | SUCCESS  |
| cvm-llm-gateway           | SUCCESS  |
| cvm-ai-services           | SUCCESS  |
| cvm-api                   | SUCCESS  |
| cvm-app                   | SUCCESS  |
| cvm-architecture-tests    | SUCCESS  |

## Fokus-Tests

- `ClaudeApiClientTest`: 6 Tests PASS.
  - `happyPath`
  - `rateLimited`
  - `serverError`
  - `tenantOverride`
  - `parameterResolverOverrideGreiftOhneRestart` *(neu)*
  - `kaputteAntwort`
- `SystemParameterCatalogTest`: 13 Tests PASS mit angepassten
  Erwartungen fuer Iteration 66 (Claude-Keys jetzt
  live-reloadable ausser `version`).

## ArchUnit

- `ParameterModulzugriffTest` -> 2 PASS (Repository und Cipher nur
  im `application.parameter..`-Paket).
- `ModulgrenzenTest` -> 7 PASS (`cvm.llm..` kennt nur `cvm.domain..`;
  der neue Port lebt im `cvm.llm.config..`-Paket und referenziert
  nichts aus `cvm.application..`).
- `TenantScopeTest` -> 2 PASS.
- `SpringBeanKonstruktorTest` -> 1 PASS.
