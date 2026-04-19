# Iteration 91 - Fortschritt

**Jira**: CVM-331 (U-07a Breadcrumbs + globale Shortcuts)

## Umgesetzt

- `CvmBreadcrumbsComponent` (shared): liest aktuelle URL via
  `Router`, aktualisiert sich auf `NavigationEnd` und bezieht die
  Crumbs aus `RoleMenuService.breadcrumbFor(...)`.
- `GlobalShortcutsDirective`: globaler Tastatur-Handler am
  Shell-Root; `?` oeffnet das Overlay, `g` startet eine
  2-Tasten-Sequenz (d/q/s/w/r) mit 1.5s-Timeout, ignoriert
  Eingabefelder und Modifier.
- `GlobalShortcutsOverlayComponent`: kleines `<cvm-dialog>`
  mit Navigations- und Hilfe-Sektion.
- `RoleMenuService.breadcrumbFor(path)`: liefert
  `[Start, Parent?, Aktuelle Seite]` mit Longest-Prefix-Match
  ueber `allEntries()` und Query-/Fragment-Schnitt.
- Shell-Template: Directive umschliesst das komplette Layout,
  Breadcrumbs oberhalb des Router-Outlets, Overlay am Ende der
  Shell.

## Nicht umgesetzt

- Dynamische Breadcrumb-Titel fuer Detail-Routen (z.B. CVE-ID
  statt "CVEs") brauchen pro Feature einen Resolver - offene
  Folge-Iteration.
- Suche / Spotlight / Notifications (U-07b) folgen in Iteration
  92.

## Technische Hinweise

- `g`-Prefix-Status lebt im Directive-Instance; der Timer verfaellt
  nach 1.5s, damit einzelne `g`-Tasten keine Navigation ausloesen.
- Die Directive hing urspruenglich am `<header>` - beim Umbau habe
  ich sie auf einen ganzen Wrapper (`<div cvmGlobalShortcuts>`)
  verschoben, damit `document:keydown` auch bei Events ausserhalb
  des Headers angezogen wird. (Technisch gleichwertig, weil
  HostListener am `document` haengt.)
- Test-Pattern: `spyOnProperty(router, 'url', 'get')` statt
  ActivatedRoute-Mocking, weil der Komponenten-Constructor bereits
  die initial URL braucht.
