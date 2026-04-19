# Iteration 93 - Plan: Report-Historie im UI (U-08 Teil 1)

**Jira**: CVM-333

## Ziel

Die `/reports`-Seite zeigt bisher nur die Reports, die in der
aktuellen Session erzeugt wurden. Der Backend-Endpoint
`GET /api/v1/reports` existiert bereits (paginiert,
neueste zuerst) und wird in dieser Iteration im UI angebunden.

## Umfang

### Service (`reports.service.ts`)
- Neue Types `ReportListResponse`, `ReportListQuery`.
- Neue Methode `list(query)` - mappt `productVersionId`,
  `environmentId`, `page`, `size` auf `?key=value`-Pairs.

### Component (`reports.component.{ts,html}`)
- Neue Signale `historie`, `historieLaedt`, `historieFehler`,
  `historieGesamt`.
- `ladeHistorie()` laedt die 20 neuesten Reports und setzt die
  Signale.
- `ngOnInit` ruft erst den Katalog, dann `ladeHistorie()`.
- `erzeuge()` triggert nach Erfolg einen Historie-Reload.
- Template: neue Card "Report-Historie" mit Refresh-Button,
  Leer-/Fehlerzustand, Tabelle mit Titel/Gesamt/Erzeugt-Daten/
  SHA-256-Prefix und Download-Button.
- Card-Untertitel der Session-Liste angepasst.

### Tests (`reports.service.spec.ts`)
- `list()` ohne Filter ruft `/api/v1/reports`.
- `list()` mit `productVersionId`+`size` baut die richtigen
  queryParams.

## Nicht-Umfang

- Pagination-Controls im UI (aktuell immer Seite 0, size 20).
  Folgt in Iteration 94 oder spaeter.
- Filter-Formular fuer Produkt-Version/Umgebung in der Historie.

## Abnahme

- `ng lint` / `ng build` / Karma gruen.
