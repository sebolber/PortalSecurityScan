# Iteration 57 - Fortschritt

**Thema**: Monaco Side-by-Side Diff-Editor fuer Profiles (CVM-107).

## Was gebaut wurde

- `profiles.component.ts`:
  - `ProfileRow.diffOffen` und `diffUmschalten(row)`.
  - `diffOriginal(row)` liefert den aktiven YAML-Text.
  - `diffModified(row)` bevorzugt den Draft, fallback auf den
    Editor-Buffer.
- `profiles.component.html`:
  - Button "Diff aktiv vs. Draft" pro Zeile mit aktivem Profil.
  - `<ngx-monaco-diff-editor>` mit `renderSideBySide=true` und
    `readOnly=true`.
- `profiles.component.scss`: Diff-Actions-Zeile + Size-Styles.

## Build

- `npx ng build` &rarr; ok.
- `npx ng lint` &rarr; All files pass linting.

## Hinweise

- Der strukturierte Feld-Diff (`ProfileDiffEntry`) bleibt erhalten;
  der neue Monaco-Diff ist ein zusaetzlicher visueller Vergleich, den
  Approver beim Sign-Off nutzen koennen.
- Kein separater Provider-Aufwand: Monaco ist bereits ueber
  `monacoRouteProviders()` (Iteration 54) eingehaengt.

## Vier Leitfragen (Oberflaeche)

1. *Weiss ein Admin, was zu tun ist?* Button ist beschriftet und
   erscheint nur, wenn eine aktive Version existiert.
2. *Ist erkennbar, ob eine Aktion erfolgreich war?* Der Editor
   highlight Unterschiede.
3. *Sind Daten sichtbar, die im Backend existieren?* Ja, aktive
   YAML-Quelle links.
4. *Gibt es einen Weg zurueck/weiter?* "Diff ausblenden" schliesst
   den Viewer.
