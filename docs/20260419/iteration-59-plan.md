# Iteration 59 - Tenant-Anlage (CRUD-Create)

## Ziel

Die Admin-Tenants-Seite kann bisher nur listen. Iteration 59
ergaenzt die Anlage eines Mandanten. Die Keycloak-Realm-Mapping
bleibt bewusst ausserhalb dieses Use-Cases - der Admin muss den
Mandanten nach der DB-Anlage manuell im Realm verdrahten.

## Vorgehen

1. **Backend**:
   - `TenantLookupService.create(tenantKey, name, active)`: trimmt,
     prueft Dublette, persistiert. Neue
     `TenantKeyAlreadyExistsException`.
   - `TenantsController.POST /api/v1/admin/tenants` mit
     `TenantCreateRequest` (tenantKey, name, active?) und lokalen
     `@ExceptionHandler` (409 bei Dublette, 400 bei Validation).
2. **Frontend**:
   - `TenantsService.create(req)` + `TenantCreateRequest`-Type.
   - `AdminTenantsComponent`: Anlage-Formular (Toggle,
     Pflichtfelder, Aktiv-Slide-Toggle). Erfolg: SnackBar + Reload.
3. **Tests**:
   - Service: Happy-Path, Dublette, leere Felder.

## Nicht-Ziele

- Aktivierung/Deaktivierung nach Anlage: bleibt Admin-SQL oder
  Folge-Iteration.
- Set-Default: bleibt Admin-SQL.

## Jira

`CVM-109` - Tenant-Anlage (Backend + Frontend).
