# Iteration 63 - Plan: Profil-Save-404-Bug fixen

**Jira**: CVM-300

## Ziel

Beim ersten Draft-Save auf einer Umgebung ohne aktive Vorgaenger-
version darf kein irrefuehrender 404 mehr erscheinen. Der Diff-
Endpunkt liefert fachlich "nichts zum Vergleichen" als HTTP 200 mit
leerer Liste; das Frontend nutzt `getOptional`, sodass der globale
`HttpErrorHandler` nicht mehr greift.

## TDD-Reihenfolge

1. **Backend-Test** (rot):
   `ProfileControllerWebTest#diffLiefertLeereListeOhneAktiv` -
   `GET /profiles/{id}/diff?against=latest` liefert 200 + [] wenn
   `environmentOf` einen Treffer hat, `latestActiveFor` aber leer
   ist.
2. **Backend-Test** (rot):
   `ProfileControllerWebTest#diffLiefert404BeiUnbekannterProfilId`
   bleibt/wird 404 wenn `environmentOf` leer ist (Profil-ID selbst
   nicht existent).
3. **Backend-Fix** (`ProfileController.java`):
   `aufloeseGegenparameter` entfallen, `diff`-Methode refaktoriert,
   die Fehlerpfade sind sauber getrennt: Profil-ID unbekannt → 404,
   Keine Vorgaenger-Version → 200 + [].
4. **Frontend-Test** (rot):
   Neuer Spec `profiles.service.spec.ts` mit zwei Faellen:
   - 200 mit Liste -> Promise loest mit Liste auf.
   - 404 -> Promise loest mit `[]` auf (KEIN throw).
5. **Frontend-Fix** (`profiles.service.ts`):
   `diffGegenAktiv` nutzt `ApiClient.getOptional<ProfileDiffEntry[]>`
   und liefert `[]` statt `null` bei 404.
6. **Editor-Reset in Komponente**:
   Nach erfolgreichem `draftSpeichern` expliziter
   `fehler: null`-Reset, damit kein Rest-Fehlerzustand die Buttons
   blockiert.

## Tests

- `cvm-api`: `./mvnw -pl cvm-api test`
- `cvm-frontend`: Karma-Spec fuer `ProfilesService`
- Gesamtlauf: `./mvnw -T 1C test` + `npx ng lint` + `npx ng build`

## Risiken

- Keine. Der bisherige fehlerhafte 404 hatte keinen legitimen
  Aufrufer, da der Draft in Wahrheit erfolgreich gespeichert wurde.
