# Iteration 52 - Fortschritt

**Thema**: Bundle-Budget-Reduktion (CVM-102).

## Ergebnis

- Initial-Bundle vor der Iteration: 2.13 MB.
- Initial-Bundle nach der Iteration: **1.10 MB**.
- Budget in `angular.json` zurueckgesetzt auf 1.5mb (Warning) /
  2mb (Error). Der Build laeuft jetzt ohne Budget-Warnungen.

## Was gebaut wurde

- Neuer Helper `shared/charts/echarts-providers.ts` kapselt
  echarts-Core-Konfiguration (Bar/Line/Pie +
  Grid/Legend/Tooltip) und liefert `provideEchartsCore`.
- `app.config.ts` ohne ECharts-Imports; ECharts lebt nur noch in
  chart-tragenden Komponenten.
- `DashboardComponent` und `TenantKpiComponent` beanspruchen den
  Provider via `providers: [echartsRouteProviders()]` - dadurch
  lazy in den jeweiligen Feature-Chunk.
- `app.routes.ts`: `login-callback` ist ebenfalls lazy
  (`loadComponent`).
- `angular.json` Budget zurueck auf 1.5mb/2mb.

## Build

- `npx ng build` &rarr; ok, keine Budget-Warnung.
- `npx ng lint` &rarr; All files pass linting.

## Hinweise

- Die Reduktion beruht fast vollstaendig auf dem Lazy-Loading von
  ECharts. Ein weiterer Hebel waere der Keycloak-Adapter; das ist
  aber kritischer und wurde in dieser Iteration bewusst nicht
  angefasst.
