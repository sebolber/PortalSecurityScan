# Iteration 77 - Test-Summary

**Jira**: CVM-314
**Datum**: 2026-04-19

## Backend

`./mvnw -T 1C test` -> **BUILD SUCCESS** in 02:38 min.

| Modul                  | Ergebnis |
| ---------------------- | -------- |
| cvm-domain             | SUCCESS  |
| cvm-persistence        | SUCCESS  |
| cvm-application        | SUCCESS  |
| cvm-integration        | SUCCESS  |
| cvm-llm-gateway        | SUCCESS  |
| cvm-ai-services        | SUCCESS  |
| cvm-api                | SUCCESS  |
| cvm-app                | SUCCESS  |
| cvm-architecture-tests | SUCCESS  |

## ArchUnit / Spring

- `ParameterModulzugriffTest`, `ModulgrenzenTest`,
  `TenantScopeTest`, `SpringBeanKonstruktorTest` gruen.
- Die neue Bean `ReachabilityAutoTriggerAdapter` hat genau
  einen Konstruktor, wird vom Context erkannt.

## Sandbox

- Keine Integration-Tests fuer den echten Subprozess-Lauf
  (Docker/Chromium). Bestands-Tests von `ReachabilityAgent`
  mocken den Subprozess und bleiben gruen.
