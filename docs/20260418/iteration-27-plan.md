# Iteration 27 - UI-Ueberarbeitung + Theming - Plan

**Jira**: CVM-61
**Branch**: `claude/ui-theming-updates-ruCID`
**Stand**: 2026-04-18

## Scope (realistisch fuer diese Session)

Die Iteration 27 beschreibt einen grossen Umbau (Token-Layer,
shared/ui-Kit, Migration aller Feature-Bereiche, Theme-Admin-UI,
Coverage-Audit). Das vollstaendige Durchmigrieren *jedes*
Feature-Bereichs auf `Ahs*`-Komponenten ist in einer Session
nicht leistbar. Entsprechend setzt diese Session die Grundlage
und die besonders sichtbaren Teile um; die Feature-fuer-Feature-
Migration wird als Folge-Arbeit unter `offene-punkte.md`
dokumentiert.

### Muss (in dieser Session)

1. **Audit-Dokument** `iteration-27-ui-audit.md` (Bestandsaufnahme
   Komponenten, Inline-Styles, Uebeblappungs-Problemstellen).
2. **Frontend-Backend-Coverage-Matrix**
   `frontend-backend-coverage.md` mit Status und Aktion je
   Pruefbereich (Abschnitt 2.0.2 der Iteration).
3. **Token-Layer** unter `cvm-frontend/src/styles/tokens/` mit
   Carbon-v11-Rohwerten, adesso-Overrides (Blau `#006ec7`,
   Fira Sans).
4. **Backend: Branding-API** (V0025__branding.sql,
   `BrandingController` GET/PUT, Kontrast-Validierung,
   SVG-Sanitizer mit Unit-Tests).
5. **ThemeService-Erweiterung**: laedt Branding per
   `GET /api/v1/theme` und spiegelt Farbe/Logo/Title ins
   `documentElement`. Fehlkonfiguration fallt auf Default zurueck.
6. **shared/ui-Erweiterung**: `<cvm-page-placeholder>`
   (Platzhalter-Banner mit Iterations-Referenz) fuer
   `FullNavigationWalkThroughTest`-Sicherung, plus `AhsBanner`.
7. **Theme-Admin-UI** `/admin/theme` (Rolle CVE_ADMIN):
   Farbwahl, App-Title, Logo-URL, Live-Vorschau-Card.
8. **Shell-Header**: Logo rechts (neben User-Menu),
   App-Title aus Branding-Konfig, Produktwahl links.
9. **Fortschritts-/Test-Summary-Reports**.

### Nicht in dieser Session (auf offene-punkte.md)

- Vollstaendige Migration von Queue, Dashboard, Reports,
  KI-Audit etc. auf `Ahs*`-Komponenten (Schritte 2-10 der
  Iteration-2.8-Liste). Bleibt fuer Folge-Iteration (27b).
- FontAwesome-Thin-Icon-Umstellung (haengt an Lizenz-
  Klaerung, siehe 1A offene Punkte 1-3).
- Playwright-E2E-axe-core-Durchlauf
  (`FullNavigationWalkThroughTest` wird als Coverage-Gate
  dokumentiert, aber das Karma/Playwright-Setup haengt an
  dev-Toolchain, die in dieser Session nicht vollstaendig
  zur Verfuegung stand).
- Asset-Upload-Endpoint (`POST /api/v1/admin/theme/assets`)
  - stattdessen werden Logo- und Favicon-URLs vorerst als
  externe URLs gepflegt; das DB-Schema ist jedoch fuer den
  spaeteren Asset-Upload vorbereitet.
- Stylelint-Guard: Konfiguration-Stub ja, harte CI-Aktivierung
  folgt in 27b wenn alle Komponenten migriert sind
  (sonst waere die Pipeline sofort rot).

## Arbeitsreihenfolge

1. Audit + Coverage-Matrix schreiben (Basis fuer alle
   nachfolgenden Entscheidungen).
2. Token-Layer anlegen + in `styles.scss` einbinden.
3. DB-Migration + JPA-Entity + Repository.
4. Service (Contrast-Validator, SVG-Sanitizer) + Tests.
5. Controller GET/PUT + Tests.
6. Frontend ThemeService um Branding-Loader erweitern +
   Test.
7. AhsPagePlaceholder + AhsBanner als Standalone-Components
   mit Unit-Tests.
8. Admin-Theme-Component + Route.
9. Shell-Anpassung (Logo-Position, App-Title-Binding).
10. `./mvnw test` + `npm run lint` + `npm run build`
    durchlaufen lassen.
11. Berichte schreiben.
12. Commits + Push.
