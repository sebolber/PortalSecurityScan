# Iteration 87 - Plan: Assessment-Audit-Trail (U-04b)

**Jira**: CVM-327

## Ziel

Im Queue-Detail die komplette Historie aller Assessment-Versionen
eines Findings sichtbar machen (BSI/DSGVO-Traceability).

## Umfang

### Backend
- `AssessmentQueueService.findHistorieByFinding(UUID)`
  (`@Transactional(readOnly=true)`), delegiert an
  `AssessmentRepository.findByFindingIdOrderByVersionAsc`.
- Neuer Endpoint `GET /api/v1/findings/{id}/assessments/history`
  in `FindingsController`.
- Neuer MockMvc-Test in `FindingsControllerWebTest`.

### Frontend
- `QueueApiService.history(findingId)`.
- `QueueDetailComponent` bekommt drei neue Signale:
  `history`, `historyLaedt`, `historyCount`.
- Ein `<details>`-Collapsible am Ende des Detail-Panels. Beim
  Oeffnen (`(toggle)`-Event) wird die Historie via API
  lazy-geladen, bis der naechste Entry-Wechsel sie wieder
  zuruecksetzt.
- Versionen werden kompakt gerendert (v{version}, Severity-Badge,
  Status, Source, Rationale, decidedBy, Zeit).

## Tests

- Backend-MockMvc: 200-Case mit zwei Eintraegen.
- Frontend: Karma-Case oeffnet das `<details>` und prueft dass
  `queueApi.history` aufgerufen wird und die Liste gesetzt ist.
- Bestehende queue-detail-Cases bleiben unveraendert.

## Abnahme

- Karma gruen.
- `./mvnw -T 1C test` BUILD SUCCESS.
