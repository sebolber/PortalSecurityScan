# Iteration 27c - Folge-Arbeit (Chart-Theme + 2 Placeholder ersetzt)

**Jira**: CVM-63
**Branch**: `claude/ui-theming-updates-ruCID`
**Stand**: 2026-04-18

Folge-Arbeitsblock auf derselben Branch nach 27b. Ziel:
ECharts-Theme aus Tokens generieren (damit der Dashboard-Donut
auf Branding-Wechsel reagiert) und zwei der sechs Platzhalter
aus 27b durch echte Basis-Listen ersetzen.

## Umgesetzt

### Frontend: ChartThemeService

- `core/theme/chart-theme.service.ts`: liest die aktuellen
  Severity-, Surface- und Text-Muted-Tokens aus dem
  `documentElement` (per `getComputedStyle`) und liefert sie
  als Angular-Signals.
- Revalidiert sich, sobald das Branding-Signal im
  `ThemeService` kippt - Dashboard-Charts folgen damit einem
  Theme-Wechsel ohne Reload.
- Fallbacks sind definiert, falls die CSS-Variablen (noch)
  nicht gesetzt sind; es gibt keine hardkodierten Farben
  mehr im Dashboard-Code.
- Unit-Tests (3): Fallback-Verhalten, CSS-Wert aus dem
  `documentElement`, Re-Auswertung nach Branding-Wechsel.

### Dashboard-Migration

- `features/dashboard/dashboard.component.ts` konsumiert den
  Service: Severity-Slice-Farben und Pie-Border-Farbe
  kommen jetzt aus Tokens, nicht aus hardkodiertem `#fff`.
- `chartOption` ist ein `computed` statt einer statischen
  Property - ECharts rendert bei Branding-Wechsel automatisch
  neu.

### Waiver-Liste (ersetzt Platzhalter)

- Backend-Endpunkt existiert bereits
  (`GET /api/v1/waivers?status=...`, Iteration 20).
- `core/waivers/waivers.service.ts` + `WaiverView`-DTO.
- `features/waivers/waivers.component.*` mit Material-Tabelle,
  Status-Filter (ACTIVE/EXPIRED/REVOKED) und `ahs-banner`-
  Warner fuer Waiver, die innerhalb der naechsten 14 Tage
  ablaufen.
- Stylesheet vollstaendig auf Tokens: Severity-Chip-Paletten,
  `--radius-pill`, `--space-*`.

### Alert-Historie (ersetzt Platzhalter)

- Neu: `AlertHistoryView`-DTO,
  `AlertHistoryService.recent(limit)` (Default 50, Max 500),
  `AlertDispatchRepository.findAllByOrderByDispatchedAtDesc(...)`.
- `GET /api/v1/alerts/history?limit=N` am bestehenden
  `AlertsController` ergaenzt; Test dazu
  (`AlertsControllerWebTest#history`).
- `core/alerts/alerts-history.service.ts` +
  `features/alerts-history/alerts-history.component.*`
  zeigen Zeit, Betreff, Empfaenger, Trigger-Key und
  Status (GESENDET / DRY-RUN / FEHLER).

## Coverage-Matrix

`frontend-backend-coverage.md` aktualisiert:

- Alert-Historie: `PLATZHALTER` -> `ANGEBUNDEN`.
- Waiver-Liste: `PLATZHALTER` -> `ANGEBUNDEN`.
- Reachability, Fix-Verifikation, Anomalie, Cross-Tenant-KPI
  bleiben als Platzhalter (27d).

## Build / Verifikation

- `./mvnw -T 1C test` -> BUILD SUCCESS. 319 Tests gesamt
  (+1 neuer `history`-Test im AlertsControllerWebTest).
- `npx ng lint cvm-frontend` -> All files pass linting.
- `npx ng build cvm-frontend` -> Application bundle
  generation complete.

## Offene Punkte (27d)

- Basis-Listen fuer Reachability-Board, Fix-Verifikation,
  Anomalie-Board und Cross-Tenant-Dashboard.
- Detail-Seite fuer einzelne Waiver (grant/extend/revoke
  aus der Queue heraus ist bereits moeglich).
- Stylelint-Guard im CI scharfschalten (noch offen, weil
  Reports/Rules/Profile/Settings/AI-Audit noch nicht
  vollstaendig auf Tokens migriert sind).
- `FullNavigationWalkThroughTest` + axe-core in Playwright.
