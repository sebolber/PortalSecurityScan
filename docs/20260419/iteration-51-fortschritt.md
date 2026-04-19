# Iteration 51 - Fortschritt

**Thema**: Profil-Edit + Profil-Soft-Delete (CVM-101).

## Was gebaut wurde

- Flyway V0034 `V0034__context_profile_soft_delete.sql` mit
  ausfuehrlichem Kommentar zur Abgrenzung
  "Edit eines DRAFT" vs. "Soft-Delete".
- `ContextProfile.deletedAt`.
- `ContextProfileService.updateDraft(id, yaml, editor)` - YAML-Edit
  fuer DRAFT-Versionen; parst das YAML vorab und lehnt
  ACTIVE/SUPERSEDED sowie soft-geloeschte ab.
- `ContextProfileService.loesche(id)` - idempotent; wirft
  `IllegalStateException`, wenn die Version `ACTIVE` ist.
- REST-Endpunkte `PUT /api/v1/profiles/{id}` und
  `DELETE /api/v1/profiles/{id}` im `ProfileController`.
- Frontend-Service `profiles.service.ts` um `draftAktualisieren`
  und `loesche` ergaenzt. Die UI-Integration im
  `profiles.component` folgt mit dem Monaco-Editor aus Iteration 54.

## Neue Tests

- `ContextProfileServiceTest` +4 Faelle (updateDraft-Happy,
  updateDraft-ACTIVE-lehnt-ab, loesche-Happy, loesche-ACTIVE-lehnt-ab,
  loesche-Idempotent).

## Build

- `./mvnw -T 1C -pl cvm-application -am test` &rarr; alle Tests
  gruen (10 Tests im ContextProfileServiceTest).
- `./mvnw -T 1C -pl cvm-app,cvm-api -am test` &rarr; 147 Tests, BUILD
  SUCCESS.
- `npx ng build` &rarr; ok.
- `npx ng lint` &rarr; All files pass linting.

## Wichtig fuer den naechsten Start

- **Neue Flyway-Migration V0034**: vor dem naechsten
  `scripts/start.sh` muss
  `./mvnw -T 1C clean install -DskipTests` laufen.

## Vier Leitfragen (Oberflaeche)

Diese Iteration liefert reine Backend + Frontend-Service-API. Die
sichtbare UI-Integration bleibt bewusst Iteration 54 vorbehalten
(Monaco-Editor). Damit bleibt die bestehende Profil-Seite unveraendert,
die neuen Methoden sind aber fuer den Editor bereits fertig.
