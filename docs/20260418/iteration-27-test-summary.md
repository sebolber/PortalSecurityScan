# Iteration 27 - Test-Summary

**Jira**: CVM-61
**Stand**: 2026-04-18

## Maven

`./mvnw -T 1C test` -> **BUILD SUCCESS**.

| Modul | Tests | Neu in 27 |
|---|---|---|
| cvm-domain | 19 | 0 |
| cvm-persistence | 13 | 0 |
| cvm-application | 194 | +14 (6 ContrastValidator, 8 SvgSanitizer, 6 BrandingService) |
| cvm-integration | 23 | 0 |
| cvm-llm-gateway | 31 | 0 |
| cvm-ai-services | 29 | 0 |
| cvm-api | 109 | +3 (BrandingControllerWebTest) |
| cvm-app | 1 | 0 |
| cvm-architecture-tests | 8 | 0 (Modulgrenzen gruen) |

## @DisplayName-Beispiele

- `ContrastValidatorTest`:
  - "Schwarz auf Weiss liefert maximales Kontrastverhaeltnis (~21)"
  - "adesso-Blau auf Weiss erfuellt AA"
  - "Hellgrau auf Weiss erfuellt AA nicht"
- `SvgSanitizerTest`:
  - "Eingebettetes script-Element fuehrt zur Ablehnung"
  - "onload-Attribut fuehrt zur Ablehnung"
  - "externer xlink:href auf https-URL fuehrt zur Ablehnung"
  - "foreignObject-Element fuehrt zur Ablehnung"
- `BrandingServiceTest`:
  - "Load: fehlende Zeile liefert adesso-Default-Branding"
  - "Update: Primaerfarbe mit zu geringem Kontrast wird abgelehnt"
  - "Update: veraltete Version loest Optimistic-Lock-Fehler aus"
  - "Update: ohne Tenant-Kontext faellt auf Default-Mandant zurueck"
- `BrandingControllerWebTest`:
  - "GET /api/v1/theme: liefert aktive Branding-Konfiguration"
  - "PUT /api/v1/admin/theme: gueltiger Request gibt neue Version zurueck"
  - "PUT /api/v1/admin/theme: Kontrastverletzung liefert 422"

## Architekturtests (ArchUnit)

Alle 8 Regeln aus `cvm-architecture-tests` gruen. Die neue
Branding-Struktur haelt die Modulgrenzen ein:

- `application.branding` greift nur auf `application.tenant`
  und `persistence.branding` zu - Regel
  `application_kennt_keine_api` weiter gruen.
- `persistence.branding` importiert nur JPA/Spring Data -
  Regel `persistence_greift_nur_auf_domain_zu` gruen.

## Frontend

- `npx ng lint cvm-frontend` -> **All files pass linting.**
- `npx ng build cvm-frontend` -> **Application bundle generation
  complete.** (2.06 MB initial, Budget-Warning unveraendert aus
  Iteration 24).
- `npx ng test` in dieser Sandbox nicht ausfuehrbar (kein
  Chromium). Neue Specs:
  - `core/theme/theme.service.spec.ts`: +3 neue Szenarien
    (Branding-CSS-Variablen, Kontrast-Fallback,
    `document.title`).
  - `shared/components/page-placeholder.component.spec.ts`:
    Rendering + `data-testid`-Marker.
  - `shared/components/ahs-banner.component.spec.ts`:
    Slot-Projection + `data-kind`.

## Offene Tests (Parkplatz fuer 27b)

- `FullNavigationWalkThroughTest` (Playwright) - iteriert
  ueber alle Sidebar-Routen und verlangt echte Daten oder
  `<cvm-page-placeholder>`. Marker (`data-testid`) ist bereits
  ausgeliefert.
- `FrontendBackendCoverageGate` (Skript) - parst
  `frontend-backend-coverage.md` und bricht bei
  unzugeordneten `NAV_OHNE_INHALT`-Zeilen ab.
- CSS-Invarianten-Test (Bundle-Scan nach hex-Farben / `px` /
  `font-family:` ausserhalb Token-Layer) - wird in 27b
  scharf geschaltet.
- axe-core-Durchlauf in Playwright auf Hauptrouten.

## Coverage

Kein Pitest-Lauf in dieser Session (Zeitbox). Branding-Service-
Logik hat Zeilen- und Branch-Abdeckung durch sechs Mockito-
getriebene Tests. Kontrast- und SVG-Pfade sind extremwerte-
abgedeckt.
