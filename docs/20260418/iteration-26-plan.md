# Iteration 26 - Inventar-UI: CVEs + Komponenten - Plan

**Jira**: CVM-57
**Branch**: `claude/iteration-22-continuation-GSK7w`
**Datum**: 2026-04-18

## Auftrag

Die Menueseiten "CVEs" und "Komponenten" sind bisher nur
Empty-State-Platzhalter. Iteration 26 erstellt die passenden
Backend-Read-Endpunkte und die dazu passenden Angular-Seiten.

## Scope

### Backend

- **`CveQueryService`** + **`CveView`-Record**
  (cvm-application), Paging + Suche:
  - `findPage(searchTerm, severityFilter, onlyKev, page, size)`.
- **`CvesController`** (`GET /api/v1/cves`, authenticated):
  Query-Parameter `q`, `severity`, `kev`, `page`, `size`.
  Liefert `CvePageResponse(items[], page, size, totalElements,
  totalPages)`.

- **`ProductQueryService`** + **`ProductView`**,
  **`ProductVersionView`** Records.
- **`ProductsController`**:
  - `GET /api/v1/products` - Liste aller Produkte.
  - `GET /api/v1/products/{id}/versions` - Versionen eines
    Produkts, neueste zuerst.
- Alle Endpunkte authenticated; keine Schreiboperationen.

- **WebMvcTests** fuer beide Controller.

### Frontend

- **`CvesService`**, **`ProductsService`** (core/...).
- **`CvesComponent`** ersetzt den EmptyState:
  - Suchfeld (`q`), Severity-Chip-Filter, KEV-Toggle.
  - Paginierte Tabelle (CVE-Key, Severity, CVSS, KEV, EPSS,
    Published-At, Summary-gekuerzt).
  - Detail-Aufklappen mit CWEs + Advisories-Links.
- **`ComponentsComponent`** ersetzt den EmptyState:
  - Produktliste links, Auswahl -> Versionen rechts.
  - Version-Detail: `version`, `gitCommit`, `releasedAt`.

### Tests

- Backend WebMvc-Slice-Tests (mindestens List + Filter).
- Frontend: ng lint + ng build gruen.

## Nicht in 26

- Schreiboperationen auf CVEs/Products (Feeds machen das aus
  dem Enrichment-Pfad bereits).
- Detail-CVE-Seite mit Findings-Cross-Link (folgt mit
  Queue-Detail-Erweiterung).
- Playwright-E2E (Chromium fehlt).
