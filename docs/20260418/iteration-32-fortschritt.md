# Iteration 32 – Fortschritt

**Thema**: UI-Exploration-Findings behoben
**Jira**: CVM-74
**Datum**: 2026-04-18

## Umgesetzt

### HIGH

- **HIGH-1 Material-Icons laden** - `index.html` laedt
  `Material+Icons` und `Fira Sans` vom Google-Fonts-CDN. Das behebt
  die 2-3-Buchstaben-Fallbacks auf allen Screens. Offline-Paket bleibt
  als TODO (siehe offene-punkte.md).
- **HIGH-2 Top-Nav-Overflow** - Rollen-Pills wandern ins User-Menue;
  im Header nur noch kompakte "N Rollen"-Zusammenfassung mit Tooltip
  (`cvm-frontend/src/app/shell/shell.component.{html,ts,scss}`).
- **HIGH-3 Keycloak-CORS** -
  `AuthService.refreshFromKeycloak` ruft kein `loadUserProfile()`
  mehr, stattdessen wird der Name aus dem ID-Token gelesen. Zusaetzlich
  `webOrigins: ["http://localhost:4200", "+"]` im Dev-Realm, damit
  andere Aufrufe nicht mehr geblockt werden
  (`infra/keycloak/dev-realm.json`).
- **HIGH-4 NG0600 Signal-Write in Effect** - `QueueComponent`
  markiert beide `effect()`-Instanzen mit
  `{ allowSignalWrites: true }`. Die Queue kann damit wieder
  Daten laden (war vorher leer).

### MEDIUM

- **MEDIUM-1 Severity-Farben auf `/cves`** - `.cvm-sev-chip` nutzt
  die Token aus `colors.scss`
  (`var(--color-severity-informational-bg)` etc.). INFORMATIONAL
  ist jetzt blau, nicht grau.
- **MEDIUM-2 `/profiles` Empty-State** - Neue Card mit Icon, Text und
  CTA `Umgebungen anlegen` (Link auf `/admin/environments`). Wird
  nur gerendert, wenn `rows().length === 0`.
- **MEDIUM-3 Admin-Theme-Text** - Header und Save-Snackbar versprechen
  kein 24-h-Fenster mehr, sondern verweisen auf die Historie.
- **MEDIUM-4 Admin-Theme History/Rollback-UI** - Neue Card
  "Historie &amp; Rollback" ruft `GET /admin/theme/history`,
  zeigt pro Eintrag Version, Primaerfarbe-Swatch, Titel, Schriftart,
  Zeitpunkt, Nutzer und einen `Auf vN zuruecksetzen`-Button, der
  `POST /admin/theme/rollback/{version}` aufruft (mit
  `window.confirm`-Bestaetigung). `BrandingHttpService` bekommt dazu
  `history()` und `rollback()`-Methoden sowie das Interface
  `BrandingHistoryEntry`.
- **MEDIUM-5 `/reports` UUID-Dropdowns** - `ReportsComponent`
  laedt in `ngOnInit` `ProductsService.list()` + pro Produkt
  `versions()` plus `EnvironmentsService.list()`. Die Felder
  "Produkt-Version" und "Umgebung" sind Dropdowns, wenn der
  Katalog erreichbar ist; ansonsten bleibt das UUID-Textfeld
  als Fallback sichtbar, und ein Hinweis erklaert den Grund.
- **MEDIUM-6 H1-Konsistenz** - `/settings`, `/rules`, `/components`,
  `/cves` bekommen `text-title-lg`-Klasse. Die `ai-audit`-Seite
  bleibt bewusst in der `ahs-card`-Variante (Diskussion in
  offene-punkte.md).

### LOW

- **LOW-1** entfiel: die "doppelten Input-Felder" waren Rendering-
  Artefakte der fehlenden Icon-Font. Nach HIGH-1 sind sie weg.
- **LOW-2** Akzentfarbe-Swatch ist jetzt analog zu Primaer-/Kontrast-
  farbe vorhanden. Leere Werte zeigen ein diagonales Muster.
- **LOW-3** `Farbschema`-Slider ist jetzt ein Segment-Control
  Hell / Dunkel mit Icon.
- **LOW-4** Severity-Quicktoggles auf `/cves` haben einen farbigen
  Top-Border pro Severity.
- **LOW-5** `/rules` Empty-State erklaert die Rolle von Regeln im
  Bewertungsprozess und die noetigen Rollen.

## Dateien

- `cvm-frontend/src/index.html`
- `cvm-frontend/src/app/shell/shell.component.{html,ts,scss}`
- `cvm-frontend/src/app/core/auth/auth.service.ts`
- `cvm-frontend/src/app/features/queue/queue.component.ts`
- `cvm-frontend/src/app/features/cves/cves.component.{html,scss}`
- `cvm-frontend/src/app/features/profiles/profiles.component.{html,ts,scss}`
- `cvm-frontend/src/app/features/admin-theme/admin-theme.component.{html,ts,scss}`
- `cvm-frontend/src/app/core/theme/branding.service.ts`
- `cvm-frontend/src/app/features/reports/reports.component.{html,ts}`
- `cvm-frontend/src/app/features/settings/settings.component.{html,ts}`
- `cvm-frontend/src/app/features/rules/rules.component.{html,scss}`
- `cvm-frontend/src/app/features/components/components.component.html`
- `infra/keycloak/dev-realm.json`

## Verifikation

- `./mvnw -T 1C test` &rarr; **BUILD SUCCESS**.
- `npx ng build` &rarr; erfolgreich (5.99 MB Initial Bundle).
- `npx ng test` konnte in dieser Sandbox nicht laufen
  (kein Chromium installiert).

## Offene Punkte

- Nur noch "Material-Icons offline haerten" (Self-Hosting-Paket statt
  Google-Fonts-CDN) sowie der bestehende Karma-Sandbox-Punkt.
