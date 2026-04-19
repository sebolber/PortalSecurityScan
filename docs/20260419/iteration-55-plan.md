# Iteration 55 - KPI-UI (Severity-Saeulen + SLA-Ampel)

## Ziel

Die bestehende Tenant-KPI-Seite bietet bereits Automatisierungsquote,
Tabelle und Burn-Down. Iteration 55 ergaenzt:
- **Severity-Saeulen**: Balken-Diagramm mit offenen Findings pro
  Severity, Farben aus dem Theme.
- **SLA-Ampel**: kleiner farbiger Kreis pro Severity-Zeile
  (gruen &ge;95 %, gelb 80..95 %, rot <80 %).

## Vorgehen

1. `tenant-kpi.component.ts`:
   - `severityBarOption` (computed) mit ECharts-Bar-Option.
   - Helper `slaAmpel(severity)` liefert 'green'|'yellow'|'red'.
2. `tenant-kpi.component.html`:
   - Neue `mat-card` "Offene Findings je Severity" mit dem
     Bar-Chart.
   - Ampel-Chip in der SLA-Spalte.
3. `tenant-kpi.component.scss`:
   - `.cvm-tenant-kpi__ampel` Klasse mit data-ampel-Selektor fuer
     die drei Farben.

## Scope

- **Nicht** in dieser Iteration: Tenant-Verwaltungs-UI. Laut Vorgabe
  wird das als eigene Iteration ausgeplant (Backend `Tenant` bleibt
  eine eigene Entity).

## Jira

`CVM-105` - KPI-UI Severity-Saeulen + SLA-Ampel.
