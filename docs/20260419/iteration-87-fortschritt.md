# Iteration 87 - Fortschritt: Assessment-Audit-Trail (U-04b)

**Jira**: CVM-327

## Was wurde gebaut

### Backend
- `AssessmentQueueService.findHistorieByFinding(UUID findingId)`.
- Neuer Endpoint
  `GET /api/v1/findings/{findingId}/assessments/history`.
- `FindingsControllerWebTest` um einen Case
  `historyLiefertAlleAssessments` erweitert (3 Tests jetzt).

### Frontend
- `QueueApiService.history(findingId)` neuer Service-Call.
- `QueueDetailComponent`:
  - Signale `history`, `historyLaedt`, `historyCount`.
  - `onHistoryToggle(event)` laedt lazy beim Oeffnen des
    `<details>`-Collapsibles; Merkt sich `historyFindingId`, um
    den Call nicht doppelt auszuloesen; wird bei Entry-Wechsel
    zurueckgesetzt.
  - Template am Ende des Panel-Bodys: `<details>` mit History-
    Icon und Zahl der Versionen; beim Oeffnen die Liste aller
    Versionen (v{n}, Severity-Badge, Status, Source, Rationale,
    decidedBy, Zeit).
- Neuer Karma-Case: oeffnen des `<details>` ruft `history` mit
  findingId auf und speichert die Liste.

## Ergebnisse

- `./mvnw -T 1C test` BUILD SUCCESS in 01:29 min.
- `FindingsControllerWebTest` 3 Tests SUCCESS.
- Karma 127 Tests SUCCESS (+ 1 neu).
- `ng lint` / `ng build` gruen.

## Migrations / Deployment

- Kein neues Backend-Dependency, keine Flyway-Migration.
- Neuer Endpoint wirkt sofort nach Deploy.
