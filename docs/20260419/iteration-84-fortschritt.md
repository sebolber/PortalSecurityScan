# Iteration 84 - Fortschritt: Tenant-Badge-Popover (U-02c)

**Jira**: CVM-324

## Was wurde gebaut

### Shell-Component
Neue Signale:
- `tenantMenuOpen` / `tenantMenuLaedt`
- `alleTenants` (readonly TenantView[])
- `istAdmin` computed ueber `auth.userRoles()`

Neue Methoden:
- `toggleTenantMenu()`: schliesst das User-Menue, oeffnet das
  Tenant-Popover; lazy-laedt die Tenant-Liste fuer Admins.
- `ladeAlleTenants()`: `TenantsService.list()` mit Loading-Flag.
- `setzeAlsDefault(tenantId)`: `TenantsService.setDefault()` +
  `ladeAlleTenants()` + `ladeTenant()` (Badge aktualisiert).
- `closeMenus()`: schliesst beide Popover.

### Template
- Tenant-Badge ist jetzt ein Button mit `chevron-down/up`.
- Popover rechts unter dem Badge (w-360):
  - Hinweis-Zeile "Mandantenwechsel = Logout + Re-Login (Keycloak)"
    fuer alle Rollen.
  - Admin-Liste: alle Mandanten mit "Als Default"-Button (falls
    nicht default + aktiv). Inaktive Tenants zeigen "inaktiv".
- Default-Marker und Active/Inactive-Unterscheidung sichtbar.

### Tests

`shell.component.spec.ts` (neu). Da der Shell zahlreiche
`providedIn:'root'`-Services mit HttpClient-Abhaengigkeit zieht,
die im Karma-Setup nicht vollstaendig aufloesbar sind, werden
die drei Popover-Methoden als Unit-Test direkt auf einer
Prototype-Instanz getestet:

- `toggleTenantMenu` oeffnet und schliesst.
- Admin-Rolle: `toggleTenantMenu` ruft `tenants.list`.
- `setzeAlsDefault` delegiert an `TenantsService.setDefault`.

## Ergebnisse

- `ng lint` OK, `ng build` OK.
- Karma 118 Tests SUCCESS (3 neu).
- `./mvnw -T 1C test` BUILD SUCCESS in 01:28 min.

## Offene Folge-Punkte

- Integrations-Test mit vollstaendigem DOM-Rendering des Shells
  (erfordert Refactoring der Service-Tree-Aufloesung, kein
  Ziel dieser Iteration).
- Wechsel via Keycloak-Claim ist Infrastruktur-Aufgabe
  (bleibt in offene-punkte als Non-Code).
