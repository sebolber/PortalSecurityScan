# Iteration 55 - Fortschritt

**Thema**: KPI-UI Severity-Saeulen + SLA-Ampel (CVM-105).

## Was gebaut wurde

- `tenant-kpi.component.ts`:
  - `severityBarOption` &ndash; ECharts-Bar-Option fuer die offenen
    Findings pro Severity, Farben aus dem Theme.
  - `slaAmpel(severity)` &ndash; liefert 'green'/'yellow'/'red'
    basierend auf der SLA-Quote (&ge;95 % / 80-95 % / &lt;80 %).
- `tenant-kpi.component.html`:
  - Neue `mat-card` "Offene Findings je Severity" mit dem
    Bar-Chart.
  - Ampel-Indikator in der SLA-Spalte der Tabelle.
- `tenant-kpi.component.scss`: Ampel-Styles fuer die drei Farben.

## Build

- `npx ng build` &rarr; ok.
- `npx ng lint` &rarr; All files pass linting.

## Nicht-Ziele

- Tenant-Verwaltungs-UI bleibt bewusst eine eigene, groessere
  Iteration (eigenes Backend-Entity, Multi-Tenant-Flows).

## Vier Leitfragen (Oberflaeche)

1. *Weiss ein Admin, was zu tun ist?* Die Seite zeigt drei Karten
   (Automatisierungsquote, Tabelle mit Ampel, Burn-Down) plus die
   neue Severity-Saeulen-Karte - die Bedeutung erklaeren die
   Titelzeilen.
2. *Ist erkennbar, ob eine Aktion erfolgreich war?* Der Fenster-
   Toggle (30/90/180 Tage) laedt neu und zeigt die aktualisierten
   Werte.
3. *Sind Daten sichtbar, die im Backend existieren?* Ja, Daten
   kommen aus `/api/v1/kpis`.
4. *Gibt es einen Weg zurueck/weiter?* "Alle 3 Fenster" sind per
   Toggle erreichbar; der Burn-Down-Chart zeigt den Verlauf
   automatisch.
