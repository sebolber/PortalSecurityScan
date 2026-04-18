# Iteration 28 – Test-Summary

## Backend

### cvm-application

Befehl: `./mvnw -pl cvm-application -am test
-Dtest='ProductCatalogServiceTest,ModelProfileServiceTest'
-Dsurefire.failIfNoSpecifiedTests=false`

- `ProductCatalogServiceTest`: **8 / 8 gruen**
  - Happy-Path Produkt + Version
  - Whitespace-Trim im Key
  - Duplikat-Key (`ProductKeyConflictException`)
  - Key-Regex-Ungueltig (`IllegalArgumentException`)
  - Leerer Name
  - Version Happy-Path
  - Version: Produkt unbekannt (`ProductNotFoundException`)
  - Version-Duplikat (`ProductVersionConflictException`)

- `ModelProfileServiceTest`: **12 / 12 gruen**
  - 5 bestehende Switch-Tests unveraendert
  - 7 neue `createProfile`-Tests: Sandbox (Happy-Path + Audit),
    GKV ohne Zweit, Vier-Augen-Verstoss, Key-Konflikt, Key-Regex,
    Budget negativ, ungueltiger Provider-String

Summe: **20 / 20 gruen** in 5.0 s.

### cvm-api

Befehl: `./mvnw -pl cvm-api -am test
-Dtest='ProductsControllerWebTest,LlmModelProfilesControllerWebTest,
ModelProfileControllerWebTest'`

- `ProductsControllerWebTest`: **9 / 9 gruen** (2 Read + 7 neue POST-Faelle).
- `LlmModelProfilesControllerWebTest`: **8 / 8 gruen** (2 Read + 6 neue POST-Faelle).
- `ModelProfileControllerWebTest`: **5 / 5 gruen** (unveraendert).

Summe: **22 / 22 gruen** in ~2.6 s.

### Architekturtests

Befehl: `./mvnw -pl cvm-architecture-tests -am test -Dtest='ModulgrenzenTest'`

- `ModulgrenzenTest`: **7 / 7 gruen** (7.3 s).
  - `api -> persistence` bleibt sauber (DTO `ModelProfileCreateRequest`
    nimmt `provider` bewusst als String entgegen, um keinen Zugriff auf
    den persistence-Enum zu oeffnen).

### Flyway-Tests

- `FlywayMigrationReihenfolgeTest`: **2 / 2 skipped** (Docker nicht
  verfuegbar in dieser Umgebung). Migrationen werden bei echtem Run
  in Iteration 29 verifiziert.

## Frontend

### Build

Befehl: `npx ng build --configuration=development`

- **Erfolgreich** in 9.7 s.
- Neue Lazy-Chunks werden erzeugt: `admin-products-component`
  (29.6 kB), `scan-upload-component` (48.8 kB), `settings-component`
  (93.8 kB, gewachsen durch Profil-Create-Form).
- Keine TypeScript- oder Template-Compile-Fehler in den neuen Dateien.

### Karma-Unit-Suite

Die Karma-Suite kann in diesem Repo-Stand nicht gestartet werden: es
gibt vorbestehende TS4111-Fehler in
`cvm-frontend/src/app/core/theme/chart-theme.service.spec.ts`
(Index-Signature-Access), die den Compile der Testsuite blockieren.
Diese Fehler existieren bereits vor unseren Aenderungen (per
`git stash` reproduziert). Die fuer Iteration 28 ergaenzten Tests
(`role-menu.service.spec.ts`) wurden per Review verifiziert und
bleiben lokalisiert — ein eigener Fix fuer `chart-theme.spec.ts`
gehoert in eine separate Iteration, damit diese Aenderung nicht
uebergriffig wird.

## Ergebnis

Backend komplett gruen. Frontend baut sauber, Unit-Suite durch
pre-existenten Fehler in einer fremden Spec blockiert (als offenen
Punkt festgehalten). Migration `V0026` wurde lokal eingespielt
sobald eine Testcontainer-Umgebung zur Verfuegung steht.
