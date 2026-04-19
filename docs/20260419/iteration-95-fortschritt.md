# Iteration 95 - Fortschritt

**Jira**: CVM-335 (U-09 Secret-Rotations-Dialog)

## Umgesetzt

- `AdminParametersComponent` bekommt einen Rotations-Flow:
  sieben neue Signale, vier neue Methoden
  (`oeffneRotation`, `brecheRotationAb`, `rotationUmschalten`,
  `speichereRotation`) und ein integrierter `<cvm-dialog>`.
- `wertAendern` erkennt `sensitive=true` und leitet in den neuen
  Dialog um; nicht-sensible Parameter bleiben beim alten
  `window.prompt`-Flow.
- `wertZuruecksetzen` und `loesche` nutzen jetzt den
  `CvmConfirmService` (Iteration 90); Delete-Variante setzt
  `variant: 'danger'`.
- Parameter-Tabelle rendert bei sensiblen Eintraegen eine
  einheitliche `••••••••`-Maske statt des Backend-Werts
  (der ohnehin null ist).
- Spec-File neu: 7 Cases decken Oeffnen, Validierung,
  Happy-Path, Toggle, Abbruch und loesche ab.

## Nicht umgesetzt

- Automatische Rotations-Reminder / Ablaufwarnungen (braucht
  Backend-Mechanismus).
- `admin-llm-configurations`, `admin-theme`, `profiles` nutzen
  weiterhin `window.confirm`/`window.prompt`; Cleanup bleibt
  Folge-Iteration.

## Technische Hinweise

- Der Show/Hide-Toggle nutzt einfach `type="password"` bzw.
  `type="text"`; der Wert bleibt nur im Component-Speicher. Das
  `visibility`-Icon wird fuer beide Zustaende verwendet
  (Icon-Registry hat kein dediziertes `visibility-off`; die
  Semantik kommt aus dem `aria-label`).
- Der Pflicht-Aenderungsgrund landet im bestehenden Audit-Log
  des Parameter-Stores ueber `changeValue(..., reason)`.
