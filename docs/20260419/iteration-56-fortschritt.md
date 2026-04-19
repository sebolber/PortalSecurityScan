# Iteration 56 - Fortschritt

**Thema**: Tenant-Liste (read-only) in der Admin-UI (CVM-106).

## Was gebaut wurde

- Backend:
  - `TenantView`-Record in `cvm-application/tenant`.
  - `TenantLookupService.listAll()` sortiert Default-Tenant zuerst,
    dann Key alphabetisch (case-insensitive).
  - `TenantsController GET /api/v1/admin/tenants` mit
    `@PreAuthorize('CVM_ADMIN')`.
- Frontend:
  - `TenantsService.list()`.
  - Neue Standalone-Komponente `AdminTenantsComponent` mit
    Material-Table (Key, Name, Aktiv/Default-Chips, Angelegt-Zeit).
  - Route `/admin/tenants` als `loadComponent` (ADMIN-only).
  - Neuer Menueintrag "Mandanten" (Icon `group`) in der
    Einstellungen-Sektion.

## Neue Tests

- `TenantLookupServiceTest` (2 Tests: Sortierung,
  Leere-Liste).

## Scope-Hinweis

Anlage, Aktivierung und Keycloak-Mapping bleiben Admin-SQL bzw.
Realm-Setup bis zur Folge-Iteration. Damit kommt die UI ohne
Vier-Augen-Flow aus; das Risiko, versehentlich Mandanten zu aendern,
ist null.

## Build

- `./mvnw -T 1C -pl cvm-application -am test` &rarr; 349 Tests,
  BUILD SUCCESS.
- `./mvnw -T 1C -pl cvm-app,cvm-api -am test` &rarr; 147 Tests,
  BUILD SUCCESS.
- `npx ng build` &rarr; ok.
- `npx ng lint` &rarr; All files pass linting.

## Vier Leitfragen (Oberflaeche)

1. *Weiss ein Admin, was zu tun ist?* Der Intro-Text nennt explizit,
   dass Anlage/Aktivierung per Admin-SQL/Keycloak laeuft; die Seite
   ist bewusst read-only.
2. *Ist erkennbar, ob eine Aktion erfolgreich war?* Einziger Action-
   Button ist "Neu laden"; der Spinner zeigt den Ladevorgang.
3. *Sind Daten sichtbar, die im Backend existieren?* Ja - `findAll()`
   wird gereiht nach Default/Key.
4. *Gibt es einen Weg zurueck/weiter?* Standardnavigation via
   Shell-Menue.
