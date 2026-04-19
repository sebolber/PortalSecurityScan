# Iteration 49 - Produkt-Versionen Soft-Delete

## Ziel

Produkt-Versionen koennen analog zu Produkten (Iteration 38) und
Umgebungen (Iteration 48) logisch entfernt werden. Bestehende Scans,
Findings und Assessments bleiben unveraendert, die Version
verschwindet nur aus Admin-/Auswahl-Listen.

## Vorgehen

1. **Flyway V0032** (`product_version_soft_delete.sql`).
2. **Entity** `ProductVersion.deletedAt`.
3. **Repository**:
   - `findByProductIdAndDeletedAtIsNull(productId)` fuer das Listen.
   - `findByProductIdAndVersionAndDeletedAtIsNull(productId, version)`
     fuer den Dubletten-Check beim Anlegen.
4. **Service** `ProductCatalogService`:
   - neue `loescheVersion(productId, versionId)` - idempotent,
     prueft Zugehoerigkeit der Version zum Produkt.
   - `anlegeVersion` pruefte bisher auf `findByProductIdAndVersion`;
     Umstellung auf die deletedAt-Variante, damit eine geloeschte
     Version "wiederbelebt" werden kann.
5. **ProductQueryService.listVersions** filtert deletedAt.
6. **Controller** `DELETE /api/v1/products/{productId}/versions/{versionId}`
   (`CVM_ADMIN`).
7. **ExceptionHandler**: neue `ProductVersionNotFoundException` &rarr;
   404.
8. **Frontend**: neue `productsService.deleteVersion`, Aktions-
   Spalte in der Versions-Tabelle mit Icon-Button und
   `window.confirm`.
9. Tests fuer Happy-Path, Idempotenz, falsches Produkt, Nicht-
   gefunden, null.

## Testerwartung

- `./mvnw -T 1C -pl cvm-application -am test` &rarr; BUILD SUCCESS.
- `./mvnw -T 1C -pl cvm-app,cvm-api -am test` &rarr; BUILD SUCCESS.
- `npx ng build`, `npx ng lint` gruen.

## Wichtig fuer den naechsten Start

- **Neue Flyway-Migration V0032**: vor dem naechsten
  `scripts/start.sh` muss
  `./mvnw -T 1C clean install -DskipTests` laufen.

## Jira

`CVM-99` - Produkt-Versionen Soft-Delete.
