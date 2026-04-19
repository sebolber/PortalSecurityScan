# Iteration 71 - Test-Summary

**Jira**: CVM-308
**Datum**: 2026-04-19

## Backend

`./mvnw -T 1C test` -> **BUILD SUCCESS** in 02:07 min.

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

- `JGitGitCheckoutAdapterTest` (5 Tests) PASS.
- `SystemParameterCatalogTest`: Sensitive-Count von 4 auf 5
  aktualisiert, alle Assertions gruen.
- Bestehende Reachability-Tests (Agent, Config, Runtime-Override)
  gruen - der Noop-Adapter bleibt in reinen Unit-Tests aktiv, weil
  der JGit-Adapter nur via Spring-Context als
  `jgitGitCheckoutAdapter`-Bean geladen wird.

## Sandbox

- Netzwerk-Clone / SSH-Auth sind nicht getestet (Sandbox-Beschraenkung).
  Lokaler `file://`-Clone deckt den Happy-Path plus Cache-Reuse +
  Cleanup ab.
