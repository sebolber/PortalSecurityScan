# Iteration 20 - VEX-Export/Import + Waiver-Management - Fortschritt

**Jira**: CVM-51
**Branch**: `claude/iteration-10-pdf-report-C9sA4`
**Abgeschlossen**: 2026-04-18

## Umgesetzt

### Waiver-Management (CVM-51 a)

- Neue Domain-Enum `WaiverStatus`
  (`ACTIVE | EXPIRING_SOON | EXPIRED | REVOKED`).
- Flyway `V0020__waiver.sql`: Tabelle `waiver` mit
  Check-Constraint auf `status`, FK auf `assessment(id)`,
  Unique-Index auf `assessment_id` (nur ein aktiver Waiver
  pro Assessment).
- `Waiver`-JPA-Entity + `WaiverRepository`
  (`findByStatus`, `findByStatusAndValidUntilBefore`,
  `findByAssessmentId`).
- `WaiverService` mit
  - `grant` (Vier-Augen: `grantedBy != decidedBy`,
    nur fuer `ACCEPT_RISK`/`WORKAROUND`),
  - `extend` (Vier-Augen: `extendedBy != grantedBy`),
  - `revoke`,
  - `byStatus`.
- `WaiverLifecycleJob` (Scheduled `0 0 1 * * *`,
  `@ConditionalOnProperty cvm.waiver.lifecycle.enabled=true`):
  - `ACTIVE` -&gt; `EXPIRING_SOON` bei Restlaufzeit
    &lt;= 30 Tage, Alert `ESKALATION_T1`.
  - `EXPIRING_SOON` -&gt; `EXPIRED` bei Ablauf, zugehoeriges
    Assessment wird via `AssessmentRepository.markiereAlsReview`
    auf `NEEDS_REVIEW` gesetzt.
  - Direkt abgelaufen -&gt; sofort `EXPIRED`.
- REST:
  - `POST /api/v1/waivers`
  - `POST /api/v1/waivers/{id}/extend`
  - `POST /api/v1/waivers/{id}/revoke`
  - `GET  /api/v1/waivers?status=...`
  - Statuscodes: 201 Created, 400 Bad Request,
    404 Not Found, 409 Vier-Augen-Konflikt,
    422 Strategie nicht waiver-faehig.

### VEX-Export/Import (CVM-51 b)

- `VexStatus`, `VexStatement` (Record),
  `VexStatementMapper` (Assessment + Mitigation -&gt;
  Statement, mit Justification-Mapping
  `component_not_present`/`code_not_reachable`/...).
- `VexExporter`:
  - CycloneDX 1.6 (`bomFormat`, `specVersion`, `version`,
    `metadata.timestamp`, `vulnerabilities[]`).
  - CSAF 2.0 (`document.category = csaf_vex`,
    `product_status`, `vulnerabilities[]`).
  - **Deterministisch**: Clock-injiziert,
    Statements nach CVE-Key sortiert,
    `ORDER_MAP_ENTRIES_BY_KEYS`, `INDENT_OUTPUT`.
  - Filtert auf aktuelle (nicht `supersededAt`)
    Assessments fuer die ProductVersion.
- `VexImporter`:
  - Validiert `bomFormat == CycloneDX`.
  - Whitelist fuer States, Original-Case bleibt in der
    Warnung erhalten.
  - Kaputtes JSON -&gt; `errors` statt Exception.
  - **Schreibt nichts** in die DB - die Statements
    sind Vorschlaege fuer die Bewertungs-Queue.
- REST:
  - `GET  /api/v1/vex/{productVersionId}?format=cyclonedx|csaf`
    -&gt; `application/json`, Header `X-VEX-Format`.
  - `POST /api/v1/vex/import` (multipart oder JSON-Body).

## Sicherheits-Invarianten

1. Vier-Augen wird fuer `grant` **und** `extend`
   geprueft (nicht nur einmalig).
2. Waiver ist nur fuer `ACCEPT_RISK`/`WORKAROUND` zulaessig;
   andere Strategien -&gt; 422.
3. Abgelaufener Waiver zieht Assessment automatisch auf
   `NEEDS_REVIEW` - kein "stiller" Fortbestand.
4. VEX-Export ist deterministisch (byte-gleiches JSON bei
   gleichem Input + Clock).
5. VEX-Import produziert **keinen** direkten DB-Zustand,
   sondern nur Vorschlaege.

## Tests

- `WaiverServiceTest` (5/5 gruen).
- `WaiverLifecycleJobTest` (5/5 gruen).
- `VexExporterTest` (5/5 gruen).
- `VexImporterTest` (4/4 gruen).
- `WaiverControllerWebTest` (7/7 gruen).
- `VexControllerWebTest` (8/8 gruen).
- Voller Build `./mvnw -T 1C test` -&gt; BUILD SUCCESS.

## NICHT umgesetzt (bewusst)

- Upload-Bulk-Import mit automatischem Anlegen von
  Assessment-Vorschlaegen - der Import bleibt ein
  Parse/Validate-Endpoint.
- UI-Eingabeformular fuer Waiver (Backend-Vertrag steht,
  Angular-Nachzug separat).
- SPDX-Export (nur CycloneDX + CSAF laut Plan).
