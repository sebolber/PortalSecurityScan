# Iteration 27d - Folge-Arbeit (Anomaly + Tenant-KPI)

**Jira**: CVM-64
**Branch**: `claude/ui-theming-updates-ruCID`
**Stand**: 2026-04-18

Folge-Arbeitsblock auf derselben Branch nach 27c. Ziel: zwei
weitere Platzhalter aus 27b/27c durch echte Basis-Sichten ersetzen
(Anomalie-Board, Mandanten-KPIs) und die verbleibenden beiden
Platzhalter (Reachability, Fix-Verifikation) mit prazisierten
Einstiegs-Hinweisen versehen.

## Umgesetzt

### Anomalie-Board (ersetzt Platzhalter)

- Backend-Endpunkt `GET /api/v1/anomalies?hours=N` existiert
  bereits (Iteration 18).
- `core/anomaly/anomaly.service.ts` kapselt
  `list(hours)` und `count24h()`.
- `features/anomaly/anomaly.component.*`:
  Zeitfenster-Toggle (24 h / 3 Tage / 7 Tage), Material-Tabelle
  mit Zeitstempel, Muster, Severity-Chip (Tokens), Begruendung
  und Assessment-Referenz. Leerzustand + Fehlerbanner.

### Mandanten-KPI-Dashboard (ersetzt Platzhalter)

- Backend-Endpunkt `GET /api/v1/kpis?window=...` existiert
  bereits (Iteration 21, `KpiController`).
- `core/kpi/kpi.service.ts` mit `KpiResult`-DTO
  (`openBySeverity`, `mttrDaysBySeverity`, `slaBySeverity`,
  `automationRate`, `burnDown`).
- `features/tenant-kpi/tenant-kpi.component.*`:
  - Hero-Kachel Automatisierungsquote (Prozent).
  - Severity-Tabelle (Offen, MTTR Tage, SLA-Quote) mit Chip-
    Palette aus Tokens.
  - Burn-Down-Linienchart (ECharts) gegen `ChartThemeService`,
    damit Branding-Wechsel auch hier automatisch reinschlaegt.
  - Zeitfenster-Toggle 30d/90d/180d.

### Reachability + Fix-Verifikation: Placeholder prazisiert

Beide Bereiche sind per-Finding-/per-Mitigation-Sichten; die
heutigen Backends erwarten einen Finding- bzw. Mitigation-
Kontext (`POST /api/v1/findings/{id}/reachability`,
`GET /api/v1/mitigations/{id}/verification`). Die Placeholder-
Texte erklaeren den Einstieg aus der Assessment-Queue und
verweisen auf eine Folge-Iteration 27e fuer die
Uebersichtsseite.

## Coverage-Matrix

- **ANGEBUNDEN**: 18 Bereiche (2 mehr als in 27c).
- **PLATZHALTER**: nur noch 2 (Reachability, Fix-Verifikation).
- Keine `NAV_OHNE_INHALT`-Zeilen.

## Build / Verifikation

- `./mvnw -pl cvm-architecture-tests -am test` -> BUILD SUCCESS
  (Backend unveraendert: 319 Tests gruen).
- `npx ng lint cvm-frontend` -> All files pass linting.
- `npx ng build cvm-frontend` -> Application bundle generation
  complete. Bundle-Budget-Warnung unveraendert.

## Offene Punkte (27e)

- Reachability-Uebersichtsseite (neuer Backend-Read-Endpunkt
  `GET /api/v1/reachability?since=...` noetig).
- Fix-Verifikation-Uebersicht (aggregierender Read-Endpunkt
  ueber offene Mitigations).
- Stylelint-Guard im CI scharfschalten (nach kompletter
  Token-Migration von Reports/Rules/Profile/Settings/AI-Audit).
- `FullNavigationWalkThroughTest` + axe-core in Playwright.
