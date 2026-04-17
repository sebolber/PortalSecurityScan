# Iteration 07 – Fortschrittsbericht

**Jira**: CVM-16
**Datum**: 2026-04-17
**Ziel**: Lauffaehige Angular-18-Shell mit Keycloak-Integration, Layout
und Dashboard-Geruest. REST-Anbindung folgt ab Iteration 08.

## 1 Was wurde gebaut

### Bootstrap & Config (`core/config`)
- `AppConfigService.load()` greift `assets/config.json` ab und cached
  Keycloak/API-URL.
- `APP_INITIALIZER` (Reihenfolge: Config, dann Keycloak) im
  `app.config.ts`.

### Keycloak (`core/auth`)
- `keycloak-init.ts` initialisiert `KeycloakService` mit
  `onLoad: 'check-sso'`, `silentCheckSsoRedirectUri`, PKCE.
- `AuthService` als reaktiver Wrapper (Signals fuer `loggedIn`,
  `userRoles`, `username`).
- `authGuard` (CanActivateFn): triggert Login bei fehlender Session,
  prueft `data.requiredRoles` aus Route oder fragt fallback
  `RoleMenuService.hasAccess(...)`.
- `authInterceptor`: setzt Bearer-Token nur fuer API-URLs, triggert
  Logout bei 401, laesst Asset-Calls in Ruhe.
- `RoleMenuService` mappt Keycloak-Rollen auf Sichtbarkeit der Menue-
  Eintraege (Roles in `cvm-roles.ts`: VIEWER, ASSESSOR, APPROVER, ADMIN,
  PRODUCT_OWNER, AI_AUDITOR).

### REST-Client (`core/api`)
- `ApiClient` mit `get/post/put` und URL-Bildung aus
  `AppConfigService.apiBaseUrl`.
- `HttpErrorHandler` zeigt deutschsprachige Snackbar fuer
  0/401/403/404/409/4xx/5xx.

### i18n (`core/i18n`)
- `LocaleService` mit `de`-Default, Locale-Persistenz im
  `localStorage`.
- `messages.de.ts` als statisches Sprach-Map.

### Shared UI (`shared/components`)
- `<ahs-card>` (Material-Card mit Hover-Schatten).
- `<ahs-button>` (Variants `primary`/`secondary`/`danger`).
- `<ahs-empty-state>` (Icon + Titel + Hinweis).
- `<ahs-severity-badge>` (Tailwind-Farben pro Severity-Stufe).
- ESLint-Selector-Regel auf `['cvm', 'ahs']` erweitert.

### Layout (`shell`)
- Header: Brand, Produkt-/Umgebungs-Wahl (statische Optionen),
  User-Menu mit Logout / Login-Button (je nach Session-Status).
- Sidebar mit dynamisch gefiltertem Menue (`menuEintraege` =
  `RoleMenuService.visibleEntries(userRoles())`).
- Content-Area mit `<router-outlet>`.

### Routing & Features (`features/*`)
- Lazy-Routes fuer alle 8 Featurebereiche (dashboard, queue, cves,
  components, profiles, rules, reports, ai-audit), jeweils
  `authGuard`-geschuetzt mit passenden `requiredRoles`.
- Skeletons mit `<ahs-empty-state>` und kontextuellem Hinweis.
- Dashboard-Geruest mit vier Cards: offene CVEs, Severity-Donut
  (ECharts), aelteste CRITICAL-CVE, Ampel "Weiterbetrieb moeglich?".

### Tests (Unit, ohne Browser)
- `RoleMenuServiceTest` (6 Faelle).
- `AppConfigServiceTest` (2 Faelle, HttpClientTesting).
- `AuthInterceptorTest` (3 Faelle: Bearer-Header, Asset-Schutz,
  401-Logout).
- `ApiClientTest` (3 Faelle: URL-Bildung, GET-OK, 5xx-Errorhandler).

## 2 Was laenger dauerte
- **`@typescript-eslint/parser` fehlte**. Die Iteration-00-eslint-Config
  konnte TypeScript-Decorators nicht parsen. Loesung: Parser ergaenzt
  (devDependency), zusaetzlich Selector-Prefix-Liste auf
  `['cvm', 'ahs']` erweitert. `npm run lint` ist nun gruen.
- **`ngx-echarts`-Provider**. Angular-18-Standalone braucht
  `provideEchartsCore({ echarts })` im `app.config.ts`. Pie+Tooltip+
  Legend-Module werden explizit registriert, damit das Bundle nicht
  unbenoetigte Charts traegt.
- **`EChartsOption`-Typing**. Initiale Definition als Plain-Object
  brach die strikte Template-Typisierung; Loesung: Cast auf
  `EChartsOption` aus `echarts/core`.
- **Karma im Sandbox-Build**: ohne Headless-Chrome-Binary nicht
  ausfuehrbar (`CHROME_BIN`-Fehler). Spezifikationen wurden trotzdem
  geschrieben und sind syntaktisch ueber `ng build` mitkompiliert &mdash;
  ein Lauf wartet auf eine CI-Umgebung mit Headless-Chrome.

## 3 Abweichungen vom Prompt
1. **Playwright-E2E nicht ausgefuehrt** (Spec markiert als
   "nice-to-have"; Sandbox hat keinen Browser-Stack).
2. **Karma-Tests geschrieben aber nicht gelaufen** &mdash; siehe oben.
3. **`<ahs-button>` wraped lediglich `mat-flat-button`** ohne eigenen
   Theme-Token. Iteration 08 kann den Button auf eigene Tailwind-Klassen
   umstellen, sobald die Material-Variante zu eng wird.
4. **Tailwind-Severity-Klassen** sind im Severity-Badge inline; eine
   Tailwind-Plugin-Variante (`severity-bg-critical` etc.) waere sauberer
   und folgt mit Iteration 08, wenn die Queue-Tabelle die gleichen
   Farben weiterverwendet.

## 4 Entscheidungen fuer Sebastian
- Soll der Default-Login wie bei PortalCore ein `onLoad: 'login-required'`
  sein, statt `check-sso`? Aktuell wartet die Shell auf eine geschuetzte
  Route, bevor der KC-Redirect ausgeloest wird.
- Soll die Produkt-/Umgebungs-Wahl im Header im LocalStorage gehalten
  werden (UX) oder pro Tab fluechtig?
- `LocaleService`: bereits vorbereitet auf EN-Variante. Ab wann sollen
  englische Texte gepflegt werden?

## 5 Naechster Schritt
**Iteration 08 &mdash; Bewertungs-Queue-UI** (CVM-17). Die
`QueueComponent`-Skeleton wird mit dem `ApiClient` an
`GET /api/v1/findings` aus Iteration 06 verdrahtet, plus Detail-Maske,
Approve/Override/Reject und Severity-Filter.

## 6 Build-/Lint-Status
```
npm run build            BUILD SUCCESS (5.73 MB initial)
npx ng lint              All files pass linting
ng test                  ChromeHeadless nicht verfuegbar (Sandbox)
./mvnw -T 1C -pl
  cvm-architecture-tests test BUILD SUCCESS
```
- Backend bleibt unveraendert (`./mvnw test` von Iteration 06: 137
  gruen, 11 Docker-skip).

---

*Autor: Claude Code.*
