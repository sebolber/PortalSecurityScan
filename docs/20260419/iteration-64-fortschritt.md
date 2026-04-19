# Iteration 64 - Fortschritt: Profil-Edit / Soft-Delete UI

**Jira**: CVM-301
**Datum**: 2026-04-19

## Was wurde gebaut

### Frontend - Komponente

`cvm-frontend/src/app/features/profiles/profiles.component.ts`:

- Neue Row-Felder: `bearbeitetDraft: boolean` (Dispatch-Flag fuer
  `draftSpeichern`) und `deleting: boolean` (Button-State).
- Neue Methode `draftBearbeiten(row)`: oeffnet den Monaco-Editor
  vorbefuellt mit `row.draft.yamlSource` und setzt den Dispatch-
  Flag auf "Update".
- Neue Methode `draftLoeschen(row)`: zeigt `window.confirm` mit
  Umgebung + Versionsnummer, ruft `ProfilesService.loesche`, setzt
  die Row zurueck und feuert einen Erfolg-Toast.
- `draftSpeichern` dispatched jetzt: `draftAktualisieren` wenn
  `bearbeitetDraft && row.draft` gesetzt sind, sonst
  `draftAnlegen` (Verhalten wie bisher). Meldung/Toast
  unterscheiden zwischen "angelegt" und "aktualisiert".
- `editorUmschalten` setzt beim Oeffnen `bearbeitetDraft=false`
  und `editorYaml` auf das aktive YAML/DEFAULT zurueck, damit der
  "Neuen Draft anlegen"-Flow konsistent bleibt.

### Frontend - Template

`cvm-frontend/src/app/features/profiles/profiles.component.html`:

- In der DRAFT-Section zwei neue Aktion-Buttons hinter dem
  Aktivieren-Button:
  - "Draft bearbeiten" (Secondary-Button, `data-testid="draft-bearbeiten"`).
  - "Draft loeschen" (Danger-Button, `data-testid="draft-loeschen"`),
    Loading-Spinner waehrend `deleting=true`.

### Tests (TDD)

- `profiles.service.spec.ts` erweitert:
  - `draftAktualisieren: PUT /api/v1/profiles/{id} mit YAML und Autor`.
  - `loesche: DELETE /api/v1/profiles/{id}`.
- `profiles.component.spec.ts` (neu, 4 Tests):
  - `draftBearbeiten` oeffnet Editor im Update-Modus; Folge-Save
    ruft `draftAktualisieren`, nicht `draftAnlegen`.
  - `draftLoeschen` bricht bei `confirm=false` ab.
  - `draftLoeschen` DELETEt, setzt Row zurueck, zeigt Toast.
  - `editorUmschalten` setzt nach Schliessen+Neu-Oeffnen wieder
    `bearbeitetDraft=false`.

## Ergebnisse

- `npx ng lint` -> "All files pass linting."
- `npx ng build` -> Bundle erfolgreich.
- Karma (9 Tests in profiles-Scope): alle gruen.
- `./mvnw -T 1C -pl cvm-api -am test` -> 150 Tests gruen.

## Offen / bewusst verschoben

- UI fuer **bereits existierende** DRAFTs einer Umgebung, die ueber
  einen Sessionwechsel persistieren, ist weiterhin nicht verfuegbar.
  Der Service hat keinen Endpunkt zum Auflisten der DRAFTs pro
  Umgebung. Als Folgepunkt in `offene-punkte.md`.

## Migrations / Deployment

- Keine Flyway-Migration.
- Keine neuen Dependencies.
