# Iteration 15 - Reachability-Agent - Test-Summary

**Jira**: CVM-40
**Datum**: 2026-04-18

## Testlauf

```
./mvnw -T 1C test  BUILD SUCCESS  (~74 s)
```

## Neue Tests

### `cvm-llm-gateway` (+9 = jetzt 61)
| Testklasse | Anzahl | Kurz |
|---|---:|---|
| `SubprocessRunnerTest` | 6 | Validierung der SubprocessRequest-Invarianten + FakeRunner. |
| `ClaudeCodeSubprocessRunnerTest` | 3 | echo OK, Timeout greift, fehlendes Binary. **OS-bedingt nur Linux/Mac**. |

### `cvm-ai-services` (+6 = jetzt 48)
| Testklasse | Anzahl | Kurz |
|---|---:|---|
| `ReachabilityAgentTest` | 6 | drei Call-Sites + ACCEPT, deaktiviert, Timeout, ungueltiges JSON, Schema-Verstoss, --read-only-Flag-Pruefung. |

### `cvm-api` (+3 = jetzt 38)
| Testklasse | Anzahl | Kurz |
|---|---:|---|
| `ReachabilityControllerWebTest` | 3 | 200 mit Result-JSON; 404 bei unbekanntem Finding; 400 bei leerer repoUrl. |

## Gesamt-Testlage

| Modul | Gruen | Skipped | Rot |
|---|---:|---:|---:|
| cvm-domain | 4 | 0 | 0 |
| cvm-persistence | 0 | 6 | 0 |
| cvm-application | 126 | 0 | 0 |
| cvm-integration | 8 | 0 | 0 |
| cvm-llm-gateway | **61** | 0 | 0 |
| cvm-ai-services | **48** | 0 | 0 |
| cvm-api | **38** | 0 | 0 |
| cvm-app | 0 | 5 | 0 |
| cvm-architecture-tests | 8 | 0 | 0 |
| **Gesamt** | **293** | 11 | 0 |

## Sicherheits-Invarianten (durch Tests gehaerttet)

- Kein Subprocess ohne `--read-only`.
- Flag-Aus -&gt; kein Subprocess + kein Audit.
- Timeout / Schema-Verstoss / kaputtes JSON -&gt; Audit-Status
  ERROR bzw. INVALID_OUTPUT, kein `AiSuggestion` ohne Audit-Bezug.
- PENDING-Audit existiert immer **vor** Subprocess-Aufruf.

## Architektur

- `ModulgrenzenTest` + `SpringBeanKonstruktorTest` -&gt; gruen.
- Subprocess-Logik im LLM-Gateway, Reachability-Logik in
  `cvm-ai-services`. CLAUDE.md-Modulgrenzen unverletzt.
