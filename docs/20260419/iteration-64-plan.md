# Iteration 64 - Plan: Profil-Edit / Soft-Delete UI-Integration

**Jira**: CVM-301

## Ziel

Die Admin-/Autor-UI fuer Kontextprofile bietet bislang nur
"Neuen Draft anlegen" + "Aktivieren". Backend-Endpunkte
`PUT /api/v1/profiles/{id}` (Draft-Edit) und
`DELETE /api/v1/profiles/{id}` (Soft-Delete) existieren seit
Iteration 51, aber ohne UI-Anbindung. Diese Iteration schliesst
die Luecke im Pattern aus `AdminProductsComponent`
(`window.confirm` + `CvmToastService`).

## Umfang

- Pro Row (pro Umgebung) erscheinen im DRAFT-Panel zwei neue
  Aktion-Buttons:
  - **Draft bearbeiten** (oeffnet Monaco-Editor, vorbefuellt
    mit dem Draft-YAML). Folge-Save ruft
    `ProfilesService.draftAktualisieren` (PUT).
  - **Draft loeschen** (`window.confirm`, dann DELETE, Toast bei
    Erfolg). Row wird zurueckgesetzt.
- "Neuen Draft anlegen" bleibt unveraendert (legt eine neue
  Version via POST `draftAnlegen` an).
- `draftSpeichern` unterscheidet anhand eines neuen Flags
  `bearbeitetDraft` zwischen "neue Version" und "Draft update".

## TDD-Reihenfolge

1. Karma-Spec `profiles.service.spec.ts` um Tests fuer
   `draftAktualisieren` (PUT) und `loesche` (DELETE) erweitern
   (URL, Methode, Request-Body).
2. Karma-Spec `profiles.component.spec.ts` (neu) fuer die drei
   UI-Flows:
   - "Draft bearbeiten" oeffnet den Editor mit Draft-YAML und
     ruft bei Save `draftAktualisieren` auf.
   - "Draft loeschen" bricht ab bei `window.confirm` false.
   - "Draft loeschen" fuehrt DELETE + Reset + Toast aus.
3. Komponente + Template anpassen.

## Keine Backend-Aenderungen

PUT/DELETE-Endpunkte und `ProfilesService.draftAktualisieren` /
`.loesche` existieren schon (Iteration 51). Diese Iteration ist
reines UI-Add.

## Abnahmekriterien

- `npx ng lint` gruen.
- `npx ng build` gruen, Bundle-Budget weiterhin eingehalten.
- Karma gruen (alter + neuer Spec).
- `./mvnw -T 1C test` bleibt gruen.
