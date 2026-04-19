# Iteration 74 - Fortschritt: Persistente DRAFTs pro Umgebung

**Jira**: CVM-311
**Datum**: 2026-04-19

## Was wurde gebaut

### Backend

- `ContextProfileService.latestDraftFor(UUID environmentId)`:
  `Optional<ProfileView>` mit der DRAFT-Version der hoechsten
  Versionsnummer.
- `ProfileController#aktuellerDraft(...)`:
  `GET /api/v1/environments/{id}/profile/draft` liefert den DRAFT
  oder HTTP 404.

### Frontend

- `ProfilesService.aktuellerDraft(envId)` ueber
  `ApiClient.getOptional`: 404 -> `null` ohne Error-Toast.
- `ProfilesComponent.laden()` laedt pro Umgebung sowohl das
  aktive Profil als auch den persistenten DRAFT. Bei Treffer
  wird zusaetzlich der Diff gegen die aktive Version gezogen,
  sodass die UI unmittelbar den DRAFT-Panel mit dem
  "wartet-auf-Freigabe"-Hinweis zeigt.
- Bestehende Component-Spec `profiles.component.spec.ts` um den
  neuen Service-Mock (`aktuellerDraft.and.returnValue(null)`)
  ergaenzt, damit die Bestands-Tests nicht brechen.

### Tests (TDD)

- Backend: zwei neue Cases in `ProfileControllerWebTest`
  (`aktuellerDraftLiefert200`, `aktuellerDraftLiefert404`).
- Frontend: zwei neue Cases in `profiles.service.spec.ts`
  (`aktuellerDraft: 200` und `aktuellerDraft: 404 -> null`).

## Ergebnisse

- `./mvnw -T 1C test` -> **BUILD SUCCESS** in 02:34 min.
- `ProfileControllerWebTest` 10 Tests PASS.
- Karma (profiles-scope) 11 Tests PASS.
- `npx ng lint` gruen, `npx ng build` erfolgreich.

## Migrations / Deployment

- Keine Flyway-Migration, keine neuen Dependencies.
