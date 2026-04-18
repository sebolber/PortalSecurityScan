# Iteration 22 - Test-Summary

**Build**: `./mvnw -T 1C test` -> **BUILD SUCCESS**.

## Modul-Summary

| Modul                     | Tests | Failures | Errors | Skipped |
|---------------------------|-------|----------|--------|---------|
| cvm-domain                |     4 |        0 |      0 |       0 |
| cvm-persistence           |     0 |        0 |      0 |       6 (Docker) |
| cvm-application           |   174 |        0 |      0 |       0 |
| cvm-integration           |    18 |        0 |      0 |       0 |
| cvm-llm-gateway           |    62 |        0 |      0 |       0 |
| cvm-ai-services           |   111 |        0 |      0 |       0 |
| cvm-api                   |    94 |        0 |      0 |       0 |
| cvm-app                   |     0 |        0 |      0 |       5 (Docker) |
| cvm-architecture-tests    |     8 |        0 |      0 |       0 |
| **Gesamt**                | **471** |  **0** |  **0** |  **11 (Docker-skip)** |

## Neue Tests in Iteration 22

### cvm-api (+5)

- `com.ahs.cvm.api.tenant.TenantContextFilterTest` (5 Tests)
  - setzt Tenant aus dem JWT-Claim `tenant_key`,
  - faellt bei fehlendem Claim auf den Default-Tenant zurueck,
  - unbekannter Claim-Key laesst Default greifen,
  - raeumt `TenantContext` auch bei Exception im Chain auf,
  - ohne Authentifizierung greift ebenfalls der Default-Tenant.

### cvm-application (+3)

- `com.ahs.cvm.application.pipeline.PipelineGateServiceTest` (+1)
  - `publiziertEvent`: Event mit repoUrl und MR-Id wird publiziert.
  - bestehende `rateLimit`-Test verifiziert zusaetzlich
    `verify(events, never()).publishEvent(...)`.
- `com.ahs.cvm.application.kpi.KpiSnapshotWriterTest` (2 Tests)
  - persistiert neuen Snapshot mit offenen Severities +
    Automation-Rate,
  - aktualisiert bestehenden Snapshot idempotent pro
    (Tag, productVersionId, environmentId).

### cvm-integration (+5)

- `com.ahs.cvm.integration.git.PipelineGateMrCommentListenerTest`
  (5 Tests)
  - postet Gate-Kommentar mit PASS/WARN/FAIL-Icon,
  - disabled -> kein Post,
  - ohne repoUrl oder mergeRequestId -> kein Post,
  - Provider-Fehler wirft nicht nach aussen,
  - rendert PASS-Kommentar ohne Aktionsempfehlung.

## Architektur-Tests

`cvm-architecture-tests/ModulgrenzenTest` (7/7 + 1 SpringBean)
bleibt gruen:

- `api -> persistence` weiterhin verboten - der neue
  `TenantContextFilter` importiert nur
  `com.ahs.cvm.application.tenant.*`.
- `integration -> api` weiterhin verboten - der neue
  `PipelineGateMrCommentListener` importiert nur
  `com.ahs.cvm.application.pipeline.*`.
- `SpringBeanKonstruktorTest`: alle neuen Beans haben genau
  einen `@Autowired`-Konstruktor bzw. Single-Constructor (implizit).

## Nicht ausgefuehrt

- **Testcontainers-Integration** fuer V0023 (Seed) und V0024
  (KPI-Snapshot-Tabelle) - weiterhin Docker-abhaengig.
- **End-to-End** (Angular + Backend) - kein Chromium im Sandbox.
- **Real-Keycloak-Flow** - kein Keycloak-Instanz-Run in der
  Sandbox; Filter ist ueber `MockHttpServletRequest` +
  `JwtAuthenticationToken` verdrahtet.
