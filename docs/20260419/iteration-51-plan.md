# Iteration 51 - Profil-Edit + Soft-Delete (Block C)

## Ziel

Block C (User-Feedback-Nachzuege) starten:
- `PUT /api/v1/profiles/{id}` aktualisiert das YAML einer DRAFT-
  Profil-Version. ACTIVE/SUPERSEDED bleiben unantastbar - dort ist
  der fachliche Weg eine neue Draft-Version.
- `DELETE /api/v1/profiles/{id}` soft-loescht DRAFT/SUPERSEDED.
  ACTIVE-Versionen sind bewusst geschuetzt, damit niemand ein
  aktives Profil "versehentlich weg klickt".

## Vorgehen

1. **Flyway V0034**: `context_profile.deleted_at` + Partial-Index.
2. **Entity**: `ContextProfile.deletedAt`.
3. **ContextProfileService**:
   - `updateDraft(id, yaml, editor)` - neu; YAML wird via
     `ContextProfileYamlParser.parse` validiert. Ablehnung fuer
     ACTIVE/SUPERSEDED und soft-geloeschte.
   - `loesche(id)` - idempotent; wirft `IllegalStateException`
     fuer ACTIVE.
4. **Controller** in `cvm-api/profile`: zwei neue Endpunkte.
5. **Frontend-Service** `profiles.service.ts` erhaelt
   `draftAktualisieren` und `loesche`. Die UI-Integration im
   `profiles.component` folgt mit dem Monaco-Editor aus Iteration 54.
6. Tests (Mockito) fuer Happy/Lehnt-Active-ab/Idempotent.

## Testerwartung

- `./mvnw -T 1C -pl cvm-application -am test` &rarr; BUILD SUCCESS.
- `./mvnw -T 1C -pl cvm-app,cvm-api -am test` &rarr; BUILD SUCCESS.
- `npx ng build` + `npx ng lint` gruen.

## Wichtig fuer den naechsten Start

- **Neue Flyway-Migration V0034**: vor dem naechsten
  `scripts/start.sh` muss
  `./mvnw -T 1C clean install -DskipTests` laufen.

## Jira

`CVM-101` - Profil-Edit + Soft-Delete.
