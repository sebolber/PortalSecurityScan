# Iteration 34b – Plan

**Thema**: Frontend-UI `/admin/llm-configurations` (Iteration 34 Folge-Stueck)
**Jira**: CVM-78
**Datum**: 2026-04-19

## Ziel

Backend aus Iteration 34 ist live: CRUD-Endpunkte unter
`/api/v1/admin/llm-configurations` + Provider-Liste unter
`/api/v1/admin/llm-configurations/providers`. Admins sollen diese
Konfigurationen ueber eine Angular-Seite pflegen koennen.

## Scope

- Neuer Angular-Service `LlmConfigurationService`
  (`cvm-frontend/src/app/core/llm-config/llm-configuration.service.ts`)
- Neue Standalone-Komponente `AdminLlmConfigurationsComponent`
  (`cvm-frontend/src/app/features/admin-llm-configurations/`)
- Route `/admin/llm-configurations` (Role-Guard `CVM_ADMIN`).
- Menue-Eintrag unter "Einstellungen" im `RoleMenuService`.
- Karma-Spec fuer Service und Komponente.
- `ApiClient` bekommt eine `delete()`-Methode (fehlt heute).

## UX

- Liste als MatTable mit Spalten: Name, Provider, Modell, Base-URL,
  Secret (geset/hint), Active (Chip), Aktionen (Bearbeiten/Loeschen).
- Formular als Side-Card:
  - Name (Pflicht)
  - Provider (Select, geladen aus `/providers`)
  - Modell (Pflicht)
  - Base-URL (Text, mit Default-Hinweis je Provider)
  - Secret (Passwort-Feld, leer = unveraendert; Reset-Checkbox "Secret
    entfernen")
  - Max Tokens, Temperature (optional)
  - Active (Toggle) mit Hinweis "eine aktive pro Mandant"
- Snackbar-Feedback bei Erfolg/Fehler.
- Bestaetigungsdialog vor DELETE.
- Empty-State-Komponente, wenn noch keine Konfig existiert.

## Sicherheit

- Secret wird nie zurueckgespielt. Response enthaelt `secretSet` und
  `secretHint` (letzte 4 Zeichen). Formular zeigt Hint als
  read-only Anzeige.
- Route-Guard mit `CVM_ADMIN`, Menue-Sichtbarkeit gleich.

## Tests (Karma)

- `LlmConfigurationServiceTest`: list/create/update/delete/providers
  schicken korrekte Requests gegen `ApiClient`.
- `AdminLlmConfigurationsComponent`: Liste laedt, Formular sendet
  Create-Request, Error-Pfad bindet snackbar.

## Stopp-Kriterien

- `npx ng build` gruen.
- `npx ng lint` gruen.
- Kein bestehender Test rot.

## Nicht-Ziel

- Anbindung an `LlmGateway` (Iteration 34c).
- E2E-Playwright (Sandbox ohne Chromium).
