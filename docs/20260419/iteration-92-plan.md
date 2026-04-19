# Iteration 92 - Plan: Globale Suche + Bell-Notifications (U-07b)

**Jira**: CVM-332

## Ziel

- `/`-Shortcut oeffnet einen globalen Such-Dialog, der sichtbare
  Menue-Eintraege filtert und bei CVE-ID-Eingabe direkt zum
  `/cves/<id>`-Deep-Link fuehrt.
- Ein Bell-Icon in der Topbar zeigt die Anzahl der T2-Eskalationen
  (aus `AlertBannerService.status().count`) und verlinkt auf
  `/alerts/history`.

## Umfang

### Neu
- `global-search.component.ts` (shared): `<cvm-dialog>` mit
  Typeahead-Input. Filter ueber `RoleMenuService.visibleEntries` +
  rekursives Flatten der Kinder, Enter aktiviert ersten Treffer.
  CVE-Pattern wird zu einem zusaetzlichen Deep-Link umgesetzt.
- `global-search.component.spec.ts`: 7 Specs (leer, Substring,
  CVE-Match, Uppercase-Normalisierung, waehle, waehleErsten, noop).

### Angepasst
- `global-shortcuts.directive.ts`: neuer `@Output() search` + `/`
  als Shortcut.
- `global-shortcuts.directive.spec.ts`: neuer Case fuer `/`.
- `global-shortcuts-overlay.component.ts`: Shortcut-Zeile fuer `/`.
- `shell.component.{ts,html}`: `searchOpen`-Signal, neuer
  Search-Button (Icon `search`) + Bell-Link mit Badge auf
  `/alerts/history`, basiert auf `alertCount`-Computed aus
  `AlertBannerService.status().count`.

## Nicht-Umfang

- Volltextsuche auf Queue-/Waiver-/Profile-Inhalten (braucht
  Backend-Endpoint).
- Bell-Popover mit letzten Alerts inline (Link auf Alert-Historie
  reicht fuer diese Iteration).

## Abnahme

- `ng lint` / `ng build` / Karma gruen.
- Backend unveraendert.
