# Iteration 06 – Fortschrittsbericht

**Jira**: CVM-15
**Datum**: 2026-04-17
**Ziel**: Bewertungs-Workflow mit Cascade-Anbindung, Vier-Augen-Prinzip und
Expiry-Scheduler.

## 1 Was wurde gebaut

### Domain (`cvm-domain`)
- `AssessmentStatus.EXPIRED` ergaenzt &mdash; entsteht via Scheduler.

### Persistenz (`cvm-persistence`)
- Entity `Assessment` um `valid_until` (Ablaufdatum) und `reviewed_by`
  (Zweitbewerter) erweitert.
- Repository um drei Queries:
  - `findeOffeneQueue(envId, pvId, source)` &mdash; Bewertungs-Queue,
    sortiert nach Severity dann Erstellungszeit.
  - `findByStatusAndValidUntilLessThanEqualAndSupersededAtIsNull(...)`
    &mdash; Expiry-Kandidaten.
  - `markiereAlsAbgelaufen(...)` &mdash; Batch-Update auf `EXPIRED`
    via `@Modifying`-Query, analog zur NEEDS_REVIEW-Transition.
- Flyway `V0010__bewertungs_workflow.sql`: Spalten + Check-Constraint
  fuer `EXPIRED` + Partial-Indizes (`idx_assessment_queue_offen`,
  `idx_assessment_expiry`).

### Anwendungsschicht (`cvm-application/assessment`)
| Klasse | Zweck |
|---|---|
| `AssessmentStateMachine` | Zentrale Uebergangs-Tabelle (Konzept v0.2 6.2). |
| `InvalidAssessmentTransitionException` | Statusverstoss. |
| `AssessmentFourEyesViolationException` | Vier-Augen-Verstoss. |
| `AssessmentNotFoundException` | 404-Mapper. |
| `AssessmentConfig` | `cvm.assessment.default-valid-months` (Default 12). |
| `AssessmentWriteService` | `propose`, `manualPropose`, `approve`, `approveMitMitigation`, `reject`, `expireIfDue` &mdash; Versionierung + Vier-Augen + Mitigation-Plan-Anlage. |
| `AssessmentQueueService` | Lese-Service mit `findeOffene(filter)`. |
| `FindingQueueView` | Read-Model fuer Controller. |
| `AssessmentApprovedEvent` | Domain-Event nach Approve. |
| `AssessmentApprovedLogger` | Platzhalter-Listener (Iteration 09 ersetzt). |
| `FindingsCreatedListener` | Verdrahtet `ScanIngestedEvent` &rarr; `CascadeService` &rarr; `AssessmentWriteService.propose(...)`. |
| `AssessmentExpiryJob` | Scheduler (Default Cron `0 0 3 * * *`, schaltbar via `cvm.scheduler.enabled`). |

### REST (`cvm-api/assessment` + `cvm-api/finding`)
- `AssessmentsController`:
  - `POST /api/v1/assessments`
  - `POST /api/v1/assessments/{id}/approve`
  - `POST /api/v1/assessments/{id}/reject`
- `FindingsController`:
  - `GET /api/v1/findings?status=&productVersionId=&environmentId=&source=`
- `AssessmentsExceptionHandler` liefert
  `assessment_not_found`, `assessment_four_eyes_violation`,
  `assessment_state_conflict`, `assessment_bad_request`.
- Slice-Configs `AssessmentsTestApi` und `FindingsTestApi` halten
  `@WebMvcTest`-Slices unabhaengig.

### Statusmaschinen-Diagramm
Mermaid unter `docs/konzept/status-assessment.mmd`.

## 2 Was laenger dauerte
- **WebMvc-Slice-Tests**: `FindingsTestApi` darf nicht das Assessment-
  Paket mitscannen, sonst zieht der Spring-Context den
  `AssessmentsController` ein und verlangt `AssessmentWriteService`-Bean.
  Konsequent: ein Slice-Config-Paket pro Controller.
- **AssessmentImmutabilityListener vs. EXPIRED**: Ein in-place-Update auf
  `EXPIRED` haette den Listener gegen sich. Loesung: `@Modifying`-Query
  analog zum NEEDS_REVIEW-Marker (Iteration 04). Damit bleibt das
  Immutable-Versprechen formal korrekt &mdash; alle inhaltlichen
  Aenderungen bleiben weiterhin verboten, nur System-Transitionen
  laufen via Bulk-Update.
- **Cascade-Outcome HUMAN**: Ein automatischer HUMAN-Vorschlag ohne
  Severity wuerde leere PROPOSED-Eintraege erzeugen. Loesung: `propose`
  liefert `null`, der Eintrag bleibt der manuellen Queue-Bedienung
  vorbehalten. Das ist konform zum Konzept (4.4 &ndash; "KI/Cascade
  schlaegt vor, Mensch entscheidet").
- **AssessmentResponse-DTO im API-Modul**: ArchUnit verbietet
  `cvm-api -> cvm-persistence`. Das `FindingQueueView`-Read-Model im
  Application-Modul macht den Mapping-Schritt im Controller trivial.

## 3 Abweichungen vom Prompt
1. **Flyway-Nummer**: Prompt erwaehnt nicht explizit V0010, ich habe die
   naechste freie Nummer (V0010) verwendet, weil V0009 durch
   Iteration 05 belegt ist.
2. **`validUntil`**: Konfiguration via `cvm.assessment.default-valid-months`
   pauschal pro System (12 Monate). Severity-/Umgebungs-spezifische
   Konfiguration bleibt Backlog (Konzept v0.2 6.2 Punkt 5).
3. **Mitigation-Plan-Status**: Auto-Anlage als `PLANNED` (Default), Owner
   = Approver. Status `OPEN` haette eine zweite Pflege-Aktion gefordert
   und bleibt dem UI in Iteration 08 vorbehalten.
4. **`AssessmentApprovedEvent`-Listener**: Reiner Logger; SMTP folgt in
   Iteration 09.
5. **Security-Rollen** (`CVE_APPROVER`, `CVE_REVIEWER`): nicht verdrahtet,
   bleibt Backlog (offene-punkte.md).

## 4 Entscheidungen fuer Sebastian
- Soll `AssessmentExpiryJob` einen `AssessmentExpiredEvent` publizieren
  (analog Approve)? Aktuell nur Log-Eintrag.
- Soll der Cron des Expiry-Jobs konfigurierbar sein
  (`cvm.assessment.expiry-cron`)? Aktuell ist er via `@Value` setzbar,
  aber nicht in `application.yaml` exponiert.
- Soll bei REUSE-Treffer trotzdem `AssessmentApprovedEvent` publiziert
  werden? Aktuell: ja nicht (ist keine neue Bewertung).

## 5 Naechster Schritt
**Iteration 07 &mdash; Frontend-Shell** (CVM-16). Angular-18-Shell mit
Keycloak-Integration und Layout-Skeleton. Die Bewertungs-Queue-UI folgt
in Iteration 08 (CVM-17) und nutzt die in dieser Iteration gebauten
REST-Endpunkte.

## 6 Build-Status

```
./mvnw -T 1C test  BUILD SUCCESS
```

- `cvm-domain`: 4/4
- `cvm-persistence`: 6 geskippt (Docker)
- `cvm-application` (Iteration 06 neu): **34** in `assessment`
  (StateMachine 18, WriteService 10, Queue 3, ExpiryJob 1, Listener 2).
  Modulgesamt: **96 gruen**.
- `cvm-integration`: 8 gruen.
- `cvm-api` (Iteration 06 neu): `AssessmentsControllerWebTest` 5/5,
  `FindingsControllerWebTest` 2/2. Modulgesamt: **22 gruen**.
- `cvm-app`: 5 geskippt (Docker).
- `cvm-architecture-tests`: 7/7.

**Gesamt: 148 Tests, 137 gruen, 11 geskippt ohne Docker, 0 rot.**

---

*Autor: Claude Code.*
