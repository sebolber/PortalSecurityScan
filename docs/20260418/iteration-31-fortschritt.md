# Iteration 31 – Fortschritt

**Thema**: `branding_config_history` + One-Click-Rollback
**Jira**: CVM-72
**Datum**: 2026-04-18

## Umgesetzt

### Persistenz

- Flyway-Migration
  [V0027__branding_config_history.sql](../../cvm-persistence/src/main/resources/db/migration/V0027__branding_config_history.sql)
  mit Tabelle `branding_config_history` (alle Felder aus
  `branding_config` plus `history_id`, `recorded_at`, `recorded_by`,
  `UNIQUE(tenant_id, version)`, absteigender Index).
- Entity
  [`BrandingConfigHistory`](../../cvm-persistence/src/main/java/com/ahs/cvm/persistence/branding/BrandingConfigHistory.java)
  und
  [`BrandingConfigHistoryRepository`](../../cvm-persistence/src/main/java/com/ahs/cvm/persistence/branding/BrandingConfigHistoryRepository.java)
  (Finder `findByTenantIdOrderByVersionDesc(Pageable)`,
  `findByTenantIdAndVersion`).

### Application

- `BrandingService` konstruktor-inkludiert jetzt
  `BrandingConfigHistoryRepository`.
- `updateForCurrentTenant` schreibt den vorherigen Stand **vor** dem
  Speichern in die History (nur wenn die alte Version &gt; 0, damit
  der erste Anlage-Call keinen leeren Snapshot produziert).
- Neue Methode `rollbackForCurrentTenant(int version, String actor)`:
  laedt den Snapshot, baut einen regulaeren `BrandingUpdateCommand`
  mit aktueller Version und ruft intern `updateForCurrentTenant`.
  Damit durchlaeuft auch ein Rollback den Kontrast-Check und wird
  selbst wieder historisiert.
- Neue Methode `history(int limit)` mit sicherem Limit (1..200).
- Neue Ausnahme `UnknownBrandingVersionException` (404-Mapping).
- Neuer View
  [`BrandingHistoryEntry`](../../cvm-application/src/main/java/com/ahs/cvm/application/branding/BrandingHistoryEntry.java)
  mit allen Feldern + Audit.

### API

- `BrandingController` erweitert um
  - `GET /api/v1/admin/theme/history?limit=N`
  - `POST /api/v1/admin/theme/rollback/{version}`
  (beide `@PreAuthorize('CVM_ADMIN')`).
- `BrandingExceptionHandler` mappt `UnknownBrandingVersionException`
  auf 404 mit Error-Code `branding_version_unknown`.

### Tests

- `BrandingServiceTest` +4:
  - History-Schreibung beim Update
  - Rollback Happy-Path
  - Rollback unbekannte Version
  - History-Liste (Pageable)
- `BrandingControllerWebTest` +3:
  - GET /admin/theme/history
  - POST /rollback (Happy)
  - POST /rollback (404)

## Verifikation

`./mvnw -T 1C test` → **BUILD SUCCESS**. Neue Module-Abhaengigkeiten
bleiben innerhalb `persistence -&gt; application -&gt; api`,
`ModulgrenzenTest` bleibt gruen. Keine Frontend-Aenderung.

## Offene Punkte

- Frontend-UI fuer `/admin/theme/history` + Rollback-Button - kommt
  mit dem Asset-Upload-UI-Ausbau (eigene Iteration).
- Persistenz-Integrationstest fuer V0027 bleibt Docker-pflichtig.

## Neue JAR

`cvm-persistence` hat eine neue Entity + Repository; fuer lokalen
Dev-Start nach `git pull` einmal
`./mvnw -T 1C clean install -DskipTests`, damit das aktualisierte
JAR im lokalen Maven-Repo landet.
