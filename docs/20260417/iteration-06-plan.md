# Iteration 06 – Plan (Bewertungs-Workflow, CVM-15)

*Stand: 2026-04-17*

## Ziel
Menschlicher Bewerter arbeitet die Queue ab. Cascade aus Iteration 05 wird an
die Persistenz angebunden: pro Finding entsteht genau ein PROPOSED-Assessment
(REUSE-Treffer direkt APPROVED-Fortschreibung). Vier-Augen-Prinzip greift bei
Downgrade auf `NOT_APPLICABLE` / `INFORMATIONAL`.

## Umfang
1. **Domain**
   - `AssessmentStatus.EXPIRED` ergaenzen.
   - Neue Enums fuer Transitionen bleiben aus; Statusmaschine bleibt intern.

2. **Persistenz**
   - `Assessment` um `valid_until` (nullable) und `reviewed_by` (nullable,
     Zweitbewerter bei Vier-Augen) ergaenzen.
   - Flyway **V0010**: Spalten anlegen, Check-Constraint `ck_assessment_status`
     um `EXPIRED`, `NEEDS_REVIEW` aktualisieren (NEEDS_REVIEW ist aus
     Iteration 04 bereits gesetzt, wird idempotent neu erstellt).
   - Repository: Queue-Query (Assessments mit Status IN (PROPOSED,
     NEEDS_REVIEW) ohne supersededAt, je Umgebung + optionaler
     productVersion/source-Filter), Expiry-Query
     (`valid_until <= :now AND status = APPROVED AND supersededAt IS NULL`).

3. **Application / cvm-application/assessment**
   - `AssessmentStateMachine` (reiner POJO). Uebergangs-Tabelle siehe
     Konzept v0.2 Abschnitt 6.2 plus `06-Bewertungs-Workflow.md`:
     - PROPOSED -> APPROVED | REJECTED | NEEDS_REVIEW | SUPERSEDED
     - APPROVED -> EXPIRED | SUPERSEDED
     - NEEDS_REVIEW -> PROPOSED | SUPERSEDED
     - EXPIRED | REJECTED | SUPERSEDED sind terminal.
     - Invalide Uebergaenge werfen `InvalidAssessmentTransitionException`.
   - `AssessmentFourEyesViolationException` (eigene Klasse; die
     `FourEyesViolationException` aus Profile bleibt unberuehrt).
   - `AssessmentWriteService` mit:
     - `propose(ProposeCommand)` (Auto-Propose aus Cascade, REUSE =
       direktes APPROVED-Fortschreiben als Version+1; RULE =
       PROPOSED-Vorschlag; HUMAN = kein Auto-Write).
     - `manualPropose(ManualProposeCommand)` (Autor setzt eigene Bewertung
       via REST, Status PROPOSED).
     - `approve(UUID, approverId)` (Vier-Augen-Wall; neue Version als
       APPROVED, alte wird SUPERSEDED; optional Mitigation-Plan).
     - `reject(UUID, approverId, Kommentar)` (Kommentar pflicht; neue
       Version REJECTED).
     - `expireIfDue(Instant)` (Scheduler-Einstieg).
   - `AssessmentQueueService` mit `findeOffene(filter)` und Projection
     `FindingQueueView`.
   - `FindingsCreatedListener`: haengt an `ScanIngestedEvent`, iteriert
     Findings, baut `CascadeInput`, ruft `cascadeService.bewerte(...)` und
     schreibt ueber `AssessmentWriteService.propose(...)`.
   - `AssessmentExpiryJob` taeglich 03:00.
   - `AssessmentApprovedEvent` + `AssessmentApprovedLogger`-Listener
     (fuer Iteration 09 Alerts).

4. **REST / cvm-api/assessment + cvm-api/finding**
   - `FindingsController`: `GET /api/v1/findings` mit Filtern
     `status`, `productVersionId`, `environmentId`, `source`.
   - `AssessmentsController`:
     - `POST /api/v1/assessments`
     - `POST /api/v1/assessments/{id}/approve`
     - `POST /api/v1/assessments/{id}/reject`
   - `AssessmentsExceptionHandler`: 400/404/409 deutsche Fehlercodes.
   - Slice-Test-Configs `AssessmentsTestApi`, `FindingsTestApi`.
   - Controller-DTOs + Mapping ueber Application-Views (ArchUnit:
     `cvm-api` darf nicht auf `cvm-persistence`).

## TDD-Reihenfolge
1. `AssessmentStateMachineTest` (pure Unit).
2. `AssessmentWriteServiceTest` (Mockito fuer Repository) - inkl.
   Vier-Augen, REUSE-Fortschreibung, Reject-ohne-Kommentar,
   Expire-Ablaufdatum.
3. `AssessmentQueueServiceTest`.
4. `FindingsCreatedListenerTest` (Mockito).
5. `AssessmentsControllerWebTest` + `FindingsControllerWebTest`
   (`@WebMvcTest` + Slice-Api).
6. `AssessmentExpiryJobTest`.

## Out-of-Scope
- Jira-Integration, UI, Alerts (9), KI-Vorbewertung (13).
- Echte Rollen-Checks (nur `authenticated()`), Security-Verfeinerung ist
  Backlog (offene-punkte.md).

## Risiken / Entscheidungen
- `valid_until`-Default: 12 Monate ab `decided_at`, konfigurierbar ueber
  `cvm.assessment.default-valid-months` (application.yaml); je
  Severity/Umgebung -> Backlog, nicht in dieser Iteration.
- Listener-Order mit CVE-Enrichment: CascadeService nutzt den
  JpaBezogenen Snapshot der CVE; wenn die Anreicherung noch nicht fertig
  ist, greift die Regel-Engine auf den roh ingestierten Stand (kev=false
  etc.). Das ist fuer Iteration 06 akzeptabel, wird in 09/13 verfeinert
  (offene-punkte.md).
- Die Mitigation-Plan-Anlage im selben Call bleibt optional im Request;
  Jira-Keys bleiben Platzhalter.
