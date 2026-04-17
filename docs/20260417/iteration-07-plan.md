# Iteration 07 – Plan (Frontend-Shell, CVM-16)

*Stand: 2026-04-17*

## Ziel
Lauffaehige Angular-18-Shell mit Keycloak-Login, Layout, Routing und
Dashboard-Geruest. Backend-Bindings (REST aus Iteration 02-06) bleiben
Platzhalter; konkrete Daten folgen ab Iteration 08.

## Umfang
1. **Bootstrap & Config**
   - `AppConfigService.load()` via `APP_INITIALIZER` vor Bootstrap.
   - `assets/config.json` als runtime-konfigurierbare Quelle (Keycloak,
     API-Base-URL).
2. **Keycloak**
   - `KeycloakService` aus `keycloak-angular` initialisieren
     (`onLoad: 'check-sso'`, `silentCheckSsoRedirectUri`).
   - `AuthGuard` (`CanActivateFn`).
   - `AuthInterceptor` injiziert Bearer-Token, behandelt 401 mit Logout.
   - `RoleMenuService` mappt Keycloak-Rollen auf Menue-Sichtbarkeit.
3. **Layout (Shell)**
   - Header: Produkt-/Umgebungs-Wahl (statisch, "Auswahl folgt"),
     Benutzer-Dropdown, Logout.
   - Sidebar mit Voll-Menue: Dashboard, Bewertungen, CVEs, Komponenten,
     Profile, Regeln, Berichte, KI-Audit (Admin).
   - Content-Area mit `<router-outlet>`.
4. **Feature-Routes**
   - `dashboard`, `queue`, `cves`, `components`, `profiles`, `rules`,
     `reports`, `ai-audit`. Jeweils Standalone-Component mit
     Platzhalter-Inhalt; durch `AuthGuard` + Rollenliste geschuetzt.
5. **Dashboard-Cards**
   - Offene CVEs gesamt (Kennzahl-Card)
   - CVEs nach Severity (ECharts-Donut)
   - Aelteste offene kritische CVE (Kennzahl-Card)
   - Ampel "Weiterbetrieb moeglich?" (Status-Card mit Severity-Badge)
6. **API-Client**
   - `ApiClient` mit Methoden `get<T>(path)`, `post<T>(path, body)`,
     `put<T>(path, body)`. URL-Praefix aus `AppConfigService`.
   - `HttpErrorHandler` zeigt Snackbar bei 4xx/5xx.
7. **Shared UI**
   - `<ahs-card>`, `<ahs-button>`, `<ahs-empty-state>`,
     `<ahs-severity-badge>` (Severity-Farbcode aus Tailwind-Theme).
8. **i18n-Grundgeruest**
   - `LocaleService` haelt aktuelle Sprache (`de`/`en`), liest aus
     `localStorage`, default `de`. Texte ueber `core/i18n/messages.de.ts`.
9. **Tailwind**
   - adesso-Farbpalette als CSS-Variablen + Severity-Farben.

## TDD-Reihenfolge
1. `AppConfigService` (sync get vor load wirft).
2. `RoleMenuService` (welche Rollen sehen welches Menue).
3. `AuthInterceptor` (Token-Header, 401-Logout).
4. `ApiClient` (URL-Bildung, GET/POST).
5. `ShellComponent` Smoke-Test (Default-Menue, Render).
6. `DashboardComponent` Smoke-Test (Cards rendern).

## Out-of-Scope
- Playwright-E2E (erfordert Keycloak-Dev-Realm; bleibt Backlog).
- Reale Daten (folgt Iteration 08).
- Profil-Editor / Regel-Editor (Iteration 18 / spaeter).
- KI-Copilot UI (Iteration 14).

## Risiken
- `keycloak-angular` 16 + Angular 18 verlangen `provideAppInitializer`
  mit asynchroner Promise-Kaskade (erst Config laden, dann Keycloak).
  Loesung: zwei separate `APP_INITIALIZER`-Tokens, in Reihenfolge.
- Karma-Tests in dieser Sandbox: Headless-Chrome benoetigt; falls nicht
  verfuegbar, fallen Karma-Tests aus. Tests muessen mit
  `ChromeHeadlessNoSandbox`-Launcher laufen oder bei fehlender Browser-
  Umgebung als Skip dokumentiert werden.
