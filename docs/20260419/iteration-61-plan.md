# Iteration 61 - UI-Harmonisierung: Tailwind-Redesign (Interaktionsplan)

**Jira**: CVM-62 (Fortsetzung von 27/28)
**Session**: 2026-04-19 Autonom-Lauf
**Ziel**: Vollstaendige Aufloesung der Material/Tailwind-Mischung. Komplettumbau
der Angular-Oberflaeche auf ein **reines Tailwind-Design-System** nach Vorbild
des PortalCore-Frontends. Dialoge werden ueberarbeitet, Detail-Bereiche nutzen
die volle Breite, Filter- und Eingabezeilen fluchten.

---

## 1 Ausgangslage (Kurzdiagnose)

| Aspekt | PortalCore (Referenz) | CVM-Frontend (Ist) | Zielzustand |
|---|---|---|---|
| Component-Library | keine - reines Tailwind | Angular Material 18 + Tailwind | reines Tailwind |
| Icons | `lucide-angular` | `material-icons` (Ligatur-Font) | `lucide-angular` |
| Fonts | Fira Sans + Fira Sans Condensed | nur Fira Sans | beide Fira-Schnitte |
| Komponenten-Utilities | `.btn-*`, `.card`, `.input-field`, `.badge-*` | nur Typo-Utilities | vollstaendige `@layer components` |
| Dark-Mode | `class="dark"` | `data-theme="dark"` + `class="dark"` | nur `data-theme`, Tailwind variant mapped |
| Forms-Plugin | `@tailwindcss/forms` | nicht aktiv | aktiv, strategy `class` |
| Detail-Bereiche | volle Breite | feste `max-w`-Werte, zentriert | `w-full`, 12-Spalten-Grid |
| Filter-Zeilen | fluchten, `gap-3`, gleiche Hoehen | Material + native Input + Button brechen um | einheitliche Hoehe `h-10`, `gap-3` |

Symptome: ueberlappende Eingabefelder in `mat-form-field`-Zeilen, inkonsistente
Radien, unsichtbare Buttons (`bg-primary` rendert transparent, wenn Token
fehlt), Drawer zu schmal, Filter nicht fluchtend.

---

## 2 Leitprinzipien

1. **Kein Angular Material.** `@angular/material`, `@angular/cdk` und
   `material-icons` verschwinden aus `package.json`. Kein `mat-*`-Element im
   Template, kein `mat.*`-Mixin im SCSS. ArchUnit-aehnlicher Lint-Check
   (ESLint-Regel) schlaegt bei Rueckfaellen fehl.
2. **Reines Tailwind + `@layer components`.** Komponenten-Kit ist eine duenne
   Schicht aus Angular-Standalone-Komponenten, die ausschliesslich Tailwind-
   Utilities und CSS-Tokens verwenden.
3. **Ein einziger Input-Stil.** Alle Eingabefelder - Text, Select, Datum,
   Suche - nutzen `h-10` (40 px), `rounded-lg`, Label oben ausserhalb, 2-px-
   Ring als Fokus. Damit fluchtet jede Filterzeile automatisch.
4. **Detailbereiche nutzen die volle Content-Breite.** Keine `max-w-5xl` mehr
   auf Detail-Seiten oder Drawer. Layout ueber 12-Spalten-Grid. Zwei-Spalten-
   Layouts erst ab `xl:` (1280 px+).
5. **Dialog-Rework.** Alle Dialoge (Bestaetigung, Freigabe, KI-Vorschlag,
   Reachability-Start, Profil-Diff, Tenant-Anlage usw.) werden in eine
   einheitliche `<cvm-dialog>`-Shell migriert: Header-Titel, Sektionen mit
   `--space-6` Abstand, Footer rechtsbuendig mit Primary+Secondary, `Esc`
   schliesst, `Enter` bestaetigt wenn Single-Primary, Fokus-Fang (Focus-Trap
   per CDK-Ersatz: native `<dialog>` oder minimaler Trap-Helper).
6. **Fluchten durch Rhythmus.** Vertikal: `--space-6` (24 px) zwischen
   Sektionen, `--space-4` (16 px) innerhalb. Filterzeile: `gap-3` (12 px),
   alle Elemente `h-10`.
7. **Icons: Lucide.** Ligatur-Font `material-icons` weicht `lucide-angular`.
   Mapping-Tabelle siehe Abschnitt 7.
8. **Fonts: Fira Sans + Fira Sans Condensed.** Body in Fira Sans, Headings
   in Fira Sans Condensed. `@fontsource/fira-sans-condensed` wird ergaenzt.
9. **Charts bleiben ECharts.** `ngx-echarts` bleibt, Theme ueber
   `ChartThemeService`, Tokens werden eingespeist. Monaco-Editor bleibt
   (eigenes Theme-System, kollidiert nicht).

---

## 3 Interaktionsdesign (vor dem Umbau)

Interaktionsmuster werden pro Komponenten-Primitiv definiert. Jede
Feature-Seite nutzt diese Primitive ohne Abweichung.

### 3.1 Button

| Variant | Klasse | Einsatz |
|---|---|---|
| primary | `.btn .btn-primary` | Haupt-Aktion pro Flaeche (maximal eine sichtbar) |
| secondary | `.btn .btn-secondary` | alternative Aktion, neutraler Kontext |
| ghost | `.btn .btn-ghost` | Toolbar, Tabellen-Aktionen |
| danger | `.btn .btn-danger` | Loeschen, Downgrade |
| icon | `.btn .btn-icon` | 40x40, Icon-only |

Zustaende: `hover`, `active`, `focus-visible` (2 px Ring
`--color-focus`), `disabled` (opacity-50, cursor-not-allowed).
Tastatur: `Enter` und `Space` loesen aus. Loading: Icon wird durch
Lucide-`Loader2`-Spin ersetzt, Text bleibt, Button disabled.

### 3.2 Input / Select / Textarea / Date

- Label oben, `text-sm font-medium text-text-muted mb-1`.
- Feld `h-10 rounded-lg border border-border bg-surface px-3 text-sm`.
- Fokus: `outline-none ring-2 ring-primary/40 border-primary`.
- Fehler: `border-severity-critical-bg ring-severity-critical-bg/30`, Meldung
  darunter in `text-xs text-severity-critical-bg`.
- Hilfetext: `text-xs text-text-muted` darunter.
- Pflichtfeld: Sternchen am Label in `text-severity-critical-bg`.
- Select (native): gleiche Metrik, Chevron-Icon rechts (Lucide `ChevronDown`).
- Textarea: `min-h-24`, `rounded-lg`, gleiche Fokus-Logik.
- Datum: native `<input type="date">` mit identischem Styling (Forms-Plugin
  normalisiert die Controls).

### 3.3 Card

- `.card`: `bg-surface rounded-xl shadow-card border border-border/60 p-6`.
- `.card-header`: `flex items-start justify-between mb-4`; Titel
  `text-title-sm`, Untertitel `text-caption`.
- `.card-footer`: Trenner `border-t border-border` + `pt-4 mt-4 flex items-center justify-end gap-2`.

### 3.4 Badge / Severity-Chip

- `.badge`: `inline-flex items-center h-6 rounded-pill px-2 text-xs font-medium`.
- Varianten: `.badge-neutral`, `.badge-success`, `.badge-warning`,
  `.badge-danger`, `.badge-info`.
- Severity-Chip nutzt Severity-Tokens (`--color-severity-*-bg` / `-fg`),
  Grossbuchstaben, `tracking-wide`.

### 3.5 Tabelle

- Container: `.table-card` (Card + `p-0 overflow-hidden`).
- Header: `bg-surface-muted text-xs font-semibold uppercase tracking-wide text-text-muted`.
- Zeilen: `border-t border-border hover:bg-surface-muted/60 transition-colors`.
- Dichte-Modi: `.table-compact` (44 px Zeilenhoehe), default (56 px).
- Sortierung: Klick auf Header, Icon `ChevronUp`/`ChevronDown`.
- Pagination: eigene `<cvm-paginator>`-Komponente mit `Previous`/`Next`,
  Seiten-Select und "x von y"-Text, fluchtet mit Filter-Zeile.
- Leerstand: `EmptyState`-Komponente statt `<p>Keine Daten</p>`.

### 3.6 Dialog (modaler Overlay)

- Shell: `<cvm-dialog [open]="..." [title]="..." [size]="md|lg|xl" (close)="...">`
- Overlay: `fixed inset-0 bg-black/40 backdrop-blur-sm z-40`.
- Panel: `bg-surface rounded-2xl shadow-lg w-[min(92vw,var(--dialog-size))] max-h-[85vh] overflow-hidden flex flex-col`.
- Header: `flex items-center justify-between px-6 py-4 border-b border-border`.
- Content: `px-6 py-6 overflow-y-auto grow`.
- Footer: `px-6 py-4 border-t border-border flex items-center justify-end gap-2`.
- Tastatur: `Esc` schliesst, `Tab`-Trap auf Panel-Elemente, `Enter` loest
  Primary bei Single-Input, Fokus wandert beim Oeffnen auf erstes Focusable
  oder auf `autofocus`.
- Animation: 160 ms Fade+Scale (von `scale-98` auf `scale-100`), respektiert
  `prefers-reduced-motion`.
- Grossen-Presets: `sm` 420 px, `md` 560 px, `lg` 720 px, `xl` 960 px.

### 3.7 Drawer (Seiten-Detail)

- Variante A **Inline-Panel**: Detail im gleichen Layout rechts, `w-full xl:w-[420px]`.
- Variante B **Rechtes Overlay**: `<cvm-drawer>`, slide-in von rechts, Breite
  `min(100vw, 720px)`, schliesst per `Esc` und Overlay-Klick.
- Beide: gleicher Header/Footer-Aufbau wie Dialog.
- **Wichtig**: Detail-Inhalte nutzen volle Breite (`w-full`), keine zentrierte
  `max-w-*`-Einschraenkung.

### 3.8 Toast / Banner

- `<cvm-banner variant="info|success|warning|critical">` - fix oben unter
  der Topbar, `rounded-lg`, Icon links, Close-Button rechts, 4 s Auto-Hide
  fuer Success, manuell fuer Warning/Critical.
- Queue: maximal drei gleichzeitig sichtbar, FIFO.

### 3.9 Tabs

- `<cvm-tabs [tabs]="..." [(active)]="...">`.
- Strich unter aktiven Tab (`border-b-2 border-primary`), inaktiv
  `text-text-muted hover:text-text`.
- Tastatur: `ArrowLeft`/`ArrowRight`, `Home`, `End`, `Enter` aktiviert.

### 3.10 Filter-Zeile (Standard)

```
+--------------------------------------------------------+
| [Suchfeld h-10 grow] [Select h-10] [Toggle h-10] [Btn] |
+--------------------------------------------------------+
```

- Container: `flex items-end gap-3 flex-wrap`.
- Alle Controls nutzen `.input-field` oder `.btn` - gleiche Hoehe, gleiche
  Baseline.
- Labels einheitlich oben, werden bei Toggles (z.B. "Nur KEV") durch
  `<cvm-switch [label]="...">`-Wrapper mit eigener Label-Zeile geloest, damit
  Toggle auf gleicher Baseline steht.

### 3.11 Navigation (Shell)

- Topbar `h-14` mit Brand links, Produkt-Switcher, Theme-Toggle, User-Menu
  rechts. Keine Mat-Toolbar.
- Sidebar `w-60` (collapsible auf `w-14` = Icon-Only).
- Sidebar-Gruppen: `text-xs uppercase tracking-wide text-text-subtle` als
  Label, 8 px Abstand ueber den Eintraegen.
- Eintrag: `h-10 rounded-lg px-3 flex items-center gap-3`, aktiv
  `bg-primary/10 text-primary font-medium`, Hover `bg-surface-muted`.
- Accordion-Gruppen (Einstellungen) sind als native `<details>` umgesetzt,
  kein `mat-expansion-panel`.

### 3.12 Charts

- ECharts bleibt, aber das Theme wird ueber `ChartThemeService` aus den
  neuen Tokens gespeist (`--color-primary`, `--color-severity-*`,
  `--color-text`, `--color-border`).
- Leerstand: einheitliches `EmptyState`-Kompnent statt Chart-eigener Placeholder.

---

## 4 Umbau-Iterationen (bindend, Reihenfolge fix)

| Iter | Name | Umfang |
|------|------|--------|
| **61A** | **Foundation** | `tailwind.config.js` erweitern (Tokens, `@tailwindcss/forms`, dark als `[data-theme="dark"]`), `styles.scss` ohne Material rebuilden, `@layer components` mit `.btn*`, `.input-field`, `.card`, `.badge*`, `.table-card`. Lucide + `@fontsource/fira-sans-condensed` einspielen. Mat-Icons-Import entfernen. Material-Theming-SCSS weg. |
| **61B** | **Shell** | `shell.component` von `mat-toolbar` + `mat-sidenav` auf Tailwind-Shell. Product-Switcher als eigener `<cvm-menu>`-Wrapper, User-Menu identisch. Sidebar mit `<details>` fuer Einstellungen. |
| **61C** | **Shared Primitives** | Neue Standalone-Komponenten: `CvmButton`, `CvmIconButton`, `CvmCard`, `CvmInput`, `CvmSelect`, `CvmTextarea`, `CvmSwitch`, `CvmCheckbox`, `CvmRadio`, `CvmBadge`, `CvmSeverityChip`, `CvmTable` (Hilfs-Direktiven), `CvmPaginator`, `CvmTabs`, `CvmDialog`, `CvmDrawer`, `CvmBanner`, `CvmMenu`, `CvmSpinner`, `CvmProgressBar`, `CvmEmptyState`, `CvmSearchBox`. |
| **61D** | **Listenseiten** | `cves`, `queue`, `rules`, `profiles`, `alerts-history`, `ai-audit`, `anomaly`, `waivers`, `fix-verification`, `reachability`, `reports`. Filter fluchten, Tabellen nutzen `CvmTable`, Pagination, EmptyState. |
| **61E** | **Detail-Seiten / Drawer** | `cve-detail`, `queue-detail`, `fix-verification`-Detail, `reachability`-Detail, Profile-Editor, Scan-Detail. Volle Breite, 12-Spalten-Grid, Drawer-Overlay. |
| **61F** | **Admin / Forms** | `admin-llm-configurations`, `admin-parameters`, `admin-products`, `admin-tenants`, `admin-environments`, `admin-theme`, `settings`, `scan-upload`, `components` (Showcase). Alle Formulare auf Reactive Forms + `CvmInput`/`CvmSelect`. |
| **61G** | **Dialoge** | Jeder Dialog wird in `CvmDialog`-Shell migriert: Freigabe-Dialog (Assessment), Downgrade-Vier-Augen, Reachability-Start, Profil-Diff, LLM-Konfigurations-Dialog, Tenant-Anlage, Waiver-Anlage, Fix-Verification-Start, Bestaetigungs-Dialog, Secret-Eingabe, Regel-Editor-Speichern. |
| **61H** | **Cleanup** | `@angular/material`, `@angular/cdk`, `material-icons` aus `package.json`. `dashboard-widgets` prueft auf `NoMaterialRule` (ESLint-Plugin `no-restricted-imports` + Template-Regex). Alle `mat-*`-Referenzen in SCSS entfernen. `styles.scss` finaler Clean-Up (keine Material-Palette-Definitionen mehr). Karma-Tests anpassen. Koverage-Audit. |

Jede Iteration hat:
1. Plan (`docs/YYYYMMDD/iteration-NN-plan.md`)
2. Fortschritt (`...-fortschritt.md`)
3. Test-Summary (`...-test-summary.md`)
4. Screenshot-Review aus `scripts/explore-ui` (Iteration D/E/F/G Pflicht).

---

## 5 Akzeptanzkriterien (harter Gate)

- [ ] `package.json` enthaelt **keine** Dependencies auf `@angular/material`,
      `@angular/cdk`, `material-icons`.
- [ ] `grep -r "mat-" cvm-frontend/src --include="*.html" --include="*.ts" --include="*.scss"` liefert **null Treffer**.
- [ ] `grep -r "from '@angular/material" cvm-frontend/src` liefert null Treffer.
- [ ] ESLint-Regel `no-restricted-imports` blockiert `@angular/material` und
      `@angular/cdk` (ausser fuer gezielte Utility-Module, falls zwingend).
- [ ] Dark-Mode haengt nur an `data-theme="dark"` (Tailwind `darkMode: ['class', '[data-theme="dark"]']`).
- [ ] Jede Filter-Zeile: Controls alle `h-10`, `gap-3`, `flex items-end`.
- [ ] Jede Detailseite nutzt `max-w-none w-full`, Grid-Layout.
- [ ] `@tailwindcss/forms` ist aktiv.
- [ ] `lucide-angular` ersetzt `material-icons`.
- [ ] Alle Dialoge laufen ueber `CvmDialog`, kein `MatDialog` mehr.
- [ ] UI-Exploration-Screenshots zeigen einheitliche Typografie, Farben,
      Abstaende auf allen Routen (Leitfragen aus `CLAUDE.md` 10 beantwortet).
- [ ] Karma + ESLint + Build gruen.

---

## 6 Risiken & Mitigation

| Risiko | Mitigation |
|---|---|
| Monaco-Editor bricht durch fehlende CDK-Overlays | Monaco verwendet eigenen Overlay-Mechanismus; kein CDK noetig. Test in `profiles` und `rules`. |
| ECharts-Tooltips / -Legenden mit Material-Schriftart | Explicit `textStyle.fontFamily: 'Fira Sans'` in `ChartThemeService`. |
| Keycloak-Login-Callback haengt an Material-Icons | Login-Callback nutzt nur Lucide nach Migration (Mapping: `check_circle` -> `CheckCircle2`). |
| Date-Picker fehlt ohne Material | native `<input type="date">` reicht; zusaetzlich optional `cvm-date-input`-Wrapper mit Mindestsupport. |
| Snackbar fehlt ohne Material | `CvmBanner`-Queue + `cvm-toast-host` im Shell. |
| Table-Sort/Filter-Logik aus `MatTable` | Sort-Logik ist schon in Services; Template-Migration genuegt. |
| Breaking Changes fuer Tests | Karma-Specs ziehen jeweilige Standalone-Komponenten, neue Selektoren `[data-testid]` statt `mat-*`. |

---

## 7 Icon-Mapping (Material Ligatur -> Lucide)

| Material | Lucide |
|---|---|
| `search` | `Search` |
| `close` | `X` |
| `check` | `Check` |
| `check_circle` | `CheckCircle2` |
| `warning` | `AlertTriangle` |
| `error` | `AlertCircle` |
| `info` | `Info` |
| `arrow_drop_down` / `expand_more` | `ChevronDown` |
| `expand_less` | `ChevronUp` |
| `chevron_right` | `ChevronRight` |
| `chevron_left` | `ChevronLeft` |
| `add` | `Plus` |
| `remove` | `Minus` |
| `delete` | `Trash2` |
| `edit` | `Pencil` |
| `save` | `Save` |
| `upload` | `Upload` |
| `download` | `Download` |
| `filter_list` | `Filter` |
| `refresh` | `RefreshCw` |
| `more_vert` | `MoreVertical` |
| `more_horiz` | `MoreHorizontal` |
| `settings` | `Settings` |
| `account_circle` | `CircleUser` |
| `logout` | `LogOut` |
| `login` | `LogIn` |
| `layers` | `Layers` |
| `shield` | `Shield` |
| `light_mode` | `Sun` |
| `dark_mode` | `Moon` |
| `visibility` | `Eye` |
| `visibility_off` | `EyeOff` |
| `content_copy` | `Copy` |
| `open_in_new` | `ExternalLink` |
| `play_arrow` | `Play` |
| `stop` | `Square` |
| `pause` | `Pause` |
| `radio_button_unchecked` | `Circle` |
| `clear` | `X` |

---

## 8 Umsetzungsreihenfolge in dieser Session

Da der Anwender nicht erreichbar ist, fuehrt Claude alle Iterationen
**sequenziell und ohne Rueckfrage** aus. Start: **61A Foundation** in diesem
Commit-Zyklus; jede Iteration endet mit `git add && git commit` mit
Conventional-Commit-Message und Jira-Key-Footer (`CVM-62`). Push erst nach
Abschluss der Gesamtmigration auf `claude/redesign-ui-consistency-pvbAQ`.
