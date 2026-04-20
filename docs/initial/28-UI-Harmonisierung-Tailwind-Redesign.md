# Iteration 28 – UI-Harmonisierung: Reines Tailwind-Redesign nach PortalCore-Vorbild

**Jira**: CVM-62
**Abhängigkeit**: Iteration 27 wird durch diese Iteration **überschrieben** in Bezug auf die Technologiebasis. Fachliche Anforderungen aus 27 (Token-Layer, Komponenten-Kit, Theming-API, Coverage-Audit) bleiben gültig und werden hier fortgeführt.
**Status**: OFFEN – mehrstufige Umsetzung über die Aufgabenliste in Abschnitt 6.
**Ziel**: Komplettumbau der Angular-Oberfläche auf ein **reines Tailwind-Design-System ohne Angular Material**, angelehnt an die harmonische, moderne Anmutung des PortalCore-Frontends. Beseitigung der aktuell sichtbaren Probleme (überlappende Eingabefelder, unruhige Filter-Zeilen, inkonsistente Abstände, schmale Detailbereiche mit ungenutztem Whitespace).

---

## 0 Ausgangslage

Der aktuelle Zustand von `cvm-frontend/` kombiniert **Angular Material 18** (Material-Theme über `mat.m2-define-light-theme`) mit **Tailwind 3.4**. Diese Mischung ist die Hauptursache für die vom Fachbereich beanstandeten UI-Probleme:

- Material-Formfields bringen eigene Paddings, Höhen und Wrapper-DOMs mit. Tailwind-Utilities (`px-4 py-2`, `gap-*`) auf einem `<mat-form-field>` brechen dessen internes Layout – **Eingabefelder überlappen**.
- Zwei Dark-Mode-Selektoren (`data-theme='dark'` für Material, `class='dark'` für Tailwind-Overrides) – einige Bereiche schalten nicht korrekt um.
- Token-Kollisionen: `--cvm-*` Legacy-Aliase, `--color-*` neue semantische Tokens und Material-eigene Theme-Variablen überschreiben sich gegenseitig.
- Keine einheitlichen Komponenten-Klassen für Inputs, Cards, Buttons – jedes Feature stylt selbst.
- Detailbereiche (CVE-Detail, Profil-Editor, KI-Audit-Drawer) nutzen feste Breiten statt der vollen Content-Breite.
- Filter-Zeilen fluchten nicht: Material-Select, native `<input>` und `<button>` haben unterschiedliche Höhen, Label-Positionen und Radii.

Das PortalCore-Frontend zeigt, wie es sauber geht: **keine Material-Komponenten**, reines Tailwind mit `@layer components` (`.btn-primary`, `.card`, `.input-field`, `.badge`), Lucide-Icons, Fira Sans + Fira Sans Condensed, klare `--portal-*`-Tokens. Das ist die Zielarchitektur für diese Iteration.

---

## 1 Leitprinzipien

1. **Kein Angular Material mehr.** Alle `@angular/material`- und `@angular/cdk`-Dependencies werden aus `package.json` entfernt. Kein `mat.*`-Mixin in SCSS. Kein `<mat-*>`-Element in Templates. Der CI-Invariantentest aus Abschnitt 5 schlägt bei Rückfällen fehl.
2. **Reines Tailwind + `@layer components`.** Das Komponenten-Kit ist eine dünne Schicht aus Angular-Standalone-Components, die ausschließlich Tailwind-Utilities und Token-basierte CSS-Custom-Properties verwenden. Vorbild: `frontend/src/styles.scss` aus PortalCore.
3. **Ein einziger Eingabefeld-Stil.** Jedes Eingabefeld – egal ob Textfeld, Select, Datumspicker, Suche – nutzt die gleiche Höhe (40 px im `md`-Default), den gleichen Radius (`rounded-lg`), das gleiche Label-Muster (oben, außerhalb des Feldes) und die gleiche Fokus-Behandlung (2-px Ring in `--color-primary`). Damit fluchten Filter- und Formularzeilen automatisch.
4. **Detailbereiche nutzen die volle Content-Breite.** Keine festen `max-w-*`-Werte außer der globalen Seiten-Max-Breite. Detail-Seiten und Drawer verwenden ein durchgängiges 12-Spalten-Grid, das auf dem verfügbaren Platz atmet. Zwei-Spalten-Layouts nur in `xl:`-Breakpoints aufwärts.
5. **Harmonie durch Spacing-Rhythmus.** Vertikaler Abstand zwischen Sektionen: immer `--space-6` (24 px). Innerhalb einer Sektion: `--space-4` (16 px). Innerhalb einer Filter-Zeile: `gap-3` (12 px). Keine Ausnahmen, keine Ad-hoc-Werte.
6. **Icon-System: Lucide.** Ersetzt `material-icons`. Lucide ist schlanker (SVG, keine Ligatur-Font), konsistent im Strichgewicht, bereits im PortalCore etabliert.
7. **Schriften: Fira Sans + Fira Sans Condensed.** Body in Fira Sans, Überschriften in Fira Sans Condensed. Keine zweite Font-Familie. Lokal ausgeliefert.
8. **State-of-the-Art-Muster**: großzügige Whitespace-Nutzung, subtile Schatten (`shadow-card`) statt harter Border, klare Typografie-Hierarchie, Badges mit Soft-Backgrounds (`bg-primary/10 text-primary`), Fokus-States mit Ring statt Outline, `prefers-reduced-motion` respektiert.

---

## 2 Ziel-Architektur

### 2.1 Dependencies (`package.json`)

**Entfernen**:
- `@angular/material`
- `@angular/cdk`
- `material-icons`

**Hinzufügen**:
- `@tailwindcss/forms` (Input-Reset für konsistentes Basis-Styling)
- `lucide-angular` (Icon-System)
- `@fontsource/fira-sans-condensed` (Headlines-Schrift)

Bleibt:
- `tailwindcss`, `autoprefixer`, `postcss`
- `@fontsource/fira-sans`
- `echarts`, `ngx-echarts` (Charts – Theme über `ChartThemeService`)
- `ngx-monaco-editor-v2` (Regel-Editor – Editor hat eigenes Theme-System, kollidiert nicht)
- `keycloak-angular`, `keycloak-js`

### 2.2 Token-Layer (`cvm-frontend/src/styles/tokens/`)

CSS-Custom-Properties auf `:root`, keine SCSS-Variablen mehr:

```scss
:root {
  /* Farben – semantisch */
  --color-primary: #0B5FFF;
  --color-primary-dark: #0847C2;
  --color-primary-light: #E6EEFF;
  --color-primary-contrast: #FFFFFF;

  --color-surface: #FFFFFF;
  --color-surface-muted: #F7F8FA;
  --color-surface-raised: #FFFFFF;
  --color-border: #E4E7EC;
  --color-border-strong: #D0D5DD;

  --color-text: #101828;
  --color-text-muted: #667085;
  --color-text-subtle: #98A2B3;

  --color-focus: #0B5FFF;

  /* Severity */
  --color-severity-critical: #D92D20;
  --color-severity-critical-bg: #FEE4E2;
  --color-severity-high: #F79009;
  --color-severity-high-bg: #FEF0C7;
  --color-severity-medium: #EAAA08;
  --color-severity-medium-bg: #FEF7C3;
  --color-severity-low: #12B76A;
  --color-severity-low-bg: #D1FADF;
  --color-severity-informational: #2E90FA;
  --color-severity-informational-bg: #D1E9FF;

  /* Spacing – 4-Punkt-Skala */
  --space-1: 4px;  --space-2: 8px;  --space-3: 12px; --space-4: 16px;
  --space-5: 20px; --space-6: 24px; --space-7: 32px; --space-8: 40px;
  --space-9: 48px; --space-10: 64px; --space-11: 80px; --space-12: 96px;

  /* Radius */
  --radius-sm: 4px;  --radius-md: 8px;  --radius-lg: 12px; --radius-pill: 9999px;

  /* Elevation */
  --shadow-xs: 0 1px 2px rgba(16,24,40,0.05);
  --shadow-sm: 0 1px 3px rgba(16,24,40,0.08), 0 1px 2px rgba(16,24,40,0.04);
  --shadow-md: 0 4px 8px -2px rgba(16,24,40,0.10), 0 2px 4px -2px rgba(16,24,40,0.06);
  --shadow-lg: 0 12px 16px -4px rgba(16,24,40,0.08), 0 4px 6px -2px rgba(16,24,40,0.03);

  /* Typografie */
  --font-family-sans: 'Fira Sans', system-ui, sans-serif;
  --font-family-heading: 'Fira Sans Condensed', 'Fira Sans', system-ui, sans-serif;
  --font-family-mono: 'Fira Code', 'Fira Mono', ui-monospace, monospace;

  --text-xs: 12px; --text-sm: 14px; --text-base: 16px;
  --text-lg: 18px; --text-xl: 20px; --text-2xl: 24px; --text-3xl: 30px;

  /* Motion */
  --duration-fast: 120ms;
  --duration-base: 180ms;
  --easing-standard: cubic-bezier(0.2, 0, 0, 1);
}

[data-theme='dark'] {
  --color-surface: #101828;
  --color-surface-muted: #0C1322;
  --color-surface-raised: #182230;
  --color-border: #1D2939;
  --color-border-strong: #344054;
  --color-text: #F9FAFB;
  --color-text-muted: #98A2B3;
  --color-text-subtle: #667085;
}
```
