# Iteration 56 - Tenant-Liste (read-only)

## Ziel

Die Admin-UI zeigt bisher keine Mandanten. Iteration 56 ergaenzt eine
read-only Liste pro Default-/Key/Name/Active/Angelegt-Spalten;
Anlage/Aktivierung bleibt Admin-SQL bzw. Keycloak-Realm-Setup bis zu
einer Folge-Iteration.

## Vorgehen

1. **Backend**:
   - `TenantView`-Record in `cvm-application/tenant`.
   - `TenantLookupService.listAll()` sortiert Default-Tenant zuerst,
     dann alphabetisch.
   - `TenantsController GET /api/v1/admin/tenants` mit
     `@PreAuthorize('CVM_ADMIN')`.
2. **Frontend**:
   - `TenantsService.list()`.
   - Standalone `AdminTenantsComponent` mit Material-Table.
   - Route `/admin/tenants` (loadComponent, ADMIN-only).
   - Menueintrag "Mandanten" in der Einstellungen-Sektion.
3. **Tests**: `TenantLookupServiceTest` (Sortierung, leere Liste).

## Jira

`CVM-106` - Tenant-Liste (read-only).
