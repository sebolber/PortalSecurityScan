# Iteration 28b - Profil-YAML-Editor in der UI

**Jira**: CVM-66
**Branch**: `claude/ui-theming-updates-ruCID`
**Stand**: 2026-04-18

Erster Teil der "Onboarding & Konfiguration ueber die UI"-
Initiative. Erlaubt Profile-Autoren und Approvern, das
Kontextprofil pro Umgebung vollstaendig aus dem Frontend
heraus zu bearbeiten.

## Umgesetzt

### Service-Erweiterung

- `ProfilesService` bekommt drei neue Methoden
  (`core/profiles/profiles.service.ts`):
  - `draftAnlegen(environmentId, yamlSource, proposedBy)`
    -> `PUT /api/v1/environments/{id}/profile`.
  - `freigeben(profileVersionId, approverId)`
    -> `POST /api/v1/profiles/{id}/approve`.
  - `diffGegenAktiv(profileVersionId)`
    -> `GET /api/v1/profiles/{id}/diff?against=latest`.
- Neuer DTO `ProfileDiffEntry`.

### ProfilesComponent

Komplett neu gebaut (`features/profiles/profiles.component.*`):

- Pro Umgebung Expansion-Panel mit aktivem Profil + YAML-Quelle.
- Button "Neuen Draft anlegen" oeffnet Textarea mit Mono-Font.
  Inhalt vorbelegt mit der aktiven YAML-Quelle oder einem
  Default-Skelett (schemaVersion 1, umgebung/architecture/
  network/hardening/compliance.frameworks).
- "Draft speichern" ruft `draftAnlegen` auf; bei Schema-Fehler
  liefert das Backend HTTP 400, die Fehlermeldung wird im
  Panel angezeigt.
- Nach erfolgreichem Draft wird automatisch der Diff gegen die
  aktive Version geladen und als Tabelle (Pfad / Alt / Neu /
  Aenderungs-Kind) gerendert.
- "Draft aktivieren (Vier-Augen)" ruft `freigeben` auf. Bei
  Autor == Approver liefert das Backend HTTP 409, die Meldung
  landet im Fehler-Banner.
- Rollengate:
  - `CVM_PROFILE_AUTHOR` oder `ADMIN` aktiviert den Editor.
  - `CVM_PROFILE_APPROVER` oder `ADMIN` aktiviert den Approve-
    Button. Andernfalls bleibt der Button disabled + Hinweis.
- Erfolg/Fehler-Banner nutzen `ahs-banner` (Severity-Tokens).
- Alle Styles auf Tokens (`--space-*`, `--color-*`,
  `--font-family-mono`).

### Rolle-Menu

Unveraendert: `/profiles` ist bereits fuer `PROFILE_AUTHOR` /
`PROFILE_APPROVER` / `ADMIN` freigeschaltet.

## Build / Verifikation

- `npx ng lint cvm-frontend` -> All files pass linting.
- `npx ng build cvm-frontend` -> Application bundle generation
  complete (2.06 MB initial, unveraendert).
- Backend unveraendert; die bestehenden Profile-Tests laufen
  weiter gruen (siehe letzter `./mvnw test`-Durchlauf in 27e).

## Hinweis zu Monaco

`ngx-monaco-editor-v2` ist in `package.json` vorhanden, aber
`angular.json` kopiert die Worker-Assets (Monaco-Loader, VS-
Worker) nicht. Das produktive Einschalten erfordert eine
Asset-Glob-Erweiterung in `angular.json` und einen
`MonacoEditorModule`-Root-Import. Dieser Schritt bleibt fuer
Iteration 28b+ als Polish offen; die Textarea mit Mono-Font
deckt die Funktion (Schreiben, Validieren ueber Backend,
Diffen, Aktivieren) vollstaendig ab.

## Offene Punkte fuer 28d / 28f

- 28d: SBOM-Upload-UI mit Drag&Drop
- 28f: Theme-Asset-Upload-UI (Logo / Favicon / Font)

Folge-Iterationen (28a/28c/28e) benoetigen zusaetzlich
CRUD-Endpunkte im Backend (Produkte, Regeln, Tenants/
Environments).
