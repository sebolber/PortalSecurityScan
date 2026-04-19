# Iteration 91 - Plan: Breadcrumbs + globale Shortcuts (U-07a)

**Jira**: CVM-331

## Ziel

Der Anwender soll auf jeder Seite sehen, wo er steht (Breadcrumb)
und mit einer Tastenkombination zwischen den Hauptseiten wechseln
koennen. `?` oeffnet eine Uebersicht aller Shortcuts, `g d`, `g q`,
`g s`, `g w`, `g r` navigieren zu Dashboard/Queue/Scan/Waiver/
Reports.

## Umfang

### Neue Komponenten und Services
- `cvm-breadcrumbs.component.ts` (shared): Nimmt die aktuelle URL
  aus dem `Router`, fragt `RoleMenuService.breadcrumbFor(url)` und
  rendert `Start > [Parent >] <aktuelle Seite>`. Letzter Eintrag
  ist Text (kein Link).
- `global-shortcuts.directive.ts`: `document:keydown`-Listener,
  ignoriert `INPUT/TEXTAREA/SELECT/contenteditable`. Behandelt `?`
  (oeffnet Overlay) und 2-Tasten-Prefix `g` + `d/q/s/w/r` mit
  1.5s-Timeout.
- `global-shortcuts-overlay.component.ts`: `<cvm-dialog>` mit
  Navigations-/Hilfe-Sektion.

### RoleMenuService
- Neue Methode `breadcrumbFor(path)`. Sucht longest-prefix-match
  ueber `allEntries()`; wenn der Treffer ein Kind (Einstellungen-
  Unterpunkt) ist, wird der Parent zwischen Start und Treffer
  eingefuegt. Query-/Fragment-Anteile werden abgeschnitten.

### Shell
- Haengt die neue Directive an einen Wrapper ueber dem gesamten
  Shell-Layout.
- Rendert `<cvm-breadcrumbs>` direkt ueber dem `<router-outlet>`.
- Rendert `<cvm-global-shortcuts-overlay [visible]...>` am Ende
  des Templates; `shortcutsOpen`-Signal gesteuert via
  `oeffneShortcutSheet()` / `schliesseShortcutSheet()`.

### Tests
- `global-shortcuts.directive.spec.ts`: 7 Specs (?, g d/q/s/w,
  INPUT-Fokus, unbekannter Suffix-Key).
- `cvm-breadcrumbs.component.spec.ts`: 7 Specs (Dashboard rendert
  nichts, /queue, /admin/products mit Parent, /cves/:id per
  Prefix-Match; plus 3 direkte Service-Specs fuer
  `breadcrumbFor`).

## Nicht-Umfang

- Breadcrumbs fuer Detail-Routen (z.B. CVE-Titel statt CVE-ID)
  bleiben Folge-Iteration - dafuer braucht es pro Feature einen
  Resolver.
- Suche/Spotlight/Notifications bleiben in Iteration 92 (U-07b).

## Abnahme

- `ng lint` / `ng build` / Karma gruen.
