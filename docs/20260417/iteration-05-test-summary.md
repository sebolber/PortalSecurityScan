# Iteration 05 – Test-Summary

**Jira**: CVM-14
**Datum**: 2026-04-17
**Command**: `./mvnw -T 1C test`
**Ergebnis**: BUILD SUCCESS

## Zusammenfassung

| Modul | Getestet | Gruen | Skipped | Rot |
|---|---|---|---|---|
| cvm-domain | 4 | 4 | 0 | 0 |
| cvm-persistence | 6 | 0 | 6 | 0 |
| cvm-application | 62 | 62 | 0 | 0 |
| cvm-integration | 8 | 8 | 0 | 0 |
| cvm-api | 15 | 15 | 0 | 0 |
| cvm-app | 5 | 0 | 5 | 0 |
| cvm-architecture-tests | 7 | 7 | 0 | 0 |
| **Summe** | **107** | **96** | **11** | **0** |

Alle Skips wegen fehlendem Docker-Daemon (`DockerAvailability#isAvailable`).

## Neu in Iteration 05

### cvm-application (34 neue gruene Tests)

| Klasse | Tests | Zweck |
|---|---|---|
| `ConditionParserTest` | 7 | eq-Basisfall, alle 8 skalaren Operatoren, verschachtelte Logik, unbekannter Pfad/Operator, leeres `all`, `between`-Validierung. |
| `RuleEvaluatorTest` | 11 | eq/gt/between/containsAny/matches, profile-Pfad, not/all/any, fehlendes Feld, Typ-Mismatch. |
| `RationaleTemplateTest` | 3 | Tokens ersetzt, profile-Pfad, unbekanntes Token. |
| `RuleEngineTest` | 3 | erste passende ACTIVE-Regel gewinnt, keine Regeln, defektes JSON. |
| `RuleServiceTest` | 5 | Draft, ungueltiges Condition-JSON, Vier-Augen-Verstoss, Aktivierung, nicht-DRAFT-Zustand. |
| `DryRunTest` | 2 | Coverage ueber 20 synthetische Findings, Konflikterkennung bei Severity-Mismatch. |
| `CascadeServiceTest` | 3 | REUSE-Kurzschluss, RULE nach REUSE-Miss, HUMAN-Fallback. |

### cvm-api (5 neue gruene Tests)

`RulesControllerWebTest`:
- `GET /rules`
- `POST /rules` (201 Created)
- `POST /rules` mit ungueltiger Condition (400)
- `POST /rules/{id}/activate` mit Vier-Augen-Verstoss (409)
- `POST /rules/{id}/dry-run` (200 mit Coverage)

## Regressionen
Keine. Die bestehenden 57 gruenen Tests aus Iterationen 00&ndash;04
laufen unveraendert; Arch-Regeln greifen weiterhin (`api -> persistence`
wird durch `RuleView`/`RuleResponse` eingehalten).

## Coverage-Notiz
Die Kernlogik (`ConditionParser`, `RuleEvaluator`, `PathResolver`,
`RationaleTemplateInterpolator`, `CascadeService`) ist ueber Happy-Path
und Fehlerpfade sehr dicht abgedeckt. Eine formale JaCoCo-Messung
(Schwellwert &ge; 90 % im DoD) bleibt der CI-Messung vorbehalten, die
den `verify`-Lifecycle laeuft.
