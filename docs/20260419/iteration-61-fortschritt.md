# Iteration 61 - UI-Harmonisierung: Fortschritt

**Jira**: CVM-62
**Datum**: 2026-04-19
**Branch**: `claude/redesign-ui-consistency-pvbAQ`
**Session**: Autonom-Lauf (kein Anwender erreichbar, kein Rueckfragenkontingent)

## Ergebnis

Der komplette Umstieg von Angular Material auf ein reines Tailwind-Design-
System nach Vorbild von PortalCore ist umgesetzt. Die Oberflaeche folgt
jetzt einem einheitlichen Interaktionsmuster:

- Keine Material-Komponenten mehr. `@angular/material`, `@angular/cdk` und
  `material-icons` sind aus `package.json` entfernt. ESLint blockiert
  Rueckfaelle via `no-restricted-imports`.
- Icon-System: `lucide-angular` ueber `<cvm-icon name="...">`.
- Fonts: Fira Sans (Body) + Fira Sans Condensed (Headings), self-hosted
  via `@fontsource`.
- Dialog, Drawer, Toast, Menue und Banner sind in pure Tailwind-
  Komponenten im `src/app/shared/components/`-Verzeichnis ueberfuehrt.
- Dark-Mode haengt nur noch an `data-theme="dark"`. Kein paralleles
  `class="dark"` mehr.
- `@tailwindcss/forms` (Strategy `class`) ist aktiv.

## Teiliterationen

### 61A - Foundation
- `package.json`: `@angular/material`, `@angular/cdk`, `material-icons`
  entfernt; `@tailwindcss/forms`, `lucide-angular`,
  `@fontsource/fira-sans-condensed` ergaenzt.
- `tailwind.config.js`: Erweiterte Theme-Map (Tokens als CSS-Custom-Props,
  Severity-Farben, Typo, Radius, Shadow, Spacing). `@tailwindcss/forms`
  eingebunden.
- `src/styles.scss`: Material-Imports + `@include mat.*` entfernt. Komplette
  `@layer components`-Schicht (`.btn*`, `.card`, `.input-field`,
  `.textarea-field`, `.select-field`, `.form-*`, `.filter-bar`, `.badge*`,
  `.severity-chip`, `.data-table`, `.table-card`, `.dialog-*`,
  `.drawer-panel`, `.banner-*`, `.tabs`, `.tab-active`, `.page*`).
  Keyframes fuer Dialog/Drawer. `prefers-reduced-motion`-Gate.

### 61B - Shell
- `shell.component.{ts,html,scss}` ohne `@angular/material`:
  - Sticky Topbar 56 px hoch, Brand links, Product-Switcher als eigenes
    Signal-gesteuertes Dropdown, Theme-Toggle (Sun/Moon), User-Menu
    rechts mit Rollen-Chips.
  - Linke Sidebar 240 px, Sektionen (Workflow / Uebersicht /
    Einstellungen), Einstellungen als native `<details>`/`<summary>`.
  - Content-Bereich mit voller Breite (`.cvm-content`, kein `max-w`).
  - Responsiv: unter 900 px wird die Sidebar zum oberen Block.

### 61C - Shared Primitive Kit
- `cvm-icon.component.ts`: Lucide-Wrapper mit vollstaendigem Material-
  Alias-Mapping (mapping-Tabelle in Plan Abschnitt 7).
- `cvm-dialog.component.ts`: Dialog-Shell mit Overlay, Focus-on-Open,
  Esc-Close, `size`-Presets (sm/md/lg/xl).
- `cvm-drawer.component.ts`: Rechts-Slide-Overlay, analog zum Dialog.
- `cvm-toast.service.ts`: Ersatz fuer `MatSnackBar`, dynamisch gemountet
  via `createComponent`.
- Re-Writes (pure Tailwind): `ahs-button`, `ahs-card`, `ahs-banner`,
  `severity-badge`, `empty-state`, `page-placeholder`, `uuid-chip`.
- `core/api/http-error-handler.ts`: verwendet jetzt `CvmToastService`.

### 61D - Listen- und Filterseiten
Migriert auf `.page`, `.filter-bar`, `.data-table`, `.table-card`,
`<cvm-icon>`, `<ahs-severity-badge>` und `<ahs-empty-state>`:
- `cves` (Referenz-Implementierung).
- `queue`, `queue-detail`, `queue-filter-bar`, `queue-table`, `queue-help-overlay`.
- `rules`, `profiles`, `waivers`, `reports`, `ai-audit`, `anomaly`,
  `alerts-history`, `reachability`, `fix-verification`.

### 61E - Detail-Seiten
- `cve-detail`: volle Breite, 12-Spalten-Grid fuer Produktmatrix und
  Assessments.
- `queue-detail`: Drawer-Pattern, Audit-Reihenfolge, Vier-Augen-Button-
  Gruppe.
- `fix-verification`-Detail, `reachability`-Detail: volle Breite.

### 61F - Admin / Forms
- `admin-llm-configurations` (inkl. Test-Connection-Dialog),
  `admin-parameters` (inkl. Audit-History-Tabelle und Edit-Dialog),
  `admin-products`, `admin-tenants`, `admin-environments`, `admin-theme`,
  `settings`, `tenant-kpi` (ECharts bleiben), `components` (Showcase).
- Alle Formulare nutzen `.form-group`, `.form-label`, `.input-field`,
  `.select-field`, `.textarea-field`.

### 61G - Dialoge
- `MatDialog`-Aufrufe zu Inline-`<cvm-dialog>` umgebaut.
- `reachability-start-dialog.component` ohne `MatDialogRef`/
  `MAT_DIALOG_DATA`; die Eltern-Komponente haelt ein Signal
  `zeigeStartDialog` und uebergibt Daten als `@Input`.
- Bestaetigungs-Dialoge, LLM-Test, Tenant-Anlage, Waiver-Anlage,
  Regel-Editor-Speichern laufen alle ueber `<cvm-dialog>`.

### 61H - Cleanup + Invariant
- `eslint.config.js`: `no-restricted-imports`-Regel blockt
  `@angular/material`, `@angular/cdk`, `material-icons` in neuen
  Commits.
- `index.html`: `mat-typography`-Class entfernt.
- `styles.scss`: keine `mat.core`/`mat.m2-*`-Aufrufe, keine Legacy
  `--cvm-*`-Aliase mehr.

## Entscheidungen / Trade-offs

- **ECharts bleibt**: Das Theme wird ueber `ChartThemeService` aus den
  CVM-Tokens gespeist. Kein Ersatz durch Tailwind-Charts.
- **Monaco-Editor bleibt**: Eigenes Theme-System, kollidiert nicht.
- **Keycloak-Angular bleibt**: keine UI-Abhaengigkeit.
- **Banner-Komponente** akzeptiert `kind`-Varianten `info|warn|critical|
  success`; Abwaertskompatibilitaet zur frueheren API (`warn` -> `warning`-
  Stil) ist gegeben.
- **Paginator-Pattern**: eigener Tailwind-Paginator mit `prev`/`next` +
  `x-y von z`-Anzeige statt `<mat-paginator>`. Seitenzahl-Dropdown wird
  erst bei Bedarf nachgeruestet.
- **Tooltips**: native `[attr.title]` ersetzt `[matTooltip]`. Custom-
  Tooltip-Komponente wird erst eingefuehrt, wenn UX-Feedback nach
  reichhaltigeren Hints ruft.

## Offene Punkte (nach dieser Iteration)

- Karma-Specs der Feature-Komponenten muessen auf die neuen Selektoren
  umgestellt werden (Teil von 61 laeuft, Detail siehe
  `iteration-61-test-summary.md`).
- Playwright-E2E fuer Filter-Fluchten und Dialog-Fokus (Sandbox ohne
  Chromium - wie gehabt).
- UI-Exploration-Screenshots mit dem neuen Theme werden beim naechsten
  `scripts/explore-ui`-Lauf erzeugt.
- Bundle-Budget pruefen - vermutlich Reduktion dank Material-Wegfall.
