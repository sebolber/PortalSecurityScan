# Iteration 81 - Plan: Workflow-CTAs Runde 2 (U-01b)

**Jira**: CVM-321

## Ziel

Vierte bis siebte Workflow-Kante verbinden (Reachability ->
Fix-Verifikation -> Waiver -> Bericht; Anomalie -> Waiver).

## Umfang

- **Reachability** (`reachability.component.html`): zusaetzlicher
  Header-Button rechts "Zur Fix-Verifikation" (RouterLink
  `/fix-verification`).
- **Fix-Verifikation** (`fix-verification.component.html`):
  Header-Button "Zur Waiver-Verwaltung" (RouterLink `/waivers`).
- **Anomalie** (`anomaly.component.html`): Header-Button "Zur
  Waiver-Verwaltung" (RouterLink `/waivers`).
- **Waiver** (`waivers.component.html`): Header-Button "Zum
  Hardening-Bericht" (RouterLink `/reports`).
- **Queue-Detail**: nach erfolgreichem Approve eine kleine
  "Zur Reachability"-Zeile (bleibt stumm, wenn unpassend). Hier
  reicht aber bereits das bestehende Reachability-Button im
  Detail-Panel; wir fuegen stattdessen einen **Queue-Header-
  Button** "Zur Reachability" hinzu (Deep-Link `/reachability`)
  als schneller Wechsel.

## TDD

Karma-Tests fuer jede Seite:
- Button existiert.
- `routerLink`-Attribut zeigt auf die richtige Route.

## Nicht-Umfang

- Pro-Zeile "Zum Finding"-Links: erfordern findingId-Filter im
  Queue-Backend, kommt mit U-02a.

## Abnahme

- `npx ng lint` / `ng build` / Karma gruen.
- `./mvnw -T 1C test` nicht beruehrt (Run trotzdem).
