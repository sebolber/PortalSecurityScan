# Iteration 76 - Plan: Product.repoUrl-Feld fuer Reachability

**Jira**: CVM-313

## Ziel

Der Reachability-Auto-Trigger aus Iteration 70 hat einen
`NoopReachabilityAutoTriggerAdapter`. Um den echten Adapter
bauen zu koennen, braucht der Finding eine Beziehung zu einer
Git-Repository-URL. Produkte sind der richtige Traeger - ein
Produkt hat ein Haupt-Repo.

## Umfang

- **Flyway** V0040: `ALTER TABLE product ADD COLUMN repo_url
  VARCHAR(512)` (nullable).
- **Entity** `Product`: neues Feld `repoUrl` mit Lombok-Accessor.
- **DTO** `ProductView` (Application): neuer Feld `repoUrl`.
  `ProductView.from(...)` kopiert das Feld.
- **Service** `ProductCatalogService.aktualisiere(...)`:
  `ProductUpdateInput` um `repoUrl` erweitert. Leer-String
  loescht den Eintrag, `null` laesst ihn unveraendert.
  Kompatibler 2-arg-Konstruktor bleibt fuer Bestandsaufrufer.
- **REST-DTO** `ProductUpdateRequest` (API): zusaetzliches Feld
  `repoUrl` mit `@Size(max=512)`.
- **Controller** `ProductsController#aktualisieren(...)`:
  Mapping auf den neuen Input-Record.
- **Frontend** `ProductsService.ProductView/UpdateRequest`:
  `repoUrl: string | null` Feld.
- **Frontend** `AdminProductsComponent.bearbeiteProdukt(...)`:
  weiteres `window.prompt` fuer die Repo-URL.

## Nicht-Umfang

- Kein tatsaechlicher Adapter fuer
  `ReachabilityAutoTriggerPort` - das ist die naechste
  Iteration.

## Abnahme

- `./mvnw -T 1C test` gruen, bestehende ProductsControllerWebTest
  folgt dem neuen Record-Arity.
- `npx ng lint`, `npx ng build`, Karma gruen.
