# Iteration 80 - Fortschritt: Workflow-CTAs (U-01a)

**Jira**: CVM-320
**Datum**: 2026-04-19

## Was wurde gebaut

### Scan-Upload -> Queue
- `scan-upload.component.ts`: neues `queueLinkParams`-Computed
  leitet `productVersionId`/`environmentId` aus der Auswahl ab.
- `scan-upload.component.html`: nach der Summary eine Button-
  Leiste mit "Findings fuer diese Version pruefen" (Deep-Link in
  die Queue) und "Dashboard". RouterLink nutzt `queryParams`, so
  dass die Queue die Filter sofort anwendet.

### Queue liest queryParams + Empty-State
- `queue.component.ts`: liest `ActivatedRoute.queryParamMap`
  und uebersetzt `productVersionId`, `environmentId` und
  `status` in den `QueueStore`-Filter. Nur wenn ein Wert sich
  aendert, wird `setFilter` gerufen, damit der bestehende
  Filter-Effect nicht unnoetig triggert.
- `queue.component.html`: wenn kein Eintrag, kein Fehler, kein
  Laden -> eine Empty-State-Card mit drei CTAs (Neuer Scan /
  Zum Bericht / Dashboard).

### Dashboard Handlungskarten
- `dashboard.component.ts`: drei neue `computed`-Rollen-
  Wachter (`darfScan`, `darfQueue`, `darfWaiver`).
- `dashboard.component.html`: oberhalb der KPI-Zeile eine
  weitere Grid-Zeile mit drei Aktionskarten (Scan / Queue /
  Waiver), jede mit Icon, Kurztext und Deep-Link. Role-Guard
  blendet Karten aus, die der Nutzer nicht betreten darf.

### Tests (TDD)
- `queue.component.spec.ts` erweitert:
  - "Empty-State zeigt die drei Workflow-CTAs"
  - "liest queryParams und setzt den Store-Filter"
- `dashboard.component.spec.ts` (neu):
  - ADMIN sieht alle drei Karten.
  - VIEWER sieht keinen Scan-/Queue-Link, aber Waiver.
- `scan-upload.component.spec.ts` (neu):
  - Summary mit Version -> Queue-CTA haelt queryParams im href.
  - Ohne Version -> kein Queue-CTA, Dashboard-CTA vorhanden.

## Ergebnisse

- `npx ng lint` -> "All files pass linting."
- `npx ng build` -> Bundle erfolgreich (14.7 s).
- Karma: 101 Tests SUCCESS (94 alt + 7 neu; Spec fuer
  `queue`-Component um 2 erweitert, 3 neue dashboard-, 2 neue
  scan-upload-Cases).
- `./mvnw -T 1C test` -> BUILD SUCCESS in 02:00 min.

## Migrations / Deployment

- Keine Flyway-Migration, keine Dependency-Aenderung.
- Keine Backend-Aenderung.
