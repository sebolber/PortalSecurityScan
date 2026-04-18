# Iteration 14 - Copilot + Delta-Summary - Test-Summary

**Jira**: CVM-33
**Datum**: 2026-04-18

## Testlauf

```
./mvnw -T 1C test  BUILD SUCCESS  (~74 s)
```

## Neue Tests

### `cvm-ai-services` (+15 = jetzt 42)
| Testklasse | Anzahl | Kurz |
|---|---:|---|
| `CopilotServiceTest` | 7 | je Use-Case + **Severity-wird-niemals-gesetzt** + leere Instruction + unbekanntes Assessment. |
| `ScanDeltaCalculatorTest` | 4 | Initial, neu/entfallen/shift, KEV-Aenderung, identisch. |
| `ScanDeltaSummaryServiceTest` | 4 | Initial-Run; Diff unter Schwelle; ueber Schwelle; unbekannter Scan. |

### `cvm-api` (+5 = jetzt 35)
| Testklasse | Anzahl | Kurz |
|---|---:|---|
| `CopilotControllerWebTest` | 2 | NDJSON-Stream (text+sources Zeilen); 400 bei leerer Instruction. |
| `DeltaSummaryControllerWebTest` | 3 | audience=short; audience=long; 404. |

## Gesamt-Testlage

| Modul | Gruen | Skipped | Rot |
|---|---:|---:|---:|
| cvm-domain | 4 | 0 | 0 |
| cvm-persistence | 0 | 6 | 0 |
| cvm-application | 126 | 0 | 0 |
| cvm-integration | 8 | 0 | 0 |
| cvm-llm-gateway | 52 | 0 | 0 |
| cvm-ai-services | **42** | 0 | 0 |
| cvm-api | **35** | 0 | 0 |
| cvm-app | 0 | 5 | 0 |
| cvm-architecture-tests | 8 | 0 | 0 |
| **Gesamt** | **275** | 11 | 0 |

## Sicherheits-Invarianten

- **Copilot setzt nie Severity** (`CopilotServiceTest.severityWirdNiemalsGesetzt`
  + Reflection-Test auf Felder von `CopilotSuggestion`).
- **Delta-Summary ohne LLM-Call bei Initial-Run / Diff unter Schwelle**.
- **Audit-Pflicht**: alle Copilot- und DeltaSummary-Calls laufen
  durch den `AiCallAuditService` (Use-Case-Label
  `COPILOT_<USECASE>` bzw. `DELTA_SUMMARY`).

## Architektur

- `ModulgrenzenTest` + `SpringBeanKonstruktorTest` -&gt; gruen.
- Copilot- und Summary-Service leben in `cvm-ai-services`,
  Controller in `cvm-api`. Keine Modulverletzung.
