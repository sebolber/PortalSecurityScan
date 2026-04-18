# Iteration 28e - Umgebungen-Verwaltung in der UI

**Jira**: CVM-69
**Branch**: `claude/ui-theming-updates-ruCID`
**Stand**: 2026-04-18

Letzter Baustein aus dem Ursprungsplan "alles ueber die GUI":
Umgebungen koennen jetzt per UI angelegt werden. Tenant-CRUD
bleibt offen (fachlich groesserer Eingriff: Tenant-Spalten-
Migration, JWT-Tenant-Resolver, RLS).

## Umgesetzt

### Backend

- `EnvironmentQueryService.create(command)` legt eine neue
  Umgebung an; validiert Pflichtfelder und Unique-Key.
- Neue Exception `EnvironmentKeyAlreadyExistsException` (409).
- `EnvironmentsController`:
  - `POST /api/v1/environments` erfordert `CVM_ADMIN`.
  - Location-Header auf `/api/v1/environments/{id}`.
- `EnvironmentsExceptionHandler`: 409 fuer Duplicate, 400 fuer
  Validation.

### Tests

- `EnvironmentQueryServiceTest` (5 Tests):
  gueltige Anlage, doppelter Key, leerer Key, fehlende Stage,
  leerer Tenant -> null.
- `EnvironmentsControllerWebTest`:
  + 3 neue Tests (201 Created, 409 Conflict, 400 Bad Request).

### Frontend

- `EnvironmentsService` bekommt `create(req)`.
- Neue Route `/admin/environments` (Rolle `CVM_ADMIN`).
- `AdminEnvironmentsComponent`:
  - Tabelle aller Umgebungen (Key, Name, Stage-Chip, Tenant).
  - Toggle "Neue Umgebung" oeffnet Formular (Key, Name, Stage-
    Dropdown DEV/TEST/REF/ABN/PROD, Tenant optional).
  - Fehler via `ahs-banner`, Erfolg als Snackbar + Reload der
    Liste.
  - Alle Styles auf Tokens (`--space-*`, `--radius-pill`,
    `--color-primary-muted`).
- Sidebar-Eintrag "Umgebungen" mit Icon `layers`.

## Zusammenfassung Iteration-28-Paket ("alles ueber die GUI")

| Teil | Status | Quelle |
|---|---|---|
| 28a Produkt-CRUD | upstream | `main` (Dritt-Commit `6e0c906`) |
| 28b Profil-Editor | mit dieser Session | `893eb1f` |
| 28c Regel-Editor | mit dieser Session | `3b30d87` |
| 28d SBOM-Upload-UI | upstream | `main` (Dritt-Commit `3bd6154`) |
| 28e Umgebungen-CRUD | diese Iteration | CVM-69 |
| 28f Theme-Asset-Upload | mit dieser Session | `6e625c3` |

Admin kann jetzt komplett aus der GUI heraus:

1. Produkte + Versionen anlegen (`/admin/products`)
2. Umgebungen anlegen (`/admin/environments`)
3. Kontextprofile schreiben + aktivieren (`/profiles`)
4. Regeln anlegen + aktivieren + Dry-Run (`/rules`)
5. SBOMs hochladen (`/scans/upload`)
6. Theme + Assets konfigurieren (`/admin/theme`)
7. LLM-Modellprofile anlegen (`/settings`)

## Offene Punkte (27f / 28g+)

- **Tenant-Verwaltungs-UI** (Create/Deaktivieren). Haengt an
  tenant-bezogener RLS-Durchsetzung und JWT-Claim-Parsing.
- **Produkt/Profil-Edit und Soft-Delete** (in
  `offene-punkte.md` bereits vermerkt).
- **Rule-Edit und Retire** (Backend-Endpunkte fehlen).
- **E2E-Test `onboarding.e2e.ts`** fuer den durchgehenden
  Happy-Path Produkt -> Umgebung -> Profil -> Regel -> SBOM-
  Upload -> Queue.
- **Karma-Suite** ist aktuell durch `chart-theme.service.spec.ts`
  (TS4111-Index-Access) blockiert - separater Fix noetig.

## Build / Verifikation

- `./mvnw -T 1C test` -> BUILD SUCCESS. 8 neue Tests (5 Service,
  3 Controller) gegenueber vorherigem Stand.
- `npx ng lint cvm-frontend` -> All files pass linting.
- `npx ng build cvm-frontend` -> Application bundle generation
  complete.
