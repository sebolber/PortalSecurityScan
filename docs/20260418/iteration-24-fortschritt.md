# Iteration 24 - UI-Ueberarbeitung + Theming - Fortschritt

**Jira**: CVM-55
**Branch**: `claude/iteration-22-continuation-GSK7w`
**Abgeschlossen**: 2026-04-18

## Umgesetzt

### 1. Rollen-Konstanten synchron zum Realm (Iteration 23)

`cvm-frontend/src/app/core/auth/cvm-roles.ts`:

- Neue Rollen-Konstanten: `REVIEWER`, `PROFILE_AUTHOR`,
  `PROFILE_APPROVER`, `RULE_AUTHOR`, `RULE_APPROVER`,
  `REPORTER`.
- Entfernt: nicht-existenter `PRODUCT_OWNER`.
- Neue Map `CVM_ROLE_LABELS` (Rolle -> human-readable
  Kurzform) fuer die Role-Chips im Userpanel.

### 2. RoleMenuService + Routen

- `RoleMenuService` listet jetzt jede Sidebar-Zeile mit den
  korrekten Realm-Rollen:
  - Queue: `ASSESSOR|REVIEWER|APPROVER|ADMIN`,
  - Profile: `PROFILE_AUTHOR|PROFILE_APPROVER|ADMIN`,
  - Regeln: `RULE_AUTHOR|RULE_APPROVER|ADMIN`,
  - Berichte: `VIEWER|REPORTER|ADMIN`,
  - Komponenten: `VIEWER|REPORTER|ADMIN`,
  - Dashboard: alle Fachrollen + Admin,
  - KI-Audit: `AI_AUDITOR|ADMIN` (unveraendert).
- `app.routes.ts` Guards mit denselben Rollen verdrahtet.
  `authGuard` liest `data.requiredRoles` bereits -> keine
  zusaetzliche Guard-Logik noetig.

### 3. ThemeService + Light/Dark-Toggle

- `core/theme/theme.service.ts` mit Signal
  `mode: 'light' | 'dark'`.
- Initialisierung: `localStorage.cvm.theme` hat Vorrang,
  sonst `matchMedia('(prefers-color-scheme: dark)')`.
- Toggle setzt `data-theme`-Attribut am `<html>`-Element
  und persistiert.
- `tailwind.config.js` nutzt
  `darkMode: ['class', '[data-theme="dark"]']`,
  `styles.scss` verwendet CSS-Custom-Properties
  (`--cvm-bg`, `--cvm-surface`, `--cvm-primary`,
  `--cvm-text`, ...) plus Angular-Material-Dark-Theme
  via `@include mat.all-component-colors(...)`.
- Toggle-Button im Header (`light_mode`/`dark_mode`-Icon).

### 4. Shell-Header

`shell.component.*`:

- Logo-Block `shield` + `CVM`-Schriftzug (Link zum
  Dashboard).
- Header hat jetzt einen CI-Rot-Verlauf
  (`--cvm-primary` -> `--cvm-primary-dark`).
- Userpanel zeigt den Benutzernamen und darunter
  Rollen-Chips (nur die Rollen, die im Frontend einen
  `CVM_ROLE_LABELS`-Eintrag haben, damit Noise-Rollen
  nicht durchleuchten).
- Login/Logout-Menu bleibt erhalten, neuer Theme-Toggle
  als `mat-icon-button` zwischen Produkt-Auswahl und
  Userpanel.

### 5. Tests

- `role-menu.service.spec.ts` neu geschrieben:
  Viewer, Approver, Reviewer, Profile-Author,
  Rule-Approver, Reporter, AI-Auditor, Admin,
  leere Rolle, hasAccess - 10 Tests.
- `theme.service.spec.ts` neu:
  init setzt data-theme, toggle wechselt, persistiert,
  liest Persistenz beim Start - 4 Tests.

## Build

- `./mvnw -T 1C test` -> **BUILD SUCCESS**, 475 Tests
  (unveraendert gegenueber Iteration 23).
- `npx ng lint` -> **All files pass linting**.
- `npx ng build` -> **Application bundle generation
  complete** (2.05 MB initial - die bereits bekannte
  Budget-Warnung bleibt; keine neuen Regressions).

## Nicht in dieser Iteration

- Ueberarbeitung der Einzelseiten (Queue-Redesign,
  Dashboard-Widgets) - pro Feature separat.
- i18n-EN-Texte + echter Locale-Switch - weiterhin offen.
- Keycloak-Login-Theme (Keycloak-Seitiges adesso-CI-Skin).
- Bundle-Budget-Reduktion (separate Iteration).
- Playwright-E2E-Smoke fuer den Theme-Toggle
  (Sandbox ohne Chromium).
