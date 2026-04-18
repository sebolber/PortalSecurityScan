# Iteration 28 – Onboarding-Setup (Produkt, LLM-Profil, SBOM-Upload-UI)

**Jira**: CVM-58 (Produkt-Create), CVM-59 (LLM-Profil-Create), CVM-60 (SBOM-Upload-UI)
**Branch**: `claude/product-llm-cve-setup-SxHIn`

## Ziel

Ein Admin kann im CVM-Frontend (a) ein Produkt + Produktversion anlegen,
(b) ein LLM-Modell-Profil anlegen und einer Umgebung zuordnen, und
(c) eine CycloneDX-SBOM per Drag-and-Drop hochladen und den Scan-Status
live verfolgen.

## Scope

### A – Produkt & Version (CVM-58)
- REST `POST /api/v1/products` (CVM_ADMIN) mit Key-Regex `^[a-z0-9-]{2,64}$`
  und Duplikat-Check → `409 product_key_conflict`.
- REST `POST /api/v1/products/{id}/versions` (CVM_ADMIN) mit Produkt-Existenz-
  und Dubletten-Check (`404`, `409`).
- Angular-Feature `/admin/products` (Rolle ADMIN) mit Listentabelle,
  Inline-Formularen zum Anlegen von Produkt und Version.

### B – LLM-Modell-Profil (CVM-59)
- Flyway `V0026__llm_model_profile_create_audit.sql`:
  `action`-Spalte auf `model_profile_change_log`, `environment_id` nullable
  für `PROFILE_CREATED`.
- REST `POST /api/v1/llm-model-profiles` (CVM_ADMIN) mit Key-Regex
  `^[A-Z0-9_]{2,64}$`, Provider-Parse (`CLAUDE_CLOUD|OLLAMA_ONPREM`), Vier-Augen
  bei GKV-Freigabe, Audit-Eintrag `PROFILE_CREATED`.
- Angular: `SettingsComponent` um Profil-Create-Formular erweitert.

### C – SBOM-Upload-UI (CVM-60)
- Angular-Feature `/scans/upload` (ADMIN/ASSESSOR) mit Produkt-/Versions-
  /Umgebungs-Auswahl, Drag-and-Drop, 5 MB-Clientcheck, Polling von
  `GET /api/v1/scans/{id}`.
- Neuer `ScansService` (core/scans).

## Architekturregeln

- `cvm-api` DTOs kennen keine Persistence-Typen: `provider` wird als
  String uebergeben und im Application-Service geparst.
- Read-Endpunkte bleiben ohne `@PreAuthorize`; Schreib-Endpunkte setzen
  `@PreAuthorize("hasAuthority('CVM_ADMIN')")`.

## TDD-Reihenfolge

Outside-In je Teil: Web-Test (Controller) → Service-Test → Impl → Frontend.

## Abschluss

Fortschritts- und Test-Summary in dieser Iteration.
