# Iteration 59 - Fortschritt

**Thema**: Tenant-Anlage (CVM-109).

## Was gebaut wurde

- Backend:
  - `TenantLookupService.create(tenantKey, name, active)` mit
    Trim, Dublettencheck, `TenantKeyAlreadyExistsException`.
  - `TenantsController.POST /api/v1/admin/tenants` (Admin).
    Lokale `@ExceptionHandler`: 409 bei Dublette, 400 bei
    IllegalArgumentException.
- Frontend:
  - `TenantsService.create(req)` und `TenantCreateRequest`-Type.
  - `AdminTenantsComponent`: Anlage-Formular mit Toggle
    (Pflichtfelder Key+Name + Active-Switch), SnackBar-Feedback,
    automatisches Neuladen der Liste.

## Neue Tests

- `TenantLookupServiceTest` +3 Faelle (createHappyPath,
  createDoppelterKey, createLeereFelder).

## Build

- `./mvnw -T 1C -pl cvm-application -am test` &rarr; 352 Tests,
  BUILD SUCCESS.
- `./mvnw -T 1C -pl cvm-app,cvm-api -am test` &rarr; 147 Tests,
  BUILD SUCCESS.
- `npx ng build` &rarr; ok.
- `npx ng lint` &rarr; All files pass linting.

## Nicht-Ziele

Aktivierung/Deaktivierung nach Anlage und Setzen des Default-
Mandanten bleiben Admin-SQL oder einer Folge-Iteration
vorbehalten. Die Keycloak-Realm-Verdrahtung ist ebenfalls weiter
separat.

## Vier Leitfragen (Oberflaeche)

1. *Weiss ein Admin, was zu tun ist?* Ja - Button "Neuer
   Mandant" klappt das Formular auf, Pflichtfelder sind markiert.
2. *Ist erkennbar, ob eine Aktion erfolgreich war?* SnackBar,
   Liste laedt neu; bei 409 erscheint eine Banner-Fehlermeldung.
3. *Sind Daten sichtbar, die im Backend existieren?* Ja.
4. *Gibt es einen Weg zurueck/weiter?* "Formular schliessen"
   setzt die Eingaben zurueck.
