# Iteration 36 – Plan

**Thema**: CVE-Detailseite mit Findings und Assessments
**Jira**: CVM-80
**Datum**: 2026-04-19

## Ziel

Backend-Read-Endpunkt + Angular-Route, der fuer eine CVE-ID
(z.B. `CVE-2017-18640`) die Basis-Daten und die verknuepften
Findings/Assessments liefert. Damit kann ein Reviewer/Admin auf der
CVE-Uebersicht auf eine CVE klicken und sehen:
- Scan-Treffer (Finding-Liste mit Scan-Id, Komponente, detectedAt)
- Aktuelle Assessments (latest Version pro Finding, Severity,
  Status, ProposalSource)

## Scope

### Backend

- Neue Records:
  - `CveFindingEntry` - Finding-Zusammenfassung.
  - `CveAssessmentEntry` - latest Assessment pro Finding.
  - `CveDetailView` - Basis + Findings + Assessments.
- `CveQueryService.findDetail(String cveId)` liefert den Detail-View.
- `CvesController.detail(String cveId)` -> GET
  `/api/v1/cves/{cveId}` (bestehende List-Route bleibt).

### Frontend

- `CvesService.detail(cveId)` ruft GET.
- Neue Komponente `CveDetailComponent` unter `/cves/:cveId`.
- Link-Setzung in `CvesComponent`: CVE-ID wird anklickbar.

## Tests

- `CveQueryServiceDetailTest` - Service liefert leere Listen fuer
  unbekannte CVE, liefert Daten, wenn Findings/Assessments existieren,
  waehlt pro Finding die juengste Assessment-Version.
- Controller-Test: 404 bei unbekannter CVE, 200 sonst.
- Frontend: Service-Spec + Komponenten-Spec (Karma).

## Stopp-Kriterien

- Keine Arch-Regel-Verletzung.
- Keine bestehenden Tests rot.
- Sprung Queue <-> CVE-Detail optional (folge-Iteration).
