# Iteration 80 - Plan: Workflow-CTAs (U-01a, erste Runde)

**Jira**: CVM-320

## Ziel (aus UX-Review Block A / U-01)

Workflow ist im Menu angekuendigt, aber Uebergaenge zwischen
Stationen fehlen. Erste Runde verdrahtet drei der wichtigsten:
- Scan-Upload → Queue
- Dashboard → Workflow (Scan, Queue, Waiver)
- Queue-Leerzustand → Reports/Dashboard

Folge-Iterationen decken die restlichen Uebergaenge
(Reachability→Queue, FixVerif→Waiver, Anomalie→Finding,
Waiver→Report).

## Umfang

### Scan-Upload-Erfolg CTAs
`scan-upload.component.html`: nach `summary()` eine
Aktions-Zeile mit zwei Buttons:
- "Findings fuer diese Version in der Queue pruefen"
  → `/queue?productVersionId=<id>&environmentId=<id?>`
- "Zum Dashboard" → `/dashboard`.

### Queue liest queryParams
`queue.component.ts` liest `ActivatedRoute.queryParamMap`:
- `productVersionId` → Filter setzen.
- `environmentId` → Filter setzen.
- `status` (bei Bedarf) → Filter setzen (erlaubt alle Werte aus
  AssessmentStatus; Default bleibt wie bisher).
Filter wird am Store gesetzt, Tabelle laedt neu.

### Dashboard "Naechste Schritte"-Zeile
Vor den vier KPI-Karten: drei Handlungskarten mit
`routerLink`:
- "Neuer Scan" (→ `/scans/upload`), Icon `cloud-upload`.
- "Bewertungen pruefen" (→ `/queue`), Icon `rule`.
- "Waiver-Verwaltung" (→ `/waivers`), Icon `rule_folder`.

Each Karte hat einen Kurztext (Ein-Satz) als Hinweis. Rollen-
Filter: die Karten sind nur sichtbar, wenn der Nutzer die
noetige Rolle hat (`AuthService.hasRole`).

### Queue-Empty-State CTAs
Im `queue.component.html`: wenn `entries().length === 0` und
kein Error/Loading, statt der stummen Leerzeile eine Empty-
State-Card:
- "Keine offenen Bewertungen" mit Icon.
- Zwei Buttons: "Zu den Berichten" und "Zum Dashboard".

## TDD

- Karma: `scan-upload.component.spec.ts` (neu) fuer die CTAs in
  der Summary-Section (Button-Selektor + routerLink-Wert).
- Karma: `dashboard.component.spec.ts` erweitern um Rollen-
  spezifische Sichtbarkeit der Handlungskarten.
- Karma: `queue.component.spec.ts` erweitern um Empty-State-
  CTAs.
- Karma: `queue.component.spec.ts` neuer Case "liest
  queryParams und setzt Filter".

## Nicht-Umfang

- Backend: kein neuer `scanId`-Queryparam. Nutze
  `productVersionId`+`environmentId`, die der Scan-Summary
  bereits kennt. `scanId` kommt spaeter mit einem echten
  Listing-Endpunkt (U-08 fuer Reports spaeter analog).
- Reachability/Anomaly/Waiver/FixVerif CTAs: kommen in U-01b.
- Breadcrumbs: U-07.

## Abnahme

- `./mvnw -T 1C test` → BUILD SUCCESS (kein Backend-Touch, aber
  sicherheitshalber Run).
- `npx ng lint` / `npx ng build` / Karma alle gruen.
