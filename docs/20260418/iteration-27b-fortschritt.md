# Iteration 27b - Folge-Arbeit (Placeholder + Asset-Upload)

**Jira**: CVM-62
**Branch**: `claude/ui-theming-updates-ruCID`
**Stand**: 2026-04-18

Nachgelagerter Arbeitsblock auf derselben Branch nach Iteration 27.
Ziel: die in 27 als "(B) Platzhalter" markierten Bereiche mit
konkreten Route-Komponenten anbinden und das Asset-Upload-Backend
liefern, damit das Theme-Admin-UI in einer Folge-Iteration die
Datei-Uploads wirklich nutzen kann.

## Umgesetzt

### Frontend: sechs Platzhalter-Routen

Jede Route verwendet `<cvm-page-placeholder>` und vermerkt die
Folge-Iteration. Das erfuellt das Kriterium des geplanten
`FullNavigationWalkThroughTest` (jede Route zeigt Daten oder
Platzhalter).

| Route | Komponente | Sidebar-Icon | Zuweisung |
|---|---|---|---|
| `/waivers` | `WaiversComponent` | `rule_folder` | (B) Iter 20 |
| `/alerts/history` | `AlertsHistoryComponent` | `history` | (B) Iter 09 |
| `/reachability` | `ReachabilityComponent` | `account_tree` | (B) Iter 15 |
| `/fix-verification` | `FixVerificationComponent` | `verified` | (B) Iter 16 |
| `/anomaly` | `AnomalyComponent` | `sensors` | (B) Iter 18 |
| `/tenant-kpi` | `TenantKpiComponent` | `insights` | (B) Iter 21 (Cross-Tenant) |

Sidebar-Eintraege im `RoleMenuService` ergaenzt (Rollenmodell je
Bereich angepasst: Waiver sichtbar fuer Assessor/Reviewer/Approver,
Anomaly nur AI_AUDITOR/ADMIN, Tenant-KPI nur ADMIN).

### Backend: Asset-Upload-Endpoint

- **`BrandingAsset`**-Entity + `BrandingAssetRepository`
  (`cvm-persistence/.../branding`). Schema war in Iteration 27
  bereits angelegt.
- **`BrandingAssetService`** mit `upload(...)` (Contrast-/MIME-/
  Groessen-/SVG-Pruefung + SHA-256 + Tenant-Kontext) und
  `findById(...)` (Download-Projektion ueber `BrandingAssetView`).
- **`BrandingController`** erweitert um:
  - `POST /api/v1/admin/theme/assets` (Multipart,
    `@PreAuthorize("hasRole('CVM_ADMIN')")`),
  - `GET /api/v1/theme/assets/{assetId}` (Byte-Auslieferung,
    ETag aus SHA-256, `Cache-Control: public, max-age=3600`).
- **Architekturkonform**: Der Controller arbeitet nur gegen die
  Application-Schicht; der ArchUnit-Gate
  `api_greift_nicht_direkt_auf_persistence_zu` bleibt gruen.
- Grenzen:
  - Logo/Favicon max. 512 KB, Font max. 2 MB.
  - MIME-Whitelist: Logo = `image/svg+xml|image/png`, Favicon =
    `image/x-icon|image/png|image/svg+xml|image/vnd.microsoft.icon`,
    Font = `font/woff2|application/font-woff2`.
  - SVGs durchlaufen den bestehenden `SvgSanitizer`.

### Token-Migration (erster Schwung)

- `EmptyStateComponent` auf Tokens umgestellt - Inline-
  Tailwind-Zinc- und `fontSize`-Hardcoding entfernt.
- `QueueTableComponent` nutzt jetzt `--color-primary-muted`
  statt `rgba(226, 0, 26, 0.08)` fuer die Zeilenauswahl.
- `DashboardComponent`-ECharts-Definition ruft weiterhin
  `#fff` fuer den Pie-Slice-Rand auf - das bleibt offen, bis
  der geplante `ChartThemeService` die Tokens an ECharts
  spiegelt (siehe offene-punkte.md).

### Stylelint-Config-Stub

- `cvm-frontend/.stylelintrc.cvm27.json` legt die kuenftigen
  harten Regeln ab (no-hex, erlaubte `font-family`/`font-size`
  nur als `var(...)`), noch **nicht** im CI scharf geschaltet,
  weil die Feature-Migration nicht abgeschlossen ist.
  Aktivierung: `npx stylelint --config ...` im CI vor `ng build`.

### Tests

- `BrandingAssetServiceTest` (6 Tests): sauberes SVG, Script-
  Ablehnung, MIME-Ablehnung, Groessen-Ablehnung, Font-MIME,
  Leer-Ablehnung.
- `BrandingControllerWebTest` um 2 Tests erweitert
  (Logo-Upload, Asset-Download mit ETag). Gesamt: 5 Tests.

### Coverage-Matrix

`frontend-backend-coverage.md` aktualisiert: keine
`NAV_OHNE_INHALT`-Zeile mehr, sechs `PLATZHALTER`-Eintraege
(jeweils mit aktiver Route und (B)-Aktion).

## Build / Verifikation

- `./mvnw -T 1C test` -> BUILD SUCCESS. 318 Tests gesamt
  (vorher 311), davon +7 Backend-Tests in dieser Session.
- `./mvnw -pl cvm-architecture-tests test` gruen (nach
  Refactoring des Controllers zur reinen Application-Ebene).
- `npx ng lint cvm-frontend` -> All files pass linting.
- `npx ng build cvm-frontend` -> Application bundle generation
  complete.

## Offene Punkte (weiterhin fuer 27c)

- Basis-Listen fuer die sechs Platzhalter-Bereiche (Waiver,
  Alerts-Historie, Reachability, Fix-Verifikation, Anomalie,
  Cross-Tenant).
- `ChartThemeService`, der ECharts-Farben aus Tokens generiert.
- Vollstaendige Token-Migration aller Feature-Bereiche.
- Stylelint-Guard im CI scharfschalten.
- `FullNavigationWalkThroughTest` (Playwright).
