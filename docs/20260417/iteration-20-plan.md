# Iteration 20 - VEX-Export/Import + Waiver-Management - Plan

**Jira**: CVM-51
**Branch**: `claude/iteration-10-pdf-report-C9sA4`
**Abhaengigkeit**: Iteration 06 (Bewertungs-Workflow), 09 (Alerts).

## Architektur

### Waiver
- `cvm-domain`: `WaiverStatus` (ACTIVE / EXPIRING_SOON /
  EXPIRED / REVOKED).
- `cvm-persistence`: `Waiver` + `WaiverRepository` + Flyway
  `V0020__waiver.sql`.
- `cvm-application/waiver`:
  - `WaiverService` (anlegen, extend, revoke) mit Vier-Augen-
    Prinzip. `WaiverNotApplicableException` wenn Mitigation-
    Strategy nicht `ACCEPT_RISK` oder `WAIT_UPSTREAM`.
  - `WaiverLifecycleJob` taeglich: Status-Uebergaenge +
    Alert-Trigger. Bei EXPIRED setzt die Zugehoerige Assessment-
    Zeile auf NEEDS_REVIEW (via vorhandene
    `markiereAlsReview`-Query).
  - `Clock`-injiziert fuer zeitabhaengige Tests.

### VEX
- `cvm-application/vex`:
  - `VexExporter` mit zwei Implementierungen: CycloneDX
    (`cyclonedx-core-java`) und CSAF 2.0 (JSON-Strings).
  - `VexImporter`: liest CycloneDX-VEX-JSON, produziert
    `ProposedAssessmentView` pro Statement - Assessment wird
    NICHT geschrieben, die Queue uebernimmt.
- REST:
  - `GET  /api/v1/vex/{productVersionId}?format=cyclonedx|csaf`
  - `POST /api/v1/vex/import` (Multipart).

## Waiver-Statusmaschine

```
ACTIVE --(30d vor validUntil)--> EXPIRING_SOON
EXPIRING_SOON --(validUntil)--> EXPIRED
<any> --(revoke)--> REVOKED
```

Expired und Revoked sind terminal. Extend setzt zurueck auf
ACTIVE mit neuem `validUntil`.

## Invarianten (durch Tests gehaertet)

1. Waiver-Anlage nur bei passender Mitigation-Strategy.
2. Vier-Augen bei Anlage + Extend (grantedBy != assessment.decidedBy).
3. Lifecycle-Job schreibt bei EXPIRED `NEEDS_REVIEW` auf das
   Assessment, nie `APPROVED`.
4. VEX-Import erzeugt nur Vorschlaege, nie APPROVED-Datensaetze.
5. VEX-Export-Mapping NOT_APPLICABLE -&gt; `not_affected`
   hart getestet.

## Tests

1. `WaiverServiceTest` (5): anlegen, falsche Strategy, Vier-
   Augen, extend, revoke.
2. `WaiverLifecycleJobTest` (4): ACTIVE, EXPIRING_SOON (Alert),
   EXPIRED (NEEDS_REVIEW), REVOKED.
3. `VexExportTest` (5): alle Status-Mappings + Justification.
4. `VexImportTest` (3): happy, Schema-Fehler, unbekannte Felder.
5. `WaiverControllerWebTest` (4): CRUD + 400/404/409.
6. `VexControllerWebTest` (3): Export CycloneDX, Export CSAF,
   Import.

## Scope NICHT IN

- VEX-Signatur (Cyclonedx unterstuetzt das, Schluessel fehlt aber).
- Auto-Waiver-Verlaengerung durch KI (explizit nicht).
- VEX-Roundtrip gegen externe Testsuites.
