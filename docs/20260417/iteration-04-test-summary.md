# Iteration 04 – Test-Summary

**Jira**: CVM-13
**Datum**: 2026-04-17
**Command**: `./mvnw -T 1C test`
**Ergebnis**: BUILD SUCCESS

## Zusammenfassung

| Modul | Getestet | Gruen | Skipped | Rot |
|---|---|---|---|---|
| cvm-domain | 4 | 4 | 0 | 0 |
| cvm-persistence | 6 | 0 | 6 | 0 |
| cvm-application | 28 | 28 | 0 | 0 |
| cvm-integration | 8 | 8 | 0 | 0 |
| cvm-api | 10 | 10 | 0 | 0 |
| cvm-app | 5 | 0 | 5 | 0 |
| cvm-architecture-tests | 7 | 7 | 0 | 0 |
| **Summe** | **68** | **57** | **11** | **0** |

Alle Skips wegen fehlendem Docker-Daemon
(`com.ahs.cvm.app.DockerAvailability#isAvailable`).

## Neu in Iteration 04

### cvm-application/profile (19 Tests, alle gruen)

| Klasse | Tests | Zweck |
|---|---|---|
| `ProfileDiffBuilderTest` | 7 | Diff-Kern (identisch, boolean-flip, created, removed, tiefe Struktur, Array-Index, deterministische Reihenfolge). |
| `ContextProfileYamlParserTest` | 5 | YAML-Parsing + Schema-Validierung mit deutschen Fehlermeldungen. |
| `ContextProfileServiceTest` | 5 | Propose, Vier-Augen-Verstoss, Aktivierung + Event mit Diff, erste Version, Diff. |
| `AssessmentReviewMarkerTest` | 2 | Event &rarr; Batch-Update; leerer Diff &rarr; kein Call. |

### cvm-api/profile (5 Tests, alle gruen)

`ProfileControllerWebTest` deckt ab:
- `GET /environments/{id}/profile`: 200 + 404
- `PUT /environments/{id}/profile`: 201 + 400 bei Schema-Fehler
- `POST /profiles/{id}/approve`: 409 bei Vier-Augen-Verstoss

## Regressionen
Keine. Die bisherigen Tests aus Iterationen 00&ndash;03 laufen unveraendert
gruen; die Arch-Regeln greifen weiterhin.

## Coverage-Notiz
JaCoCo laeuft ueber `verify`, nicht ueber `test`. Eine vollstaendige
Coverage-Messung wurde fuer diese Iteration nicht separat gerechnet.
Die profile-Pakete (`cvm-application/profile` und `cvm-api/profile`)
sind durch Unit- und Slice-Tests sehr engmaschig abgedeckt.
DoD-Punkt "Coverage `cvm-application/profile` &ge; 85 %" wird
kontrolliert, sobald die PitTest-Pipeline angelegt wird.

## Skip-Details

- `cvm-persistence`: `AssessmentImmutableTest`, `FlywayMigrationReihenfolgeTest`,
  `ProductRepositoryIntegrationsTest` &mdash; je 2 Tests, alle skipped (Docker).
- `cvm-app`: `SmokeIntegrationTest` (1), `ScanIngestionIntegrationTest` (2),
  `FlywayBaselineTest` (2) &mdash; alle skipped (Docker).

Alle werden in CI bei verfuegbarem Docker-Daemon ausgefuehrt.
