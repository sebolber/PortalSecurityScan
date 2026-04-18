# Frontend-Backend-Coverage-Matrix (Iteration 27)

**Stand**: 2026-04-18
**Jira**: CVM-61

Status-Werte: `ANGEBUNDEN` / `NAV_OHNE_INHALT` / `FEHLT_GANZ` /
`BACKEND_FEHLT`. Aktion: `(A)` Anbinden, `(B)` Platzhalter,
`(C)` aus Navigation entfernen.

## Matrix

| Iteration | Backend-Endpunkt | Frontend-Route | Komponente | Status | Aktion |
|---|---|---|---|---|---|
| 07 | `GET /api/v1/alerts/banners` | `/dashboard` | `DashboardComponent`, `AlertBannerComponent` | ANGEBUNDEN | - |
| 21 | `GET /api/v1/kpi/tenant`, `GET /api/v1/kpi/trend` | `/dashboard` | `DashboardComponent` | ANGEBUNDEN | - |
| 08 | `GET /api/v1/queue`, `POST /api/v1/assessments` | `/queue` | `QueueComponent` + Kinder | ANGEBUNDEN | - |
| 03/04/11/26 | `GET /api/v1/cves` | `/cves` | `CvesComponent` | ANGEBUNDEN | - |
| 02/26 | `GET /api/v1/products`, `GET /api/v1/products/{id}/versions` | `/components` | `ComponentsComponent` | ANGEBUNDEN | - |
| 04 | `GET /api/v1/profiles/aktiv?...` | `/profiles` | `ProfilesComponent` | ANGEBUNDEN | - |
| 05/17 | `GET /api/v1/rules`, `POST /api/v1/rules/dry-run` | `/rules` | `RulesComponent` | ANGEBUNDEN | - |
| 10/19 | `GET /api/v1/reports`, `GET /api/v1/reports/{id}` | `/reports` | `ReportsComponent` | ANGEBUNDEN | - |
| 11 | `GET /api/v1/ai-audit` | `/ai-audit` | `AiAuditComponent` | ANGEBUNDEN | - |
| 22 | `GET /api/v1/settings/*` | `/settings` | `SettingsComponent` | ANGEBUNDEN | - |
| 27 | `GET /api/v1/theme`, `PUT /api/v1/admin/theme` | `/admin/theme` | `AdminThemeComponent` | ANGEBUNDEN (diese Iteration) | - |
| 06 | `GET /api/v1/assessments/findings/{id}` | `/queue` (Detail) | `QueueDetailComponent` | ANGEBUNDEN | - |
| 09 | `GET /api/v1/alerts/history` | `/alerts/history` | `AlertsHistoryComponent` | ANGEBUNDEN | - (seit 27c) |
| 10 | `GET /api/v1/reports/archive` | `/reports` | `ReportsComponent` | ANGEBUNDEN (Archiv-Sicht) | - |
| 13 | `GET /api/v1/ai-queue` | - | - | BACKEND_FEHLT | (B) - Vorbewertungs-Queue nutzt zentrale Queue, eigene Ansicht in 27b |
| 14 | `POST /api/v1/copilot/sessions` | `/queue` (Detail) | `QueueDetailComponent` (embedded) | ANGEBUNDEN | - |
| 15 | `GET /api/v1/reachability/{id}` | `/reachability` | `ReachabilityComponent` (Placeholder) | PLATZHALTER | (B) - Board folgt, Platzhalter aktiv seit 27b |
| 16 | `GET /api/v1/fix-verification/{id}` | `/fix-verification` | `FixVerificationComponent` (Placeholder) | PLATZHALTER | (B) - Board folgt, Platzhalter aktiv seit 27b |
| 17 | `GET /api/v1/rule-suggestions` | `/rules` (Tab) | `RulesComponent` | ANGEBUNDEN | - |
| 18 | `GET /api/v1/anomaly`, `POST /api/v1/profile-assistant` | `/anomaly` | `AnomalyComponent` (Placeholder) | PLATZHALTER | (B) - Board folgt, Platzhalter aktiv seit 27b |
| 19 | `POST /api/v1/nl-query` | `/reports` (Exec) | `ReportsComponent` (Exec-Tab) | ANGEBUNDEN | - |
| 20 | `GET /api/v1/waivers` | `/waivers` | `WaiversComponent` | ANGEBUNDEN | - (seit 27c) |
| 21 | `GET /api/v1/kpi/tenant` (Cross-Tenant) | `/tenant-kpi` | `TenantKpiComponent` (Placeholder) | PLATZHALTER | (B) - Cross-Tenant-View folgt, Platzhalter aktiv seit 27b |

## Zusammenfassung

- **ANGEBUNDEN**: 16 Bereiche (Dashboard, Queue, CVEs,
  Komponenten, Profile, Regeln, Reports, KI-Audit,
  Settings, Theme-Admin, Queue-Detail, Copilot,
  Rule-Suggestions, NL-Query, Waiver-Liste seit 27c,
  Alert-Historie seit 27c).
- **PLATZHALTER** - 4 Bereiche (je mit aktiver Route und
  `<cvm-page-placeholder>`): Reachability-Board,
  Fix-Verifikation, Anomalie-Board, Cross-Tenant-Dashboard.
  `FullNavigationWalkThroughTest` darf dank des Platzhalters
  gruen bleiben.
- **(A) Sofortanbindung in 27**: Theme-Admin. Die in 2.0.4
  priorisierten Basis-Listen (CVE-Browser, Komponenten-
  Browser, Profile, Report-Archiv) waren bereits durch
  Iteration 26 und fruehere Schritte abgedeckt.
- **(C) aus Navigation entfernen**: keine.
- **Keine `NAV_OHNE_INHALT`-Zeilen mehr**: Coverage-Gate-
  Voraussetzung erfuellt.

## Konsequenz fuer `FullNavigationWalkThroughTest`

Alle Menuepunkte in `role-menu.service.ts` fuehren aktuell
auf angebundene Seiten. Platzhalter-Menuepunkte
(Alert-Historie, Waiver, Reachability, Fix-Verifikation,
Anomalie, Cross-Tenant) werden in 27b ergaenzt, sobald die
jeweiligen Komponenten samt `<cvm-page-placeholder>`
existieren - der Gate-Test muss bis dahin diese Routen nicht
abdecken.

## Konsequenz fuer Go-Live-Checkliste

Abschnitt 1 der Checkliste wird um einen Prueftschritt
erweitert: "Aktuelle `frontend-backend-coverage.md` liegt
vor, alle `NAV_OHNE_INHALT`-Zeilen sind einer Aktion
zugeordnet." - uebernommen in `offene-punkte.md`.
