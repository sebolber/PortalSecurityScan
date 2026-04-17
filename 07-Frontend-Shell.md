# Iteration 07 – Frontend-Shell (Angular 18, Keycloak, Dashboard-Gerüst)

**Jira**: CVM-16
**Abhängigkeit**: 00
**Ziel**: Arbeitsfähige Angular-Anwendung mit Login, Layout, Routing und
Dashboard-Gerüst.

---

## Kontext
Angular 18 Standalone Components, Keycloak-Integration wie in PortalCore.
Design angelehnt an adesso health solutions CI (Farben, Typografie aus
`cvm-frontend/src/assets/design-tokens.json` – zunächst Platzhalter, später
Feintuning).

## Scope IN
1. `keycloak-angular`-Integration, Runtime-Konfiguration via
   `assets/config.json` (Keycloak-URL, Realm, Client-ID, API-Base-URL).
2. Auth-Guard, Token-Injection via HTTP-Interceptor.
3. Rollen-basierte Menüsichtbarkeit: `CVE_VIEWER`, `CVE_ASSESSOR`,
   `CVE_APPROVER`, `CVE_ADMIN`, `PRODUCT_OWNER`, `AI_AUDITOR`.
4. Haupt-Layout:
   - Header mit Produkt-/Umgebungswähler (noch ohne Datenanbindung),
     User-Menü, Logout.
   - Sidebar mit Menüpunkten: Dashboard, Bewertungs-Queue, CVEs,
     Komponenten, Profile, Regeln, Berichte, KI-Audit (Admin).
   - Content-Area (Router-Outlet).
5. Dashboard-Gerüst (leere Cards mit Platzhaltern):
   - Offene CVEs gesamt
   - CVEs nach Severity (ECharts-Tortendiagramm)
   - Älteste offene kritische CVE
   - Ampel-Status „Weiterbetrieb möglich?"
6. HTTP-Client-Service `ApiClient` mit Basis-URL aus Config,
   Fehlerbehandlung (Snackbar bei 4xx/5xx).
7. i18n-Grundgerüst (Deutsch als Default, Platzhalter für Englisch).
8. Playwright-E2E-Smoke-Test: Login-Flow gegen Keycloak-Dev-Realm.

## Scope NICHT IN
- Queue-UI (Iteration 08).
- Profil-Editor (Iteration 18).
- Regel-Editor (später).
- Copilot-UI (Iteration 14).

## Aufgaben
1. Angular-Modul-Struktur:
   ```
   src/app/
     core/ (auth, api-client, config)
     shared/ (ui-komponenten, pipes)
     features/
       dashboard/
       queue/ (leerer Platzhalter, gefuellt in Iteration 08)
       cves/
       components/
       profiles/
       rules/
       reports/
       ai-audit/
   ```
2. Tailwind-Config mit adesso-Farben als CSS-Variablen.
3. Basis-Design: `<ahs-button>`, `<ahs-card>`, `<ahs-empty-state>`,
   `<ahs-severity-badge>` (mit Farbcode je Severity).
4. Config-Loader vor Bootstrap (`APP_INITIALIZER`).
5. Jasmine-Unit-Tests für Interceptor, Guard, Config-Loader.

## Test-Schwerpunkte
- `AuthInterceptorTest`: Token wird injiziert, 401 führt zu Logout.
- `RoleMenuServiceTest`: Menüeinträge sichtbar je Rolle.
- Playwright: Login → Dashboard sichtbar → Logout.
- Visual Regression (nice-to-have, nicht Pflicht): Screenshot der Shell.

## Definition of Done
- [ ] `npm start` zeigt Login-Redirect zu Keycloak-Dev.
- [ ] Nach Login ist Dashboard erreichbar.
- [ ] Menüeinträge je nach Rolle sichtbar/ausgegraut.
- [ ] Config via `config.json` austauschbar ohne Rebuild.
- [ ] Coverage Frontend (`cvm-frontend`) ≥ 70 %.
- [ ] Fortschrittsbericht.
- [ ] Commit: `feat(frontend): Angular-Shell mit Keycloak und Dashboard-Geruest\n\nCVM-16`

## TDD-Hinweis
Frontend-TDD: Tests für Services und kritische Logik (Interceptor, Guard).
Layout kann pragmatischer entwickelt werden, aber mit Playwright-Smoke abgesichert.

## Abschlussbericht
Standard, plus Screenshot der Shell unter `docs/YYYYMMDD/iteration-07-shell.png`.
