# Iteration 95 - Plan: Secret-Rotations-Dialog (U-09)

**Jira**: CVM-335

## Ziel

Sensible System-Parameter (API-Keys, Tokens) sollen im UI einen
dedizierten Rotations-Flow bekommen statt des generischen
`window.prompt`-Dialogs. Der Dialog bietet ein Passwort-Feld mit
Show/Hide-Toggle, Pflicht-Begruendung und strukturiertes
Error-Handling. In der Parameter-Tabelle werden sensible Werte
einheitlich maskiert dargestellt.

## Umfang

### Admin-Parameters-Component
- Neue Signale `rotationOffen`, `rotationEintrag`, `rotationWert`,
  `rotationGrund`, `rotationSichtbar`, `rotationPending`,
  `rotationFehler`.
- `wertAendern(entry)` ruft bei `sensitive=true` den neuen
  `oeffneRotation`-Flow statt `window.prompt`-Kette.
- Neue Methoden `oeffneRotation`, `brecheRotationAb`,
  `rotationUmschalten`, `speichereRotation`.
- `wertZuruecksetzen` und `loesche` wechseln auf `CvmConfirmService`
  (window.confirm weg).

### Template
- Parameter-Tabelle: bei `p.sensitive` einheitliche `••••••••`-
  Maske mit `data-testid="param-sensitive-mask-<key>"`.
- Neuer `<cvm-dialog>` "Secret rotieren" am Ende des Templates
  mit Passwort-Input, Toggle-Button, Pflicht-Begruendung,
  Fehleranzeige und Save/Cancel.

### Tests (`admin-parameters.component.spec.ts` - neu)
- 7 Specs: Oeffnen bei sensiblem Eintrag, Validierung
  (Wert/Grund), Happy-Path, Toggle, Abbruch, `loesche` nutzt
  CvmConfirmService.

## Nicht-Umfang

- Backend-seitige Secret-Rotations-History (bleibt im Audit-Log).
- Automatische Rotations-Reminder / Ablauf-Warnungen.
- Weiterhin `window.confirm` in
  `admin-llm-configurations`/`admin-theme`/`profiles` - diese
  Flows bleiben fuer einen Folge-Cleanup offen.

## Abnahme

- `ng lint` / `ng build` / Karma gruen.
