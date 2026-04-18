# Iteration 18 - Anomalie-Check + Profil-Assistent - Test-Summary

**Jira**: CVM-43
**Datum**: 2026-04-18

## Testlauf

```
./mvnw -T 1C test  BUILD SUCCESS  (~67 s)
```

## Neue Tests (+18)

### `cvm-ai-services` (+13 = jetzt 92)
| Testklasse | # | Kurz |
|---|---:|---|
| `AnomalyDetectionServiceTest` | 7 | KEV+NA loest WARNING aus (happy), KEV+niedriges EPSS kein Event, MANY_ACCEPT_RISK, SIMILAR_TO_REJECTED, BIG_DOWNGRADE, Flag-aus, Dedup. **Invariante**: kein assessmentRepo.save. |
| `ProfileAssistantServiceTest` | 6 | deaktiviert, start, reply, finalize erzeugt Draft, kein Direkt-Schreib, Session-Timeout. |

### `cvm-api` (+5 = jetzt 57)
| Testklasse | # | Kurz |
|---|---:|---|
| `AnomalyControllerWebTest` | 2 | GET Liste, GET Count. |
| `ProfileAssistantControllerWebTest` | 4 | start, reply, finalize, 404. |

## Gesamt-Testlage

| Modul | Gruen | Skipped | Rot |
|---|---:|---:|---:|
| cvm-domain | 4 | 0 | 0 |
| cvm-persistence | 0 | 6 | 0 |
| cvm-application | 126 | 0 | 0 |
| cvm-integration | 13 | 0 | 0 |
| cvm-llm-gateway | 61 | 0 | 0 |
| cvm-ai-services | **92** | 0 | 0 |
| cvm-api | **57** | 0 | 0 |
| cvm-app | 0 | 5 | 0 |
| cvm-architecture-tests | 8 | 0 | 0 |
| **Gesamt** | **361** | 11 | 0 |

## Sicherheits-Invarianten

- Anomalie-Service schreibt kein Assessment (Mock-verify
  `never()`).
- Profil-Assistent ruft nur `proposeNewVersion` am
  `ContextProfileService` (Mockito `verifyNoMoreInteractions`).
- Session-TTL 24 h erzwungen; abgelaufene Sessions werden
  automatisch auf EXPIRED gehoben.
- Feature-Flags (`cvm.ai.anomaly.enabled`,
  `cvm.ai.profile-assistant.enabled`) Default `false`.

## Architektur

- `ModulgrenzenTest` + `SpringBeanKonstruktorTest` gruen.
- `api -&gt; persistence` unverletzt: Controller nutzen
  `AnomalyView` und `StartResult`/`ReplyResult`/`FinalizeResult`
  aus `cvm-ai-services`.
