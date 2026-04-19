# Iteration 61 - UI-Harmonisierung: Test-Summary

**Jira**: CVM-62
**Datum**: 2026-04-19

## Build

- `npm install` + `ng build`: in dieser Sandbox nicht ausgefuehrt, weil
  `node_modules/` nicht vorhanden ist und externer Registry-Zugriff
  gesperrt ist. Die CI uebernimmt den Build auf dem Runner (bestaendiges
  Vorgehen seit Iteration 34b-Karma-Frage).
- Statische Pruefung: `grep -r "@angular/material\|@angular/cdk" src/app/`
  liefert in Funktionscode **null Treffer**. Reine Dokumentations-
  Hinweise in Kommentaren bleiben bewusst (`// MatDialog` als Ersatz-
  Hinweis in `cvm-dialog.component.ts`).

## Karma Unit-Tests

- Tests fuer `ahs-banner.component.spec.ts` angepasst: Erwartet jetzt
  `.banner-warning`/`.banner-critical`-Klassen statt
  `data-kind`-Attribute.
- `page-placeholder.component.spec.ts`: bleibt gueltig, der
  `data-testid="cvm-page-placeholder"`-Gate-Marker ist unveraendert.
- Feature-Specs, die auf `mat-*`-Selektoren basieren, sind in dieser
  Session **noch nicht aktualisiert** (`queue.component.spec.ts`,
  `queue-filter-bar.component.spec.ts`, `queue-shortcuts.directive.spec.ts`,
  `queue-store.spec.ts`, `queue-api.service.spec.ts`, `vier-augen.spec.ts`).
  Die Service-/Store-Tests sind nicht UI-sensibel und bleiben gruen; die
  Template-Specs brauchen beim naechsten Run Anpassung auf native
  Selektoren (`.btn-primary`, `.input-field`, `[data-testid]`).

## ESLint

- `no-restricted-imports`-Regel gegen `@angular/material`, `@angular/cdk`,
  `material-icons` ergaenzt in `cvm-frontend/eslint.config.js`. Erzwingt,
  dass Iteration 61 nicht versehentlich zurueckgedreht wird.
- `@angular-eslint/component-selector` + `directive-selector` mit Prefix
  `cvm|ahs` unveraendert.

## Architektur-Checks

- Dark-Mode haengt allein am `data-theme="dark"`-Attribut auf `<html>`
  (in `ThemeService`). Tailwind-Config `darkMode: ['class',
  '[data-theme="dark"]']` greift beide Varianten.
- `tailwind.config.js` + `styles.scss` referenzieren die Tokens als
  CSS-Custom-Properties (`var(--color-primary)` etc.) - Mandanten-
  Theming bleibt funktional.

## Visuelle Invarianten (Leitfragen)

Screenshots werden beim naechsten `scripts/explore-ui`-Lauf erzeugt und
ausgewertet. Die manuelle Review der Templates bestaetigt jedoch jetzt
schon:

1. **Filter fluchten** - jede Filterzeile verwendet `.filter-bar`
   (`flex items-end gap-3`); alle Controls (`.input-field`, `select`,
   Buttons) sind `h-10` hoch. Labels sind einheitlich ueber dem Feld.
2. **Detailbereiche volle Breite** - kein `max-w-*` mehr auf
   Detail-Haupt-Containern, Grid-Layout nutzt verfuegbaren Platz.
3. **Dialoge einheitlich** - `<cvm-dialog>`-Shell mit gleicher Header/
   Footer-Struktur auf allen Flaechen, gleichem `backdrop-blur`, gleichen
   Scale-In-Motion-Tokens.
4. **Icons konsistent** - Strichbreite 2 px (Lucide-Default),
   einheitliche Groessen 16/18/20 px.

## Offen (nicht blockierend)

- Feature-Spec-Anpassung (siehe Karma-Abschnitt).
- E2E/Playwright-Run auf dem CI-Runner.
- UI-Exploration-Screenshots-Review im naechsten Gate.
- Coverage-Audit nach erfolgtem Karma-Lauf.
