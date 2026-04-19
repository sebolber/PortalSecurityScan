# Iteration 92 - Fortschritt

**Jira**: CVM-332 (U-07b Globale Suche + Bell)

## Umgesetzt

- `GlobalSearchComponent`: neuer Such-Dialog. Filter laeuft ueber
  die Menue-Eintraege, die der aktuelle User sehen darf; bei
  CVE-Pattern kommt ein Deep-Link als erster Treffer. Enter
  oeffnet den ersten Treffer, Klick auf eine Zeile ebenfalls.
- `GlobalShortcutsDirective`: neuer `search`-Output + `/`-Handler.
- `GlobalShortcutsOverlayComponent`: neue Zeile "`/` - Globale
  Suche".
- `ShellComponent`: zwei neue Topbar-Buttons fuer eingeloggte
  User:
  - Suche-Icon (Shortcut `/`) oeffnet den Dialog.
  - Bell-Icon routet nach `/alerts/history`; ein rotes Badge zeigt
    den aktuellen T2-Count, wenn der `AlertBannerService.status()`
    Werte liefert.
- `alertCount = computed(...)` im Shell kapselt den Badge-Wert.

## Nicht umgesetzt

- Backend-Volltextsuche: die Such-Hits kommen ausschliesslich aus
  dem statischen Menue-Katalog. Queue-Eintraege, Waiver-Texte und
  Profil-Namen sind bewusst nicht gemappt - dafuer braucht es
  einen eigenen Endpoint.
- Bell-Popover mit letzten Alerts: aktuell fuehrt der Klick direkt
  auf `/alerts/history`. Inline-Popover bleibt Folge-Iteration.

## Technische Hinweise

- Die Shell haengt den neuen Search- und Bell-Button hinter einer
  `@if (loggedIn())`-Guard, damit Anonyme Nutzer keine Such-Button
  sehen (sonst koennte der Dialog leer oeffnen).
- Das Badge nutzt absolute Positionierung `-top-1 -right-1` und
  cappt die Zahl auf `99+`.
