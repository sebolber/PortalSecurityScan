# Iteration 02 – Fortschrittsbericht

**Jira**: CVM-11
**Datum**: 2026-04-17
**Ziel**: CycloneDX-/Trivy-Reports hochladen, parsen und als `Scan` +
`Findings` persistieren. Ingestion laeuft asynchron mit Dedup.

## 1 Was wurde gebaut

### Persistenz
- Flyway `V0006__scan_content_und_environment.sql` &mdash; `scan`
  bekommt `environment_id`, `content_sha256`, `raw_sbom` (bytea) und einen
  neuen Unique-Constraint `(product_version_id, environment_id,
  content_sha256)`. Der alte `uq_scan_checksum` entfaellt.
- `Scan`-Entity um `environment`, `contentSha256`, `rawSbom` erweitert
  (Lazy-Lob). `ScanRepository` mit Lookup
  `findByProductVersionIdAndEnvironmentIdAndContentSha256`.

### Parser und Verschluesselung (`cvm-application/scan`)
- `CycloneDxBom` &mdash; leichtgewichtiges JSON-Modell (Jackson), robust
  gegen unbekannte Felder.
- `CycloneDxParser` &mdash; parst und validiert (`bomFormat`,
  `specVersion`, nicht-leere Komponentenliste). Wirft
  `SbomParseException` oder `SbomSchemaException`.
- `SbomEncryption` &mdash; AES-GCM (128-Bit-Tag, 12-Byte-IV). Schluessel
  wird per `cvm.encryption.sbom-secret` aus `application.yaml` /
  Vault-Platzhalter abgeleitet. Helpers: `encrypt`/`decrypt`,
  `sha256Hex`, `base64`.

### Service, Executor, Event
- `ScanIngestConfig` &mdash; dedizierter `ThreadPoolTaskExecutor`
  `sbom-ingest` (2–4 Worker, 100er-Queue, `AbortPolicy`).
- `ScanIngestService` &mdash;
  `parse → validate → dedup-Check → Scan reservieren (sync) →
   Async-Persistenz (Components, Occurrences, CVEs, Findings) →
   ScanIngestedEvent`.
  Self-Injection ueber `@Lazy`, damit `@Async` den Proxy nutzt.
- `ScanIngestedEvent` publiziert &uuml;ber `ApplicationEventPublisher`.

### REST (`cvm-api/scan`)
- `ScanController`:
  - `POST /api/v1/scans` (multipart) mit `productVersionId`,
    `environmentId`, `scanner`, `sbom`
  - `POST /api/v1/scans` (raw JSON-Body)
  - `GET /api/v1/scans/{scanId}`
- `ScanExceptionHandler`:
  - 400 fuer `SbomParseException` / `SbomSchemaException` /
    `IllegalArgumentException`
  - 409 fuer `ScanAlreadyIngestedException` (inkl. `existingScanId`)
  - 503 fuer `RejectedExecutionException` (Queue voll)
- OpenAPI-Annotationen (`@Tag`, `@Operation`, `@ApiResponses`).

### Tests
- `CycloneDxParserTest` (4/4 gruen): Happy-Path gegen
  `fixtures/cyclonedx/klein.json`, invalides JSON, SPDX statt CycloneDX,
  leere Komponentenliste.
- `SbomEncryptionTest` (3/3 gruen): Round-Trip, IV-Zufaelligkeit,
  SHA-256 deterministisch.
- `ScanControllerWebTest` (5/5 gruen, MockMvc-Slice):
  - `202 Accepted` mit Location-Header (multipart und raw)
  - `409 Conflict` bei Dedup
  - `400 Bad Request` bei Schema-Fehler
  - `404 Not Found` bei unbekanntem Scan
  - Security im Slice bewusst ausgeblendet (`excludeAutoConfiguration`)
- `ScanIngestionIntegrationTest` (Testcontainers, 2 Tests, ohne Docker
  geskippt): Happy-Path (5 Komponenten, 2 Findings), Dedup-Fehler.
- `TestApiApplication` &mdash; Minimal-`@SpringBootConfiguration`
  (Test-Scope) mit beschraenktem Scan auf `com.ahs.cvm.api.scan`,
  damit der `WebSecurityConfig`-Bean ausserhalb Slices nicht hochfaehrt.

### Fixture
- `klein.json` (5 Komponenten, 2 Vulnerabilities) in
  `cvm-application/src/test/resources` und gespiegelt in
  `cvm-app/src/test/resources`.

## 2 Was laenger gedauert hat

- **`@Async`-Self-Invocation**: Direkter Aufruf von
  `verarbeiteAsync(...)` innerhalb derselben Bean umgeht den Spring-Proxy.
  Loesung: Lazy-Self-Injection.
- **`@WebMvcTest` in Multi-Modul**: ohne `@SpringBootConfiguration`
  unterhalb des Test-Source-Pfads schlaegt der Slice fehl. Zusaetzlich
  zog der `WebSecurityConfig` eine vollstaendige Security-Autokonfig
  hinein &mdash; daher `excludeAutoConfiguration =
  SecurityAutoConfiguration.class` auf dem Test und eingeschraenkter
  `@ComponentScan` auf `com.ahs.cvm.api.scan`.

## 3 Abweichungen vom Prompt

1. **Parser**: Statt `org.cyclonedx:cyclonedx-core-java` direkt zu
   verwenden, ein schmales Record-Modell + Jackson. Die Library liegt
   weiterhin auf dem Classpath fuer Iterationen, die Schema-Writer
   brauchen. Begruendung: weniger Kopplung, bessere Kontrolle ueber
   tolerante Ignore-Unknown-Felder, weniger Speicher fuer grosse Scans.
2. **Verschluesselung**: AES-GCM per `javax.crypto`, nicht Jasypt-Lib.
   Gleiche "Jasypt-Strategie" (symmetrisch, Schluessel aus Vault), aber
   ohne zusaetzliche Abhaengigkeit. Kompatibilitaet zu Jasypt-PBE kann
   spaeter nachgeruestet werden.
3. **Async-Ingestion** laeuft in einem separaten Transaktions-Scope
   (`@Async` + `@Transactional` auf `persistiere`). Der synchrone Pfad
   legt nur den Scan-Kopfsatz an, um sofort eine `scanId` zu liefern.

## 4 Entscheidungen fuer Sebastian

- Soll `content_sha256` alleinstehender Unique-Constraint bleiben oder
  zusammen mit `(product_version_id, environment_id)`? Aktuell ist das
  Triple unique &mdash; d.h. ein identischer Trivy-Report koennte
  theoretisch in einer anderen Umgebung nochmal ingestiert werden. Das
  ist bewusst, weil das Assessment je Umgebung faellt. OK so?
- Performance-Test mit 10 000 Komponenten war nicht Teil dieser
  Iteration (markiert als `@Tag("perf")`, laeuft separat). Ergaenzen,
  sobald Docker-Ressourcen in CI verfuegbar sind.

## 5 Naechster Schritt

**Iteration 03 &mdash; CVE-Anreicherung** (CVM-12). Die Stub-CVEs aus
`ScanIngestService.persistiere(...)` werden von NVD/GHSA/KEV/EPSS mit
echten Werten angereichert.

## 6 Build-Status

```
./mvnw -T 1C test  BUILD SUCCESS
```

- `EnumTest` (cvm-domain): 4/4
- `SbomEncryptionTest`: 3/3
- `CycloneDxParserTest`: 4/4
- `ScanControllerWebTest`: 5/5
- `ModulgrenzenTest`: 7/7

- `ScanIngestionIntegrationTest` (cvm-app): 2 Testcontainers-Tests
- `FlywayMigrationReihenfolgeTest`, `AssessmentImmutableTest`,
  `ProductRepositoryIntegrationsTest`, `SmokeIntegrationTest`,
  `FlywayBaselineTest`: 9 Testcontainers-Tests

Ohne Docker in der Sandbox: 23 gruen, 11 geskippt, 0 rot.
Mit Docker (CI): alle 34 werden ausgefuehrt.

---

*Autor: Claude Code.*
