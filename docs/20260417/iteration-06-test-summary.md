# Iteration 06 – Test-Summary

**Stand**: 2026-04-17, nach `./mvnw -T 1C test`.

## Aggregat
- **148 Tests**, **137 gruen**, **0 rot**, **11 geskippt** (Docker).
- BUILD SUCCESS.

## Neu in Iteration 06 (alle gruen)

### `cvm-application/assessment`
| Test | Faelle | Zweck |
|---|---|---|
| `AssessmentStateMachineTest` | 18 | Tabellengetrieben: zulaessige und unzulaessige Uebergaenge, Terminal-Erkennung. |
| `AssessmentWriteServiceTest` | 10 | Cascade-Propose (REUSE/RULE/HUMAN), `manualPropose`, `approve` (single-step + Vier-Augen), `reject` (Kommentar pflicht), `approveMitMitigation`. |
| `AssessmentQueueServiceTest` | 3 | Queue-Filter (env, Status), Sperre fuer ungueltige Status. |
| `AssessmentExpiryJobTest` | 1 | Scheduler delegiert an `expireIfDue(Instant)`. |
| `FindingsCreatedListenerTest` | 2 | Cascade pro Finding; Fehlertoleranz beim Throw eines Cascade-Calls. |

### `cvm-api`
| Test | Faelle |
|---|---|
| `AssessmentsControllerWebTest` | 5 (POST /assessments 201, Validation 400, approve 200/409, reject 404). |
| `FindingsControllerWebTest` | 2 (Queue-Liste, Status-Filter). |

## Coverage
JaCoCo wurde mit jeder Modul-Build-Phase mitgelaufen
(`prepare-agent`-Goal). Konkrete Coverage-Auswertung folgt nach dem
`verify`-Lauf in einer separaten Iteration. Heuristik aus dem Surefire-
Bericht: jede neue Klasse hat mindestens einen Pfad-Test, die kritischen
Methoden (StateMachine, Vier-Augen, REUSE-Fortschreibung) jeweils
mehrere.

## Bekanntes Skip-Verhalten
- `cvm-persistence` Integrationstests
  (`AssessmentImmutableTest`, `FlywayMigrationReihenfolgeTest`,
  `ProductRepositoryIntegrationsTest`): pgvector-Container nicht
  verfuegbar.
- `cvm-app` Integrationstests
  (`FlywayBaselineTest`, `SmokeIntegrationTest`,
  `ScanIngestionIntegrationTest`): Postgres-Container nicht verfuegbar.
- Skip-Mechanik: `@EnabledIf("com.ahs.cvm.testsupport.DockerAvailability#isAvailable")`.

## ArchUnit
`ModulgrenzenTest` 7/7 gruen. `cvm-api -> cvm-persistence` bleibt
verboten; das neue `FindingQueueView` (Application) und
`AssessmentResponse` (API) halten diesen Schnitt ein.

## Naechster Test-Block
- Persistenz-IT fuer V0010 + Queue-Query (Postgres-Container).
- E2E-Test: Scan-Ingest &rarr; Cascade &rarr; `GET /findings` mit Docker.
- Pitest fuer `AssessmentStateMachine` und Vier-Augen-Logik
  (Konzept-Vorgabe 100 % Mutation Survival).
