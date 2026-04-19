# Iteration 48 - Fortschritt

**Thema**: Umgebungen Soft-Delete (CVM-98).

## Was gebaut wurde

- Flyway-Migration V0031
  (`V0031__environment_soft_delete.sql`) mit `deleted_at` +
  Partial-Index.
- `Environment.deletedAt`, neue Repository-Methode
  `findByDeletedAtIsNullOrderByKeyAsc`.
- `EnvironmentQueryService`:
  - `listAll()` filtert jetzt auf aktive Umgebungen.
  - `loesche(UUID)` ist idempotent und wirft `EntityNotFoundException`
    fuer unbekannte IDs sowie `IllegalArgumentException` fuer
    `null`.
- `EnvironmentsController.DELETE /{environmentId}` mit
  `@PreAuthorize('CVM_ADMIN')` und 204.
- `EnvironmentsExceptionHandler` fuer `EntityNotFoundException`
  (404).
- Frontend: neue `EnvironmentsService.delete(id)`, neue Aktions-
  Spalte mit Icon-Button (Material delete) und `window.confirm`-
  Dialog, SnackBar-Feedback.

## Neue Tests

- `EnvironmentQueryServiceTest` um 5 Faelle erweitert (listAll-
  Filter, loesche Happy-Path, Idempotenz, null, not-found).

## Build

- `./mvnw -T 1C -pl cvm-application -am test` &rarr; 330 Tests, BUILD
  SUCCESS.
- `./mvnw -T 1C -pl cvm-app,cvm-api -am test` &rarr; 147 Web-Tests
  gruen.
- `npx ng build` + `npx ng lint` &rarr; ok.

## Wichtig fuer den naechsten Start

- **Neue Flyway-Migration V0031**: vor dem naechsten
  `scripts/start.sh` muss
  `./mvnw -T 1C clean install -DskipTests` durchlaufen, damit die
  Migration im Image landet.

## Vier Leitfragen (Oberflaeche)

1. *Weiss ein Admin, was zu tun ist?* Ja - Icon-Button
   (Papierkorb rot) in der Aktions-Spalte, Tooltip erklaert den
   Soft-Delete.
2. *Ist erkennbar, ob eine Aktion erfolgreich war?* Ja - SnackBar
   meldet "Umgebung xyz entfernt" und die Liste wird neu geladen.
3. *Sind Daten sichtbar, die im Backend existieren?* Ja - aktive
   Umgebungen bleiben; geloeschte verschwinden aus der Liste
   (bleiben aber in der DB).
4. *Gibt es einen Weg zurueck/weiter?* Eine Resurrection-UI gibt es
   bewusst nicht; die Wiederherstellung erfolgt bei Bedarf ueber
   Admin-SQL (dokumentierte, seltene Admin-Operation).
