# Iteration 85 - Plan: Waiver-Lifecycle im UI (U-03)

**Jira**: CVM-325

## Ziel

Backend hat seit Iteration 20 `POST /waivers/{id}/extend` und
`POST /waivers/{id}/revoke`. Im Frontend fehlen die Buttons -
Admins muessten cURL nutzen. Diese Iteration schliesst die
Luecke.

## Umfang

### WaiversService
- Neue Methoden `extend(id, validUntil, extendedBy)` und
  `revoke(id, revokedBy, reason)`.

### WaiversComponent
- Neue Aktion-Spalte in der Tabelle.
- Pro ACTIVE-Waiver: Buttons "Verlaengern" und "Widerrufen".
- Expire-Warn-Banner nutzt schon `<ahs-banner kind="warn">`
  (Bestand) - Bleibt.
- Zwei Dialoge (`<cvm-dialog>`):
  - **Verlaengern**: Datum-Input (neues `validUntil`), Begruendung
    (optional/fuer Audit). Vier-Augen: Warnung "Verlaengerer
    muss != Erteiler sein".
  - **Widerrufen**: Freitext Begruendung (Pflicht).
- Bei Erfolg: Toast + Neuladen.

### Auth / Vier-Augen
- `extendedBy` / `revokedBy` = aktueller Username.
- UI-Warnung in der Verlaengern-Dialog: wenn `waiver.grantedBy ==
  auth.username`, rote Box "Verlaengerer darf nicht Erteiler
  sein" + Button disabled.

## Tests

- `waivers.service.spec.ts` (neu) fuer `extend` und `revoke`.
- `waivers.component.spec.ts` erweitert:
  - Buttons pro Zeile (nur fuer ACTIVE).
  - Verlaengern-Dialog: disable bei Auto-Erteiler.
  - Widerrufen-Dialog: Begruendung pflicht.

## Abnahme

- `ng lint` / `ng build` / Karma gruen.
