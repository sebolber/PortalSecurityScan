# Iteration 38 – Fortschritt

**Thema**: Produkt-Soft-Delete
**Jira**: CVM-82
**Datum**: 2026-04-19

## Umgesetzt

### Backend
- Flyway **V0029** `product_soft_delete.sql`: Spalte
  `product.deleted_at TIMESTAMPTZ NULL` + Partial-Index.
- Entity `Product` um `deletedAt`-Feld (mit Getter/Setter) erweitert.
- `ProductRepository.findByDeletedAtIsNullOrderByKeyAsc()` fuer
  das Listing.
- `ProductQueryService.listProducts()` nutzt die gefilterte Query;
  tote Eintraege verschwinden aus der Admin-Liste.
- `ProductCatalogService.loesche(id)` Soft-Delete (idempotent,
  404 fuer unbekannte Id). Drei neue Tests in
  `ProductCatalogServiceTest`.
- Neuer Endpoint `DELETE /api/v1/products/{id}` (CVM_ADMIN).

### Frontend
- `ProductsService.delete(productId)`.
- `AdminProductsComponent.loescheProdukt(p)` mit
  `window.confirm`-Bestaetigung und Snackbar-Feedback.
- Tabellenzeile bekommt einen `delete`-IconButton neben dem
  Edit-Button.

## Hinweis

- Neue Flyway-Migration: vor dem naechsten `scripts/start.sh`
  einmal `./mvnw -T 1C clean install -DskipTests` laufen lassen,
  damit die aktualisierte `cvm-persistence.jar` beim Start
  Flyway anzieht.

## Test-Status

- `./mvnw -T 1C test`: BUILD SUCCESS.
- `npx ng build` + `npx ng lint`: gruen.
