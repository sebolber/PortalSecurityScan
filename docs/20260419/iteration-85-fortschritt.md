# Iteration 85 - Fortschritt: Waiver-Lifecycle im UI (U-03)

**Jira**: CVM-325

## Was wurde gebaut

### WaiversService
- Neue Methoden `extend(id, validUntil, extendedBy)` und
  `revoke(id, revokedBy, reason)`.

### WaiversComponent
- Neue Aktion-Spalte in der Waiver-Tabelle. Fuer ACTIVE-
  Waiver: Buttons "Verlaengern" und "Widerrufen".
- Zwei `<cvm-dialog>`-Instanzen:
  - **Verlaengern**: Date-Picker, Default = heute + 90 Tage;
    Vier-Augen-Warnung im Banner, wenn aktueller User der
    Erteiler ist (Button disabled).
  - **Widerrufen**: Freitext-Begruendung (Pflicht; Button
    disabled ohne Inhalt).
- Erfolg: Toast + Liste neu laden. Fehler: Error-Toast.

### Tests
- `waivers.service.spec.ts` (neu): 2 Cases fuer extend +
  revoke HTTP-Pfad.
- `waivers.component.spec.ts` erweitert:
  - Active-Waiver zeigt beide Buttons.
  - Vier-Augen-Konflikt disabled den Confirm-Button im
    Verlaengern-Dialog.
  - Revoke ohne Begruendung -> Warning-Toast + kein API-Call.

## Ergebnisse

- `ng lint` / `ng build` OK.
- Karma 123 Tests SUCCESS (118 + 5 neu).
- `./mvnw -T 1C test` BUILD SUCCESS in 01:31 min.

## Migrations / Deployment

- Kein neues Backend, keine Flyway-Migration.
