# Iteration 13 - KI-Vorbewertung - Test-Summary

**Jira**: CVM-32
**Datum**: 2026-04-18

## Testlauf

```
./mvnw -T 1C test  BUILD SUCCESS  (~70 s)
```

## Neue Tests (Iteration 13)

### `cvm-application` (+4)
| Testklasse | Anzahl | Kurz |
|---|---:|---|
| `CascadeServiceAiStageTest` | 3 | AI-Port liefert / leer / Fehler. |
| `CascadeServiceTest` | (Backwards) | weiterhin gruen mit `Optional.empty()`-Port. |

### `cvm-ai-services` (+7)
| Testklasse | Anzahl | Kurz |
|---|---:|---|
| `AutoAssessmentOrchestratorTest` | 7 | deaktiviert; Happy-Path; **konservativer Default**; **Halluzinations-Check** (3 Faelle); Service-Fehler. |

## Gesamt-Testlage

| Modul | Gruen | Skipped | Rot |
|---|---:|---:|---:|
| cvm-domain | 4 | 0 | 0 |
| cvm-persistence | 0 | 6 | 0 |
| cvm-application | **126** | 0 | 0 |
| cvm-integration | 8 | 0 | 0 |
| cvm-llm-gateway | 52 | 0 | 0 |
| cvm-ai-services | **27** | 0 | 0 |
| cvm-api | 30 | 0 | 0 |
| cvm-app | 0 | 5 | 0 |
| cvm-architecture-tests | 8 | 0 | 0 |
| **Gesamt** | **255** | 11 | 0 |

## Sicherheits-Invarianten (durch Tests gehaerttet)

- `AutoAssessmentOrchestratorTest.konservativerDefault`:
  Severity bleibt auf Original-Wert wenn Datenlage duenn.
- `AutoAssessmentOrchestratorTest.halluzination` +
  `halluzinationOhneAdvisoryVersion`: NEEDS_VERIFICATION wenn
  Modell eine Fix-Version nennt, die nicht belegbar ist.
- `AutoAssessmentOrchestratorTest.deaktiviert`: Flag aus -&gt; kein
  LLM-Call.
- `CascadeServiceAiStageTest.aiPortFehler`: KI-Ausfall darf den
  Cascade nicht killen - faellt auf MANUAL.

## Architektur

- `ModulgrenzenTest` (7) + `SpringBeanKonstruktorTest` (1) gruen.
- `cvm-application` haengt weiterhin nur von `domain`, `persistence`
  ab (nicht von `cvm-llm-gateway`).
- KI-Logik bleibt in `cvm-ai-services`, ueber Port-Interface
  in `cvm-application` aufrufbar.
