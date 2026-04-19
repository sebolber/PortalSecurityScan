# Iteration 72 - Test-Summary

**Jira**: CVM-309
**Datum**: 2026-04-19

## Backend

`./mvnw -T 1C test` -> **BUILD SUCCESS** in 02:32 min.

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

## Fokus

- `OsvJsonlMirrorTest` (7 Tests) PASS.
- `SystemParameterCatalogTest` bleibt gruen (die zwei neuen
  non-sensitive Eintraege zaehlen nicht zu Secret-Count).
- Bestehende `OsvComponentLookupTest` und
  `OsvEffectivePropertiesTest` unveraendert gruen; der Mirror-
  Lookup ist per `@ConditionalOnProperty` ausgeschaltet, solange
  `cvm.enrichment.osv.mirror.enabled` nicht `true` ist.
