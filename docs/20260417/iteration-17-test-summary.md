# Iteration 17 - KI-Regel-Extraktion - Test-Summary

**Jira**: CVM-42
**Datum**: 2026-04-18

## Testlauf

```
./mvnw -T 1C test  BUILD SUCCESS  (~73 s)
```

## Neue Tests (+20)

### `cvm-ai-services` (+14 = jetzt 79)
| Testklasse | # | Kurz |
|---|---:|---|
| `AssessmentClustererTest` | 4 | Mindestgroesse ("weniger als fuenf erzeugt keinen Vorschlag"), happy cluster, Severity-Trennung, Determinismus. |
| `DryRunEvaluatorTest` | 5 | leere Historie, 100%-Coverage, Konfliktliste, invalid JSON, 0 Treffer. |
| `RuleSuggestionServiceTest` | 5 | Approve erzeugt Rule, **Vier-Augen**, nicht-PROPOSED, Reject, leerer Kommentar. |

### `cvm-api` (+6)
| Testklasse | # | Kurz |
|---|---:|---|
| `RuleSuggestionsControllerWebTest` | 6 | Liste, Approve, Vier-Augen 409, Reject, 400, 404. |

## Gesamt-Testlage

| Modul | Gruen | Skipped | Rot |
|---|---:|---:|---:|
| cvm-domain | 4 | 0 | 0 |
| cvm-persistence | 0 | 6 | 0 |
| cvm-application | 126 | 0 | 0 |
| cvm-integration | 13 | 0 | 0 |
| cvm-llm-gateway | 61 | 0 | 0 |
| cvm-ai-services | **79** | 0 | 0 |
| cvm-api | **51** | 0 | 0 |
| cvm-app | 0 | 5 | 0 |
| cvm-architecture-tests | 8 | 0 | 0 |
| **Gesamt** | **342** | 11 | 0 |

## Sicherheits-Invarianten

- **Mindestgroesse** fuer Cluster (&ge; 5 Assessments + 3 distinkte CVEs).
- **Vier-Augen** bei Approve - approver != suggester.
- **Kein Auto-Activate** - Rule entsteht ausschliesslich nach
  Admin-Approve.
- **Override-Rueckkopplung** setzt aktive AI-Regel auf DRAFT.
- **Condition-Schema**: kaputtes JSON -&gt; leerer Dry-Run statt
  Exception.

## Architektur

- `ModulgrenzenTest` + `SpringBeanKonstruktorTest` gruen.
- `api -&gt; persistence` bleibt strikt: Controller nutzt
  `RuleSuggestionView` und `ApproveRuleResult` aus
  `cvm-ai-services`, nie direkt die JPA-Entities.
