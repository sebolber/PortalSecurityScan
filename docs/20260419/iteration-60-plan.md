# Iteration 60 - Tenant Active-Toggle

## Ziel

Admin kann einen Mandanten aktivieren/deaktivieren. Der Default-
Mandant ist bewusst ausgeschlossen, damit JWTs ohne tenant_key einen
Fallback behalten.

## Vorgehen

- Backend: `TenantLookupService.setActive(id, active)` mit
  Schutz des Default-Tenants. PATCH
  `/api/v1/admin/tenants/{id}/active`.
- Frontend: Icon-Button (toggle_on/toggle_off) in der Aktions-
  Spalte; disabled fuer den Default-Mandanten.
- Tests: Happy, Default-Schutz, unbekannte ID.

## Jira

`CVM-110` - Tenant Active-Toggle.
