# Iteration 94 - Plan: Dashboard als Handlungszentrale (U-08 Teil 2)

**Jira**: CVM-334

## Ziel

Das Dashboard zeigt ausser den vorhandenen KPIs und
Handlungskarten zwei neue Handlungs-Cards:

1. **T2-Eskalation**: Ampel aus `AlertBannerService.status()`.
   Bei sichtbarem Banner wird die Anzahl und der T2-Zeitschwellwert
   mit CTA "Zur Queue" angezeigt; sonst eine LOW-Ampel.
2. **Zuletzt erzeugte Reports**: Liste der 5 neuesten Reports per
   `ReportsService.list({ size: 5 })`, rollen-gefiltert (ADMIN,
   REPORTER, VIEWER).

## Umfang

### Dashboard-Component
- Neue Imports: `DatePipe`, `ReportsService`, `AlertBannerService`.
- `OnInit`-Hook laedt Reports, wenn `darfReports()` true ist.
- Neue Signale `letzteReports`, `letzteReportsLaedt`,
  `letzteReportsFehler`.
- Template-Section `data-testid="dashboard-handlungszentrale"`
  enthaelt die beiden Cards; Report-Card nur bei `darfReports()`.

### Tests
- Erweitere `dashboard.component.spec.ts`: 5 neue Cases.
  - ADMIN laedt Reports, sieht Report-Card.
  - ASSESSOR sieht Report-Card nicht und `list` wird nicht
    aufgerufen.
  - T2-Ampel rendert CRITICAL bei visible=true.
  - T2-Ampel rendert LOW bei keinem Banner.
  - Fehler beim Report-Laden setzt `letzteReportsFehler`.

## Nicht-Umfang

- Pagination/Filter der Dashboard-Report-Liste (dort reicht die
  Top-5-Kurzliste; Details auf `/reports`).
- Persoenliche "Mein Tag"-Card mit zugewiesenen Queue-Items -
  braucht einen Backend-Endpoint.

## Abnahme

- `ng lint` / `ng build` / Karma gruen.
