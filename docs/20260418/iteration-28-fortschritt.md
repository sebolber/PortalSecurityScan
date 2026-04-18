# Iteration 28 – Fortschritt

Stand: 18.04.2026, Branch `claude/product-llm-cve-setup-SxHIn`.

## Was gebaut wurde

### Teil A – Produkt & Version (CVM-58)

- `cvm-application`:
  - `ProductCatalogService.anlege(ProductCreateInput)` und
    `anlegeVersion(UUID, ProductVersionCreateInput)` — inkl.
    Key-Regex-Pruefung, Whitespace-Trim, Duplikat- und Existenz-Checks.
  - Neue Ausnahmen `ProductKeyConflictException`,
    `ProductNotFoundException`, `ProductVersionConflictException`.
- `cvm-api`:
  - `ProductsController` um zwei `@PreAuthorize('CVM_ADMIN')`-POST-Endpunkte
    erweitert, mit `ProductCreateRequest` (Pattern `^[a-z0-9-]{2,64}$`) und
    `ProductVersionCreateRequest`.
  - `ProductsExceptionHandler` mappt die neuen Ausnahmen auf `400/404/409`.
- `cvm-frontend`:
  - `ProductsService.create/createVersion` erweitert.
  - Neues Feature `/admin/products` (Route + Menueintrag + Component +
    HTML/SCSS). Inline-Formulare fuer Produkt- und Versions-Anlage.

### Teil B – LLM-Modell-Profil (CVM-59)

- `cvm-persistence`:
  - Flyway `V0026__llm_model_profile_create_audit.sql`:
    - `action TEXT NOT NULL DEFAULT 'PROFILE_SWITCHED'` mit CHECK-Constraint.
    - `environment_id` wird nullable, Row-Level-Check erzwingt
      `environment_id` nur bei `PROFILE_SWITCHED`.
  - `ModelProfileChangeLog`-Entity: neues `Action`-Enum, `environmentId`
    nullable, `@PrePersist` setzt Default auf `PROFILE_SWITCHED`.
- `cvm-application`:
  - `ModelProfileService.createProfile(CreateCommand)` mit Profile-Key-Regex
    `^[A-Z0-9_]{2,64}$`, Provider-String-Parse, GKV-Vier-Augen-Pflicht,
    Budget >= 0, Audit-Eintrag `PROFILE_CREATED`.
  - Neue Ausnahme `ProfileKeyConflictException` (innerer Typ des Services).
- `cvm-api`:
  - `LlmModelProfilesController` um `POST /api/v1/llm-model-profiles`
    (CVM_ADMIN) erweitert.
  - `ModelProfileCreateRequest`-DTO (provider als String,
    `@Pattern(CLAUDE_CLOUD|OLLAMA_ONPREM)`), um die Modulgrenzen einzuhalten.
  - `ModelProfileExceptionHandler` deckt nun beide Controller ab
    (`assignableTypes = {ModelProfileController, LlmModelProfilesController}`)
    und mappt `profile_key_conflict` → 409.
- `cvm-frontend`:
  - `ModelProfileService.createProfile` neu.
  - `SettingsComponent` um ein Profil-Create-Formular erweitert (inline,
    aufklappbar), inkl. Frontend-Validierung von Regex, Budget und
    Vier-Augen.

### Teil C – SBOM-Upload-UI (CVM-60)

- `cvm-frontend`:
  - Neuer `ScansService` (core/scans) fuer Multipart-Upload via
    HttpClient + Status-Abfrage ueber `ApiClient`.
  - Neue Feature-Route `/scans/upload` (ADMIN + ASSESSOR).
  - `ScanUploadComponent` mit Dropzone, Cascading Product/Version,
    optionaler Umgebungsauswahl, 5-MB-Client-Check, Polling bis max.
    60 s (30 × 2 s) und freundlichen deutschen Fehlertexten fuer
    `sbom_schema_error`, `sbom_parse_error`, `scan_already_ingested`,
    `413`, `403`.

## Menue & Navigation

Zwei neue Menueeintraege in `role-menu.service.ts`:
- `Produkte` (`/admin/products`, nur ADMIN)
- `Scan hochladen` (`/scans/upload`, ADMIN + ASSESSOR)

Passende `role-menu.service.spec.ts`-Tests ergaenzt.

## Was nicht gebaut wurde (bewusst out of Scope)

- Produkt-/Version-Editierung und -Loeschung.
- LLM-Profil-Edit/-Deaktivierung.
- Frontend-E2E-Suite (Playwright) fuer Admin-Onboarding.
- Server-seitige Dateigroesse-Begrenzung per `spring.servlet.multipart.max-file-size`
  (clientseitig begrenzt; serverseitige Konfiguration separat).

## Offene Punkte

- Karma-Suite laesst sich im Repo-Zustand nicht laufen: vorbestehende
  TS4111-Fehler in `cvm-frontend/src/app/core/theme/chart-theme.service.spec.ts`
  blockieren den Suite-Start. Betrifft nicht diese Iteration — als
  offener Punkt notiert.
