# Iteration 52 - Bundle-Budget-Reduktion

## Ziel

Initial-Bundle unter 2 MB halten. Dazu:
- ECharts nur in Dashboard und Tenant-KPI laden.
- LoginCallback-Komponente lazy machen.
- `angular.json` Budget-Grenzen zurueck auf Produktionswerte
  (`maximumError: 2mb`, `maximumWarning: 1.5mb`).

## Vorgehen

1. Neuer Helper `shared/charts/echarts-providers.ts` mit
   `echartsRouteProviders()`, der `echarts/core` +
   Bar/Line/Pie-Charts + Grid/Legend/Tooltip-Components konfiguriert
   und `provideEchartsCore` liefert.
2. `app.config.ts` entfernt `provideEchartsCore` + die direkten
   `echarts/...`-Imports komplett.
3. `DashboardComponent` und `TenantKpiComponent` binden den
   Provider ueber `providers: [echartsRouteProviders()]` im
   `@Component`-Metadata ein. Dadurch landet echarts nur im
   jeweiligen Feature-Chunk.
4. `app.routes.ts`: `login-callback` wird `loadComponent`-geladen.
5. `angular.json`: Budget auf 1.5mb (Warning) / 2mb (Error).

## Ergebnis

- Initial-Bundle: 2.13 MB &rarr; 1.10 MB
  (Reduktion um rund 1 MB dank lazy ECharts).
- Kein Budget-Warning mehr (Warning-Schwelle 1.5 MB).

## Jira

`CVM-102` - Bundle-Budget-Reduktion.
