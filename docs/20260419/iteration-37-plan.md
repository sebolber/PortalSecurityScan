# Iteration 37 – Plan

**Thema**: Queue-Detail verlinkt CVE-Detail + Produkt-Update
**Jira**: CVM-81
**Datum**: 2026-04-19

## Ziel

Zwei kleine Folge-Iterationen gebuendelt:
1. Der Queue-Detail-Slide-In verlinkt die CVE-ID auf die in
   Iteration 36 gebaute Detailseite (`/cves/:cveId`).
2. Produkt-Edit-Endpunkt `PUT /api/v1/products/{id}` +
   Frontend-Inline-Edit im Admin-Produkte-Screen (Name/Beschreibung).
   Alter Backlog-Eintrag aus Iteration 28 (Produkt-/Profil-Edit und
   Soft-Delete). Soft-Delete bleibt fuer eine spaetere Iteration.

## Scope

### Queue-Detail → CVE-Detail
- `queue-detail.component.ts` bekommt `RouterLink` und bindet die
  CVE-Key an `/cves/{cveKey}`.

### Produkt-Edit
- Backend:
  - `ProductCatalogService.update(productId, req)` (JPA-Update,
    liefert neu gelesene View).
  - `ProductsController` -> `PUT /{id}` mit Name/Beschreibung.
- Frontend:
  - `ProductsService.update(id, req)` + `update` in UI aus dem
    `AdminProductsComponent`.

## Tests

- `ProductCatalogServiceUpdateTest` (Mockito).
- `ProductsControllerUpdateTest` (Slice).
- Frontend-Build und -Lint.

## Stopp-Kriterien

- Architektur-Regeln bleiben gruen.
- Keine bestehenden Tests rot.
