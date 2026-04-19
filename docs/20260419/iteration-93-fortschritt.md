# Iteration 93 - Fortschritt

**Jira**: CVM-333 (U-08 Teil 1 Report-Historie)

## Umgesetzt

- `ReportsService.list(query)` mit `ReportListResponse` und
  `ReportListQuery`-Types; baut queryParams konditional.
- `ReportsComponent` laedt beim Init und nach jedem erfolgreichen
  `erzeuge()` die 20 neuesten Reports per `list({ size: 20 })`.
- Neue Card "Report-Historie" rendert Tabelle oder Leer-/
  Fehlerzustand. Refresh-Button `data-testid="reports-historie-
  refresh"` feuert `ladeHistorie()`.
- Untertitel der Session-Karte aktualisiert, damit klar ist, dass
  die Session-Liste keine Historie ersetzt.

## Nicht umgesetzt

- Pagination (zurueck/vor) und Filter auf der Historie - die
  aktuelle Card zeigt immer die 20 neuesten ueber alle Produkt-
  Versionen. Folgt in Iteration 94, wenn das Dashboard als
  Handlungszentrale konsolidiert wird.

## Technische Hinweise

- Angular 17 Control-Flow unterstuetzt keine `as`-Aliasse auf
  Folge-`@else if`-Zweigen; `historieFehler()` wird deswegen
  zweimal aufgerufen.
