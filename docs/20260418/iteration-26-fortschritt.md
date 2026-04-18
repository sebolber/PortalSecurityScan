# Iteration 26 - Inventar-UI (CVEs + Komponenten) - Fortschritt

**Jira**: CVM-57
**Branch**: `claude/iteration-22-continuation-GSK7w`
**Abgeschlossen**: 2026-04-18

## Umgesetzt

### Backend

- **`CveQueryService`** + **`CveView`** (cvm-application).
  `findPage(searchTerm, severityFilter, onlyKev, page, size)`
  liefert `Page<CveView>`. Schlagwort-Suche ueber
  `cveId` und `summary`, Severity-Ableitung aus
  `cvssBaseScore`. Stream-Filter mit TODO-Marker auf Criteria.
- **`CvesController`** (`GET /api/v1/cves`) mit
  Query-Parametern `q`, `severity`, `kev`, `page`, `size` ->
  `CvePageResponse(items, page, size, totalElements,
  totalPages)`.
- **`ProductQueryService`** + **`ProductView`** +
  **`ProductVersionView`** (cvm-application).
- **`ProductsController`**:
  - `GET /api/v1/products` -> Liste der Produkte
    (alphabetisch nach `key`),
  - `GET /api/v1/products/{id}/versions` -> Versionen
    (neueste zuerst, `releasedAt` absteigend).
- **WebMvcTests** fuer beide Controller (4 Tests).

### Frontend

- **`CvesService`** (paginierter Lookup via URLSearchParams).
- **`ProductsService`** (list + versions).
- **`CvesComponent`** (`/cves`):
  - Filter-Card mit Suchfeld, Severity-Toggle
    (`CRITICAL|HIGH|MEDIUM|LOW|INFORMATIONAL|Alle`), Only-KEV-
    Switch.
  - Paginated Material-Tabelle mit Severity-Chip, CVSS,
    KEV-Icon, EPSS-Prozent, Veroeffentlicht, Summary.
  - `MatPaginator` mit Seitengroessen 10/25/50.
- **`ComponentsComponent`** (`/components`):
  - Zwei-Spalten-Layout: Produktliste links, Versionen rechts.
  - Klick auf Produkt laedt dessen Versionen
    (`gitCommit`, `releasedAt`).
  - Auto-Auswahl des ersten Produkts beim Laden.

## Build

- `./mvnw -T 1C test` -> BUILD SUCCESS
  (cvm-api 106 Tests, +4 neue gegen Iter 25).
- `npx ng lint` -> All files pass linting.
- `npx ng build` -> Application bundle generation complete
  (2.06 MB initial, bekannte Budget-Warnung).

## Nicht in 26

- **Detail-CVE-Seite** mit Findings-Cross-Link
  (folgt mit Queue-Detail-Ausbau).
- **Volltextsuche auf `advisories`** (aktuell nur `cveId`
  und `summary`).
- **Criteria-basiertes Server-Paging** (bleibt im
  offene-punkte-Log als Performance-Optimierung fuer
  grosse Kataloge).
- **Produkt-/Versions-CRUD-UI** (nur Read in dieser
  Iteration).
