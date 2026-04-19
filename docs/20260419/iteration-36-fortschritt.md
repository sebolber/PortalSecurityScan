# Iteration 36 – Fortschritt

**Thema**: CVE-Detailseite mit Findings und Assessments
**Jira**: CVM-80
**Datum**: 2026-04-19

## Umgesetzt

### Backend
- Neuer View-Record `CveDetailView` (+ Inner-Records
  `FindingEntry`, `AssessmentEntry`).
- `CveQueryService.findDetail(cveId)` aggregiert CVE-Metadaten,
  Finding-Liste und das juengste Assessment pro Finding.
- `CvesController` um `GET /api/v1/cves/{cveId}` erweitert (404 bei
  unbekannter CVE).
- Mockito-Test `CveQueryServiceDetailTest` (4 Faelle): leere
  Eingabe, unbekannte CVE, CVE ohne Findings, juengstes Assessment
  pro Finding.

### Frontend
- `CvesService.detail(cveId)` + neue Typen `CveDetailView`,
  `CveFindingEntry`, `CveAssessmentEntry`.
- Neue Komponente `CveDetailComponent` unter Route
  `/cves/:cveId`. Drei Cards: Metadaten, Findings, Assessments.
  Zurueck-Button nach `/cves`.
- CVE-Liste verlinkt die CVE-ID per `[routerLink]` auf die
  Detailseite.

## Test-Status

- `./mvnw -T 1C test`: BUILD SUCCESS, alle Module gruen.
- `npx ng build`: gruen.
- `npx ng lint`: "All files pass linting."

## Offene Punkte

- Cross-Link von Queue-Detail nach CVE-Detail (folgt als separate
  Iteration).
- CVE-Detail zeigt Findings auch fuer `REUSE`-Assessments. Sobald
  eine "Status-Gruppierung" gewuenscht ist, Filter/Group-By im UI.
