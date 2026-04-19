# Iteration 63 - Fortschritt: Profil-Save-404-Bug fixen

**Jira**: CVM-300
**Datum**: 2026-04-19

## Was wurde gebaut

### Backend

- `ProfileController.diff` umgebaut
  (`cvm-api/src/main/java/com/ahs/cvm/api/profile/ProfileController.java`):
  - Die Profil-ID wird zuerst gegen `ContextProfileService.environmentOf`
    geprueft. Ist sie unbekannt, bleibt das Verhalten wie gehabt
    (HTTP 404 + `profile_not_found`).
  - Ist die Profil-ID bekannt und `against=latest`, aber es existiert
    keine aktive Vorgaenger-Version, liefert der Endpunkt jetzt
    HTTP 200 mit einer **leeren Liste** (fachlich "nichts zu
    vergleichen") statt einer ProfileNotFoundException.
  - Die Aufloesung des `against`-Parameters ist inline gezogen; die
    Hilfsmethode `aufloeseGegenparameter` ist entfallen, weil der
    Kontrollfluss unterschiedliche HTTP-Codes (404 vs 200) je Pfad
    erzeugt.
- Operation-Beschreibung erweitert, damit Swagger-Nutzer das neue
  Verhalten sehen.

### Frontend

- `ProfilesService.diffGegenAktiv`
  (`cvm-frontend/src/app/core/profiles/profiles.service.ts`)
  ruft ueber `ApiClient.getOptional<ProfileDiffEntry[]>` und liefert
  `[]` bei 404, statt zu werfen. Der globale `HttpErrorHandler` wird
  nicht mehr durchlaufen, die rote Snackbar verschwindet.
- `ProfilesComponent.draftSpeichern` setzt nach erfolgreichem Save
  `fehler: null` explizit (zusaetzlich zum bisherigen Reset bei
  Save-Start), damit kein Rest-Fehlerzustand die Buttons sperrt.

### Tests (TDD)

Backend (`ProfileControllerWebTest.java`):

- `diffLiefertLeereListeOhneAktiv` - 200 + [] bei fehlender
  Vorgaenger-Version.
- `diffLiefert404BeiUnbekannterProfilId` - 404 + `profile_not_found`
  bei unbekannter Profil-ID.
- `diffLiefertEintraegeGegenAktiv` - Happy-Path bleibt 200.

Frontend (`profiles.service.spec.ts`, neu):

- 200 mit Liste -> Promise loest mit Liste auf, kein `errorHandler.show`.
- 404 -> Promise loest mit `[]` auf, kein `errorHandler.show`.
- 500 -> Promise verworfen **und** `errorHandler.show` aufgerufen.

## Was nicht umgesetzt / bewusst weggelassen

- Kein weiteres Refactoring der Komponente. Die vier Leitfragen aus
  CLAUDE.md §10 sind nach dem Fix erneut erfuellt (Save meldet jetzt
  eindeutig Erfolg, keine falsche Fehlermeldung, Editor bleibt
  bedienbar).

## Build / Testergebnisse

- `./mvnw -T 1C test` -> **BUILD SUCCESS** (alle Module).
- `ProfileControllerWebTest` -> 8 Tests, 0 Failures.
- Karma (`profiles.service.spec.ts`) -> 3 Tests, 0 Failures.
- `npx ng lint` -> "All files pass linting."
- `npx ng build` -> Bundle erzeugt, Bundle-Budget weiterhin im
  gruenen Bereich (initial 1.10 MB).

## Migrations / Deployment

- Keine neue Flyway-Migration.
- Keine neuen Module/Dependencies.
- Kein Neubau der Images noetig.
