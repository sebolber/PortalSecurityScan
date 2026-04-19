# Iteration 37 – Fortschritt

**Thema**: Queue-Detail verlinkt CVE-Detail + Produkt-Update
**Jira**: CVM-81
**Datum**: 2026-04-19

## Umgesetzt

### Queue -> CVE-Detail

- `queue-detail.component.ts`: CVE-Id wird jetzt als Anchor mit
  `[routerLink]="['/cves', entry.cveKey]"` gerendert; Cursor und
  Hover-Underline lenken darauf hin.

### Produkt-Update

- Backend:
  - `ProductCatalogService.aktualisiere(id, ProductUpdateInput)` -
    laesst Key unveraendert, aktualisiert Name/Beschreibung, leere
    Beschreibung wird auf null normalisiert.
  - Neuer Endpunkt `PUT /api/v1/products/{id}` mit
    `ProductUpdateRequest`. CVM_ADMIN-Role, 404 bei unbekannter Id,
    400 bei leerem Name.
  - Fuenf neue Testfaelle in `ProductCatalogServiceTest`.
- Frontend:
  - `ProductsService.update(productId, req)`.
  - `AdminProductsComponent#bearbeiteProdukt(p)` mit zwei
    `window.prompt`-Dialogen (Name + Beschreibung) und Snackbar-
    Feedback.
  - Admin-Produkte-Tabelle: neuer Edit-IconButton neben dem
    Versionen-Button.

## Test-Status

- `./mvnw -T 1C test`: BUILD SUCCESS.
- `npx ng build`: gruen.
- `npx ng lint`: gruen.

## Offene Punkte (zur spaeteren Iteration)

- Produkt-Soft-Delete.
- Modal-Dialog statt window.prompt fuer den Edit-Flow
  (wenn UI-Feedback entsprechend gefordert).
- Profil-Edit-/Soft-Delete (getrennter Scope).
