# Iteration 06 – Bewertungs-Workflow (Queue, Approve, Vier-Augen)

**Jira**: CVM-15
**Abhängigkeit**: 02, 05
**Ziel**: Der menschliche Bewerter kann die Queue bearbeiten: Vorschläge
übernehmen, überschreiben, ablehnen. Vier-Augen-Prinzip für Downgrades greift.

---

## Kontext
Konzept v0.2 Abschnitt 6.2. Die Cascade aus Iteration 05 wird hier in den
Scan-Ingest-Flow eingehängt. Assessments entstehen mit `PROPOSED` (REUSE-Treffer
unmittelbar `RESOLVED_BY_ASSESSMENT`).

## Scope IN
1. Integration der Cascade in Scan-Ingest-Pipeline aus Iteration 02:
   Event-Listener `FindingsCreatedListener` → `CascadeService` → Persistenz
   von Assessments im Status `PROPOSED`.
2. Statusmaschine `Assessment`:
   `PROPOSED` → `APPROVED` | `REJECTED` | `NEEDS_REVIEW`
   `APPROVED` → `EXPIRED` (Scheduler auf `validUntil`)
   `NEEDS_REVIEW` → `PROPOSED`.
   Zustandsübergänge zentral in `AssessmentStateMachine`, invalide
   Übergänge werfen `InvalidAssessmentTransitionException`.
3. REST-Endpunkte:
   - `GET /api/v1/findings?status=PROPOSED&productVersionId=&environmentId=&source=`
     (Queue, Pagination, Sortierung nach Severity dann Alter).
   - `POST /api/v1/assessments` mit Body `{findingId, ahsSeverity, rationale, strategy, plannedFixDate}`
   - `POST /api/v1/assessments/{id}/approve` (Zweitfreigabe, Vier-Augen).
   - `POST /api/v1/assessments/{id}/reject` (Kommentar pflicht).
4. Vier-Augen-Wall:
   - Downgrade auf `NOT_APPLICABLE` oder `INFORMATIONAL` → `PROPOSED` bleibt,
     bis `approve` von anderem User mit Rolle `CVE_APPROVER` erfolgt.
   - Alle anderen Einstufungen: Autor und Approver dürfen gleich sein
     (Single-Step-Approval).
5. `validUntil`-Logik: Default 12 Monate, konfigurierbar pro
   Umgebung/Severity.
6. Mitigation-Plan-Anlage im selben Call (Strategy, Ziel-Release,
   geplantes Fix-Datum). Kein Jira in dieser Iteration (Platzhalter).

## Scope NICHT IN
- Jira-Integration (später)
- Alerts (Iteration 09)
- KI-Vorbewertung (Iteration 13)
- UI (Iteration 08)

## Aufgaben
1. `AssessmentWriteService` mit klaren Methoden `propose`, `approve`,
   `reject`, `expireIfDue`.
2. Versioning: bei `approve` entsteht neues `APPROVED`-Assessment,
   das `PROPOSED` wird `SUPERSEDED` markiert.
3. Assessment-Immutable-Invariante aus Iteration 01 wird hier aktiv
   genutzt.
4. Query-Service mit kompakten Projections (Queue-Einträge sind leicht).
5. Scheduler `AssessmentExpiryJob` (täglich 03:00): markiert abgelaufene
   Assessments als `EXPIRED`, triggert Re-Vorschlag für nächsten Scan.
6. Event `AssessmentApprovedEvent` → Listener füllt spätere Alert-Wege
   (Iteration 09). In dieser Iteration nur Logger-Listener.

## Test-Schwerpunkte
- Statusmaschinen-Test (tabellengetrieben: jede Kombination valider/invalider
  Übergänge).
- `FourEyesTest`: Autor=Approver bei Downgrade → Ablehnung; unterschiedliche
  User → Freigabe.
- Integrationstest: Scan-Ingest aus Iteration 02 + Cascade + Queue →
  `GET /findings?status=PROPOSED` liefert korrekte Einträge.
- REUSE-Test: vorhandenes APPROVED-Assessment für (CVE, PV, Env) →
  neuer Scan erzeugt `RESOLVED_BY_ASSESSMENT` ohne Queue-Eintrag.
- `@DisplayName`: `@DisplayName("Vier-Augen: Downgrade auf NOT_APPLICABLE durch Autor schlaegt fehl")`

## Definition of Done
- [ ] Vollständiger Bewertungs-Workflow via REST bedienbar.
- [ ] Vier-Augen greift nachweislich.
- [ ] Scheduler läuft, Expiry nachweislich getestet.
- [ ] Coverage `cvm-application/assessment` ≥ 90 %.
- [ ] Fortschrittsbericht.
- [ ] Commit: `feat(assessment): Bewertungs-Workflow mit Cascade und Vier-Augen\n\nCVM-15`

## TDD-Hinweis
Beginne mit der Statusmaschine (reiner Unit-Test, keine Spring-Abhängigkeit).
Dann Service, dann Controller. **Ändere NICHT die Tests** bei Rot.

## Abschlussbericht
Standard, plus Statusmaschinen-Diagramm als `docs/konzept/status-assessment.mmd` (Mermaid).
