# Iteration 25 - Einstellungen + Regeln + Profile-UI - Fortschritt

**Jira**: CVM-56
**Branch**: `claude/iteration-22-continuation-GSK7w`
**Abgeschlossen**: 2026-04-18

## Umgesetzt

### Backend

- **`EnvironmentQueryService`** + **`EnvironmentView`-Record**
  (cvm-application, Iteration-25-eigene Read-Schicht).
- **`ModelProfileQueryService`** + **`ModelProfileView`-Record**.
- **`EnvironmentsController`**
  (`GET /api/v1/environments`, authenticated).
- **`LlmModelProfilesController`**
  (`GET /api/v1/llm-model-profiles`, CVM_ADMIN).
- **2 WebMvcTests** (2 Tests + 2 Tests = 4 neue Tests). Der
  bestehende `ModelProfileControllerWebTest` bekam ein
  zusaetzliches `@MockBean ModelProfileQueryService`, weil der
  Package-ComponentScan beide Controller laedt.

### Frontend - Services

- `EnvironmentsService` (list).
- `ModelProfileService` (list + switch).
- `RulesService` (list + activate + dry-run).
- `ProfilesService` (aktives Profil pro Umgebung, 404 ->
  `null`).

### Frontend - Seiten

- **`SettingsComponent`** neu (`/settings`, Menue-Eintrag fuer
  alle Fach-Rollen):
  - User-Preferences: Theme-Toggle, Sprache (Dropdown aus
    `LocaleService`), Standard-Produkt
    (`localStorage` `cvm.default-product`).
  - Admin (nur CVM_ADMIN): pro Umgebung Dropdown aller
    Modellprofile, Vier-Augen-Confirmer-Feld, Begruendung,
    Submit-Button -> POST `/environments/{id}/model-profile/switch`.
    Vier-Augen-Regel wird Client-seitig gegen den eingeloggten
    User vorvalidiert; der Server ist weiterhin die harte
    Autoritaet.
- **`RulesComponent`** ersetzt den EmptyState durch eine
  Accordion-Liste aller Regeln:
  - Status-Chip (DRAFT/ACTIVE/RETIRED), Severity, Version.
  - Condition-JSON und Rationale-Template im Detail.
  - `Dry-Run`-Button (180 Tage) fuer
    `CVM_RULE_AUTHOR|APPROVER|ADMIN`.
  - `Aktivieren`-Button nur bei DRAFT fuer
    `CVM_RULE_APPROVER|ADMIN`.
- **`ProfilesComponent`** ersetzt den EmptyState:
  - listet alle Umgebungen auf, holt pro Umgebung das aktive
    Profil (oder zeigt "kein aktives Profil").
  - zeigt Freigabe-Meta + YAML-Quelle (read-only).

### Spec-Updates

- `role-menu.service.spec.ts`: Settings-Assertions ergaenzt,
  AI-Auditor-Test angepasst (enthaelt nun auch Settings).

## Build

- `./mvnw -T 1C test` -> BUILD SUCCESS,
  cvm-api 102 Tests (+4 neue), Rest unveraendert.
- `npx ng lint` -> All files pass linting.
- `npx ng build` -> Application bundle generation complete
  (2.05 MB initial, bekannte Budget-Warnung, keine Regression).

## Nicht in 25

- **CVE- und Komponenten-UI** (Iteration 26, eigene
  Backend-Endpunkte erforderlich).
- **Rules-Editor fuer Create/Update** - nur List + Activate +
  Dry-Run. Autor-Workflow ueber REST bleibt.
- **Profil-Editor (YAML-Edit)** - nur Read-only-Anzeige.
- **Tenant-Verwaltung**: Admin-Sektion zeigt nur den
  `tenant`-String der Umgebung; Tenant-CRUD-UI folgt wenn
  Rollout beginnt.
- **Karma-Lauf**: Sandbox ohne Chromium (bekannt).
