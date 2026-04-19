# Iteration 89 - Plan: Admin-Produkte Edit-Dialog (U-06a)

**Jira**: CVM-329

## Ziel

Ersetze die window.prompt-Kette in `bearbeiteProdukt` durch einen
echten `<cvm-dialog>` mit Formularfeldern fuer Name, Beschreibung
und Git-Repository-URL. Pflichtfeld-Validierung auf Name sowie
Format-Validierung auf repoUrl (`http(s)://|ssh://|git@`) muessen
im Dialog selbst passieren, damit der Admin-Flow ohne Browser-
Prompts auskommt.

## Umfang

### Component (`admin-products.component.ts`)
- Neue Signale: `editDialogOffen`, `editProdukt`, `editName`,
  `editBeschreibung`, `editRepoUrl`, `editPending`,
  `editRepoUrlFehler`.
- `bearbeiteProdukt(p)` befuellt die Signale und oeffnet den
  Dialog.
- `speichereEdit()` validiert Name (nicht leer) und repoUrl-Format,
  ruft `ProductsService.update` und schliesst den Dialog bei Erfolg.
- `brecheEditAb()` schliesst den Dialog und setzt Fehlerstate
  zurueck.
- Import der `CvmDialogComponent` im `imports`-Array.

### Template (`admin-products.component.html`)
- `<cvm-dialog>` unterhalb der vorhandenen Sections, gesteuert
  ueber `[open]="editDialogOffen()"` + `(close)="brecheEditAb()"`.
- Formularfelder: Name (required, maxlength 200), Beschreibung
  (textarea, maxlength 1000), Repo-URL (maxlength 512) mit
  Fehler-/Hilfetext.
- Footer: `Abbrechen` + `Speichern`-Buttons.

### Tests (`admin-products.component.spec.ts` - neu)
- bearbeiteProdukt befuellt Signale.
- bearbeiteProdukt normalisiert null-Felder zu Leerstring.
- speichereEdit warnt bei leerem Namen.
- speichereEdit setzt repoUrl-Fehler bei ungueltigem URL-Format.
- speichereEdit akzeptiert https://, ssh://, git@ URLs.
- speichereEdit schliesst Dialog und ruft `list` neu.
- brecheEditAb setzt Fehlerstate zurueck.

## Nicht-Umfang

- Kein neues Backend-Endpunkt-Feld.
- Keine Aenderung an `loescheProdukt` oder Versions-Dialog (bleiben
  window.confirm basiert, werden ggf. in Iteration 90 angegangen).

## Abnahme

- `ng lint` / `ng build` / Karma: gruen.
- `./mvnw -T 1C test`: BUILD SUCCESS.
