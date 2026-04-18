# Iteration 25 - Einstellungen + Regeln + Profile-UI - Plan

**Jira**: CVM-56
**Branch**: `claude/iteration-22-continuation-GSK7w`
**Datum**: 2026-04-18

## Auftrag

Fuege eine Einstellungen-Seite hinzu (Benutzer + Admin) und
verwandle die Placeholder-Seiten `Regeln` und `Profile` in
funktionale UIs gegen die bereits bestehenden Backend-Endpunkte.

## Scope

### Backend (klein, Read-only)

- **Neuer Controller** `EnvironmentReadController`
  (`GET /api/v1/environments`): liefert alle Umgebungen als
  View-Records (id, key, name, stage, tenant,
  llmModelProfileId). Read-only; Schutz: authenticated.
- **Neuer Controller** `LlmModelProfileReadController`
  (`GET /api/v1/llm-model-profiles`): alle Profile als
  Views (id, profileKey, provider, modelId,
  costBudgetEurMonthly, approvedForGkvData). Fuer
  Admin-Dropdowns im Settings-Admin-Bereich.
- View-Records liegen in `cvm-application` (Modulgrenze).

### Frontend

- **Neue Route** `/settings` mit Eintrag in der Sidebar
  (sichtbar fuer alle eingeloggten Fach-Rollen).
- **SettingsComponent** mit zwei Abschnitten:
  1. **Benutzer-Preferences**: Theme (Light/Dark/System),
     Locale-Dropdown (DE/EN), Default-Produkt
     (persistent in `localStorage`).
  2. **Admin**: sichtbar nur mit `CVM_ADMIN`.
     - Umgebungs-Liste mit aktuell zugeordnetem
       Modell-Profil + Dropdown zum Wechsel (inkl.
       Vier-Augen-Confirmer-Feld + Begruendung).
     - Tenants-Teaser (aus dem Default-Tenant-Seed).
     - Link auf Alert-Konfiguration (bestehender
       Admin-Endpunkt).

- **RulesComponent**: Liste aus `GET /api/v1/rules`,
  Status-Spalte, Detail-Aufklappen mit Condition-JSON +
  Rationale-Template; Buttons `Dry-Run` und
  `Aktivieren` (nur fuer User mit entsprechender Rolle).
- **ProfilesComponent**: Umgebungs-Liste (aus dem neuen
  Endpunkt) + Anzeige der aktuell aktiven Profil-YAML pro
  Umgebung.

### Services (Frontend)

- `EnvironmentService`, `ModelProfileService`,
  `RulesService`, `ProfilesService` in `core/`.
- Integration mit `ApiClient`.

### Tests

- Karma-Specs fuer die Services.
- Backend-WebMvcTests fuer die beiden Read-Controller
  (mindestens happy-path).

## Nicht in 25

- CVE- und Komponenten-Detail-UIs (Iteration 26).
- Rules-Editor inkl. Create/Edit (bisher nur Aktivierung/Dry-Run).
- Profil-Editor (nur Read; YAML-Edit in Iteration 18-Nachzug).
