# Iteration 28f - Theme-Asset-Upload-UI

**Jira**: CVM-68
**Branch**: `claude/ui-theming-updates-ruCID`
**Stand**: 2026-04-18

Erweitert die bestehende Theme-Admin-UI (`/admin/theme`) um einen
Multipart-Upload fuer Logo, Favicon und Schrift. Der Backend-
Endpunkt `POST /api/v1/admin/theme/assets` steht seit Iteration
27b; hier folgt die UI-Seite dazu.

## Umgesetzt

### BrandingHttpService

- Neue Methode `uploadAsset(kind, file)` in
  `core/theme/branding.service.ts`. Baut `FormData` (kind, file)
  und schickt es via `HttpClient` an
  `POST /api/v1/admin/theme/assets`.
- Rueckgabe-DTO `BrandingAssetResponse` (id, kind, contentType,
  sizeBytes, sha256, url).
- ApiClient-Basis-URL wird wiederverwendet (`api.url(...)`), damit
  das Setup in `AppConfigService` nicht dupliziert wird.
- Explizites `HttpClient`-Injekt statt `ApiClient`, weil der
  Generic-Wrapper Multipart nicht abbildet (setzt kein
  `Content-Type: multipart/form-data`).

### AdminThemeComponent

- Dritte Card "Assets hochladen" im Grid (spannt beide Spalten).
- Drei Slots: **Logo** (SVG/PNG, 512 KB), **Favicon** (ICO/PNG/
  SVG, 512 KB), **Schrift** (woff2, 2 MB) - jeweils mit
  passendem `accept`-Filter und Hinweistext.
- Native `<input type="file">` hinter stylistem `mat-stroked-
  button`-Label (`cvm-admin-theme__file-trigger`).
- Nach Upload:
  - Draft wird automatisch gepatcht: `logoUrl` /
    `faviconUrl` / `fontFamilyHref` bekommen die zurueckgelieferte
    URL.
  - Erfolgsmeldung als `ahs-banner` kind=success mit
    Groessenangabe in KB.
  - Der Upload-Spinner pro Slot ist isoliert (Record nach
    AssetKind).
- Fehler (MIME-Ablehnung, Groesse ueberschritten, SVG-Sanitizer-
  Verdacht) landen im bestehenden Error-Banner.

### Styles

- Token-konform: `--space-*`, `--radius-sm`, `--font-family-mono`,
  `--color-surface-muted`.
- Die Asset-Karte spannt beide Spalten (`grid-column: 1 / -1`),
  damit lange URLs nicht die Formular-Spalte sprengen.

## Build / Verifikation

- `npx ng lint cvm-frontend` -> All files pass linting.
- `npx ng build cvm-frontend` -> Application bundle generation
  complete.
- Backend unveraendert (Upload-Endpunkt + Sanitizer + Tests
  bestehen seit 27b).

## Folge-Iterationen

Noch offen aus dem Gesamt-Paket "alles ueber die GUI":

- Nach Upstream-Rebase bereits auf `main`: Produkt-Anlage
  (`/admin/products`), SBOM-Upload (`/scans/upload`), LLM-
  Modellprofil-Anlage.
- Noch offen: Regel-Editor (Create/Update im Frontend),
  Umgebungs-CRUD, Tenant-Verwaltung, Produkt- und Profil-Edit
  inkl. Soft-Delete (siehe `offene-punkte.md`).
