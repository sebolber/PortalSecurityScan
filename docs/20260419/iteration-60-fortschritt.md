# Iteration 60 - Fortschritt

**Thema**: Tenant Active-Toggle (CVM-110).

- `TenantLookupService.setActive(tenantId, active)` mit
  Default-Tenant-Schutz (wirft IllegalStateException bei Versuch,
  den Default zu deaktivieren).
- PATCH `/api/v1/admin/tenants/{tenantId}/active` (CVM_ADMIN).
- Frontend: Icon-Button in der Aktions-Spalte,
  `toggleActive(t)` im Component.
- Tests: 3 neue Faelle (Toggle regulaer, Default nicht
  deaktivierbar, unbekannte ID).

## Build

- `./mvnw -T 1C -pl cvm-application -am test` &rarr; 355 Tests,
  BUILD SUCCESS.
- `./mvnw -T 1C -pl cvm-app,cvm-api -am test` &rarr; 147 Tests,
  BUILD SUCCESS.
- `npx ng build` und `npx ng lint` &rarr; gruen.
