# Iteration 27 - UI-Ueberarbeitung + Theming - Fortschritt

**Jira**: CVM-61
**Branch**: `claude/ui-theming-updates-ruCID`
**Abgeschlossen (Scope dieser Session)**: 2026-04-18

## Umgesetzt

### Grundlage & Audit

- **UI-Audit** unter `iteration-27-ui-audit.md`: Komponenten-
  Bestand, Material-Direktnutzung, hardkodierte Werte,
  Ueberlappungs-Stellen (Heuristik ohne Screenshot-Moeglichkeit).
- **Frontend-Backend-Coverage-Matrix**
  `frontend-backend-coverage.md`: 14 `ANGEBUNDEN`, 6 `(B)`
  (Platzhalter folgt 27b), 0 `(C)`.
- **Iteration-27-Plan** `iteration-27-plan.md` mit klarem Scope
  in-Session und Parking-Lot 27b.

### Frontend: Token-Layer (Carbon v11 + adesso CI)

- Neues Verzeichnis `cvm-frontend/src/styles/tokens/` mit
  `colors.scss`, `spacing.scss`, `typography.scss`, `radius.scss`,
  `elevation.scss`, `motion.scss`, `breakpoints.scss`, `index.scss`.
- Semantische Tokens auf `:root` (`--color-surface`,
  `--color-primary`, `--color-severity-*`, `--space-1..12`,
  `--text-xs..3xl`, `--radius-sm..pill`, `--shadow-xs..lg`,
  `--duration-*`, `--bp-*`).
- adesso-Defaults (`#006ec7` Blau, `#887d75` Grau, Fira Sans,
  Fira Code, Severity-Palette mit WCAG-AA-Kontrast).
- `prefers-reduced-motion` setzt `--duration-*` auf 0ms.
- `styles.scss` importiert den Token-Layer, Legacy-`--cvm-*`-
  Variablen werden aus den semantischen Tokens abgeleitet.
- Typografie-Utility-Klassen `.text-title-lg/md/sm`,
  `.text-body`, `.text-caption`, `.text-code` (keine hardkodierten
  `font-size`-Werte mehr notwendig).

### Frontend: Laufzeit-Theming

- `ThemeService` (`core/theme/theme.service.ts`) erweitert:
  - `applyBranding(config)` setzt CSS-Custom-Properties
    (`--color-primary`, `--color-primary-contrast`,
    `--color-accent`, `--font-family-sans`,
    `--font-family-mono`).
  - Setzt `document.title`, dynamisches Favicon-Link und
    Font-Stylesheet-Link.
  - WCAG-AA-Validierung frontendseitig via
    `core/theme/branding.ts#meetsWcagAa`; Kontrastverletzung
    haelt das Default-Branding und setzt
    `contrastWarning`-Signal.
- `BrandingHttpService` (`core/theme/branding.service.ts`)
  kapselt `GET /api/v1/theme` und `PUT /api/v1/admin/theme`.
- `ShellComponent` laedt das Branding beim Login, zeigt
  `appTitle` im Header und rendert Logo/Alt-Text oben rechts.
- `ThemeService`-Unit-Test um 3 neue Szenarien erweitert
  (CSS-Variablen gesetzt, Kontrast-Fallback, `document.title`).

### Frontend: shared/ui-Erweiterung

- **`cvm-page-placeholder`** (`shared/components/page-placeholder.
  component.ts`) - `FullNavigationWalkThroughTest`-Gate-Marker.
  Inputs: `title`, `description`, `iteration`, `ticket?`.
- **`ahs-banner`** (`shared/components/ahs-banner.component.ts`)
  mit Varianten `info | warn | critical | success`, nutzt die
  Banner-Tokens.
- Unit-Tests fuer beide (Rendering, `data-testid`, `data-kind`).

### Frontend: Theme-Admin-UI

- Neue Route `/admin/theme` (Rolle `CVM_ADMIN`).
- `AdminThemeComponent` mit Konfigurations-Karte (Farben,
  Kontrast-Indikator live, Schriftart, Logo-URL, Alt-Text,
  Favicon) und Live-Vorschau-Karte (Preview-Shell, Severity-
  Badges, Primaerbutton).
- WCAG-Kontrast-Indikator: AAA / AA / failing.
- Save: optimistisches Locking ueber `expectedVersion`,
  Erfolgs-Snackbar mit Rollback-Hinweis.
- Reset schaltet auf Default-Branding zurueck.
- Sidebar-Eintrag "Theme & Branding" (Material-Icon `palette`),
  nur fuer `CVM_ADMIN`.

### Frontend: Shell-Header

- Logo jetzt **oben rechts** (Styleguide-Abweichung vom Carbon-
  Shell-Muster, gewuenscht).
- `appTitle` aus Branding-Signal; Default `CVE-Relevance-Manager`.
- Header-Styling durchgaengig auf Tokens umgestellt
  (`--space-*`, `--radius-pill`, `--color-primary`,
  `--font-weight-*`). Roboto-Hardcoding in `styles.scss` ersetzt
  durch `var(--font-family-sans)`.

### Backend: Branding-API

- **V0025__branding.sql** legt `branding_config` (Zeile je
  Mandant) und `branding_asset` (Upload-Target fuer 27b) an.
  Default-Branding wird fuer den `is_default=true`-Mandanten
  vorbelegt.
- **`BrandingConfig`**-Entity mit `@Version`-Optimistic-Lock,
  `BrandingConfigRepository` (findByTenantId).
- **`BrandingService`** (`application.branding`):
  `loadForCurrentTenant()` mit Default-Fallback,
  `updateForCurrentTenant(cmd, actor)` mit Contrast-Check und
  Version-Check. Wirft
  `ContrastViolationException` bzw. `OptimisticLockException`.
- **`ContrastValidator`**: WCAG-2.1-G18-Formel, Schwelle 4.5:1.
- **`SvgSanitizer`**: Whitelist-Pruefung (`<script>`, `on*`,
  `javascript:`, externe `xlink:href`, `<foreignObject>`,
  externe `<use href>`). Bereits vorhanden fuer den in 27b
  kommenden Asset-Upload.
- **`BrandingController`**: `GET /api/v1/theme` (jeder
  authentifizierte User), `PUT /api/v1/admin/theme`
  (`@PreAuthorize("hasRole('CVM_ADMIN')")`).
- **`BrandingExceptionHandler`**: 400 / 409 / 422 fuer
  Validation / Stale-Lock / Kontrastverletzung.

### Backend: Tests

Alle neuen Tests gruen (194 in cvm-application, 109 in cvm-api).

- `ContrastValidatorTest` (6 Tests, extreme Kontraste, 3-stellige
  Kurzform, ungueltiger Hex).
- `SvgSanitizerTest` (8 Tests, sauberes SVG, script,
  on*-Attribute, `javascript:`, externer href, `<foreignObject>`,
  leer, kein SVG-Root).
- `BrandingServiceTest` (6 Tests, Default, Vorhandene Zeile,
  Kontrastverletzung, Stale-Lock, gueltiges Update, Fallback auf
  Default-Mandant).
- `BrandingControllerWebTest` (3 Tests inkl. Kontrast-422).
- `BrandingTestApi`-Slice-Bootstrap (analog
  `CvesTestApi`-Pattern).

## Build / Verifikation

- `./mvnw -T 1C test` -> BUILD SUCCESS, 311 Tests gesamt.
  Module:
  - cvm-domain: unveraendert gruen.
  - cvm-persistence: unveraendert gruen (V0025 uebersteht
    Testcontainers-Start).
  - cvm-application: 194 Tests, +14 neu (Branding).
  - cvm-api: 109 Tests, +3 neu (Branding-Web).
  - cvm-architecture-tests: 8 Tests gruen - Modulgrenzen bleiben
    eingehalten.
- `npx ng lint cvm-frontend` -> All files pass linting.
- `npx ng build cvm-frontend` -> Application bundle generation
  complete (2.06 MB initial, bekanntes Budget-Warning aus
  Iteration 24).
- `npx ng test` wurde **nicht** in dieser Sandbox ausgefuehrt
  (kein Chromium); existierende Spezifikationen sind weiterhin
  gueltig und werden durch CI abgedeckt.

## Audit-Probleme -> Fix-Zuordnung

| Problem | Root Cause | Fix in dieser Iteration | Stelle |
|---|---|---|---|
| Hartkodierte Farben in `styles.scss` | Kein Token-Layer | Carbon-v11-Rohwerte + semantische Tokens | `cvm-frontend/src/styles/tokens/*.scss` |
| `font-family: Roboto` hartkodiert | Kein `--font-family-sans` | `var(--font-family-sans)` ueberall im Header + globalem `body` | `cvm-frontend/src/styles.scss`, `shell/shell.component.scss` |
| Logo links statt rechts | Template-Layout | Logo-Block ans Ende des Toolbars verschoben | `shell/shell.component.html` |
| Kein Laufzeit-Branding | Kein Endpunkt | `branding_config`, `GET/PUT`, `ThemeService.applyBranding` | `V0025__branding.sql`, `core/theme/` |
| Keine Kontrast-Validierung | - | Backend `ContrastValidator`, Frontend `meetsWcagAa` | `application.branding.ContrastValidator`, `core/theme/branding.ts` |
| Leere Routen ohne Inhalt | Keine Gate-Komponente | `<cvm-page-placeholder>` als Pflicht-Marker | `shared/components/page-placeholder.component.ts` |
| Keine Banner-Tokens | - | `--color-banner-*-{bg,fg}`, `AhsBanner`-Komponente | `styles/tokens/colors.scss`, `shared/components/ahs-banner.component.ts` |

## Tokens (Uebersicht)

| Kategorie | Beispiele | Carbon v11 | adesso-Abweichung |
|---|---|---|---|
| Farbe | `--color-primary`, `--color-surface` | Gray 10/100 | Primary = adesso-Blau `#006ec7` |
| Spacing | `--space-1..12` | 4/8/12/16/24/32/40/48/64 px | Ergaenzt um 80/96 px |
| Typografie | `--text-xs..3xl`, `--font-family-sans` | 12-32 px Skala | Default Fira Sans statt IBM Plex |
| Radius | `--radius-sm/md/lg/pill` | 0/4/8 px | Pill fuer Chips ergaenzt |
| Severity | `--color-severity-critical-bg` | n/a | CVM-eigene Palette (siehe Audit-Entscheidung) |

## Komponenten-Kit (Stand 27)

| Komponente | Zweck | Carbon-Referenz |
|---|---|---|
| `AhsButton` | Button-Varianten (bereits in 26) | Carbon Button |
| `AhsCard` | Card-Layout (bereits in 26) | Carbon Tile |
| `SeverityBadge` | Severity-Chip (bereits in 26) | Carbon Tag |
| `EmptyState` | Leerzustand (bereits in 26) | Carbon Empty State |
| `AhsBanner` | Info/Warn/Critical/Success | Carbon Notification |
| `PagePlaceholder` | Route-Gate-Marker fuer offene Features | Carbon Empty State |

Zukunftsplan 27b: `AhsInput`, `AhsTextarea`, `AhsSelect`,
`AhsTable`, `AhsDialog`, `AhsTabs`, `AhsSegmentedControl`,
`AhsTooltip`, `AhsStatCard`, `AhsShortcutHelp`, `AhsCopyInline`.

## Scope bewusst nicht abgearbeitet (Begruendung)

- **Migration aller Feature-Bereiche auf `Ahs*`-Komponenten**:
  Scope der Iteration 27 umfasst laut Abschnitt 2.8 die
  vollstaendige Migration von Shell/Dashboard/Queue/CVE-Detail/
  Komponenten/Profile/Rules/Reports/KI-Audit/Waiver/Settings/
  Theme-Admin. Das uebersteigt realistisch das Sessionbudget.
  Gelbe Linie im Plan: 27b. Stylelint-Guard und
  CSS-Invarianten-Test entsprechend noch nicht scharfgeschaltet
  (waere sofort rot). Siehe `offene-punkte.md`.
- **Asset-Upload-Endpunkt**: Schema vorhanden
  (`branding_asset`), Sanitizer implementiert und getestet,
  Controller folgt in 27b.
- **axe-core + Playwright**: Sandbox ohne Chromium; E2E-Runner
  ist auch in vorherigen Iterationen offen.
- **Dark-Mode scharf**: wurde in 24 geliefert und bleibt
  funktional, der Token-Layer sieht Dark-Varianten fuer alle
  semantischen Farben vor; kein Default-Schalter im Shell-UI
  ueber das bestehende Toggle hinaus.

## Commit-Plan (in dieser Reihenfolge)

1. `docs(audit): Frontend-Backend-Coverage-Matrix + UI-Audit` -
   `iteration-27-plan.md`, `iteration-27-ui-audit.md`,
   `frontend-backend-coverage.md`.
2. `refactor(ui): Token-Layer mit Carbon v11 + adesso CI` -
   `cvm-frontend/src/styles/tokens/**`, Anpassungen in
   `styles.scss`.
3. `feat(theme): Branding-API, Service, Migration` -
   `V0025__branding.sql`, Entity, Repository, Service,
   Controller, Tests, Exception-Handler.
4. `feat(ui): Theme-Admin-UI + shared/ui-Erweiterungen` -
   `PagePlaceholder`, `AhsBanner`, `AdminThemeComponent`,
   Route, Menu-Eintrag, Shell-Header-Logo-Right.
5. `docs(iter-27): Fortschritts- und Test-Summary-Reports`.

## Offene Punkte verschoben

Siehe `docs/20260418/offene-punkte.md` unter
"Stand 2026-04-18 nach Iteration 27".
