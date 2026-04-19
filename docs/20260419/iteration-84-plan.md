# Iteration 84 - Plan: Tenant-Badge interaktiv + Default-Switch (U-02c)

**Jira**: CVM-324

## Ziel

Das bestehende Tenant-Badge in der Topbar zeigt heute nur einen
statischen Namen. Diese Iteration macht daraus ein Menue:

- Klick auf das Badge oeffnet ein Popover.
- Popover zeigt den aktuellen Mandanten + Hinweistext
  "Mandantenwechsel = Logout + Re-Login via Keycloak".
- Fuer Admin-Nutzer listet das Popover alle Mandanten mit
  Set-Default-Button.
- Nach Set-Default-Klick wird das Badge aktualisiert.

## Umfang

### Shell-Component
- Neues Signal `tenantMenuOpen`.
- Neues Signal `alleTenants` (lazy geladen beim Open).
- Toggle-Funktion `toggleTenantMenu()`.
- `setzeAlsDefault(tenantId)` ruft `TenantsService.setDefault`
  und laedt Tenants neu.
- Close-on-Outside via bestehendes Pattern (wie
  `closeMenus()`).

### Template
- Badge wird Button.
- Popover rechts unter dem Badge mit:
  - Liste der Mandanten (nur ADMIN).
  - Default-Marker + "Als Default setzen"-Button pro Zeile.
  - Info-Box fuer alle Rollen: Wie Mandant wechseln.

## TDD

- `shell.component.spec.ts` (neu): Popover oeffnet bei Klick,
  Admin sieht Liste, Nicht-Admin sieht nur Info-Box,
  Set-Default-Button ruft TenantsService.

## Abnahme

- `ng lint` / `ng build` / Karma gruen.
