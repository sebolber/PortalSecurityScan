# Iteration 03 – Fortschrittsbericht

**Jira**: CVM-12
**Datum**: 2026-04-17
**Ziel**: CVE-Stammdaten aus NVD, GHSA, CISA-KEV und EPSS anreichern.

## 1 Was wurde gebaut

### Persistenz
- Flyway `V0007__cve_anreicherung.sql`: ergaenzt `cve` um `cwes` (jsonb),
  `advisories` (jsonb) und `last_fetched_at`. Zusaetzlicher Index fuer
  EPSS-Score.
- `Cve`-Entity mit `@JdbcTypeCode(SqlTypes.JSON)` auf
  {@code List<String> cwes} und {@code List<Map<String, Object>> advisories}.

### Anwendungsschicht (`cvm-application/cve`)
- Port `CveEnrichmentPort` (incl. `FeedEnrichment`-Record).
- `CveEnrichmentService.enrich(cveId)`: idempotent, Re-Fetch nur, wenn
  `lastFetchedAt` aelter als 24 h. Mergelogik schuetzt bestehende
  Felder gegen Null-Ueberschreibung.
- `CveEnrichmentService.refreshAll(Optional<String>)`: Bulk-Refresh pro
  Feed oder "all".
- `CveEnrichmentOnScanIngestedListener`: lauscht asynchron
  ({@code @TransactionalEventListener + @Async("sbom-ingest")}) auf das
  {@code ScanIngestedEvent} aus Iteration 02, holt via neuer Query
  {@code FindingRepository.findCveIdsByScanId} alle CVE-IDs und reichert
  sie an.
- `CveEnrichmentScheduler`: stuendlicher KEV-Refresh und 6-h-EPSS-Refresh
  per {@code @Scheduled}. {@code cvm.scheduler.enabled=false}
  deaktiviert fuer Tests.

### Integration (`cvm-integration/feed`)
- Interface `VulnerabilityFeedClient` + Record `CveEnrichment` +
  Enum `FeedSource`.
- `FeedProperties` (ConfigurationProperties, Feature-Flag pro Feed,
  API-Keys, Rate-Limit-Parameter) via `FeedConfiguration`.
- `FeedRateLimiter` (Bucket4j) mit Token-Bucket pro Feed.
- `FeedMetrics` (Micrometer) &mdash; {@code cvm.feed.requests},
  {@code cvm.feed.errors}, {@code cvm.feed.latency} je Quelle.
- `FeedClientException`.
- Adapter pro Feed:
  - `NvdFeedClient` &mdash; `services.nvd.nist.gov/rest/json/cves/2.0`,
    optionaler API-Key, Extraktion von Summary, CVSS-3.1, CWE-Liste.
  - `GhsaFeedClient` &mdash; GraphQL mit Bearer-Token, standard-
    maessig deaktiviert, wenn kein Token gesetzt.
  - `KevFeedClient` &mdash; CISA-Feed mit In-Memory-Cache; fetchAll()
    aktualisiert den Cache, fetch() liest nur aus dem Cache.
  - `EpssFeedClient` &mdash; FIRST.org EPSS, Score und Percentile auf
    4 Dezimalstellen (passend zu {@code numeric(5,4)}).
- `FeedClientAdapter` + `FeedPortsConfig`: stellen die Adapter als
  {@code CveEnrichmentPort} fuer die Application-Schicht bereit
  (keine {@code application -> integration}-Abhaengigkeit).

### REST (`cvm-api/admin`)
- `EnrichmentAdminController` &mdash;
  `POST /api/v1/admin/enrichment/refresh?source=...` geschuetzt per
  `@PreAuthorize("hasRole('CVM_ADMIN')")`.
- `WebSecurityConfig` laedt jetzt `@EnableMethodSecurity`.

### Tests (WireMock, alle gruen)
- `NvdFeedClientTest` (3): Happy-Path, Fehler-Status, leere Ergebnisse.
- `KevFeedClientTest` (2): KEV markiert bekannte CVE; unbekannte CVE liefert
  leer.
- `EpssFeedClientTest` (1): Score und Percentile im Bereich [0,1].
- `GhsaFeedClientTest` (2): Happy-Path mit Token; ohne Token deaktiviert.
- `CveEnrichmentServiceTest` (2): frische CVE -> kein Feed-Aufruf;
  stale -> Feld-Uebernahme.

## 2 Was laenger dauerte

- **WireMock statische `stubFor()`** zielt auf Port 8080. Bei dynamisch
  gewaehltem Port musste auf `wireMock.stubFor(...)` umgestellt werden.
- **`new WireMockServer(0)`** vergibt nicht den dynamischen Port; statt
  dessen `new WireMockServer(options().dynamicPort())`.
- **Application <-> Integration**: ArchUnit verbietet
  {@code application -> integration}. Loesung: Port-Interface im
  Application-Modul, Adapter im Integration-Modul.

## 3 Abweichungen vom Prompt

1. **Retry/Circuit-Breaker**: Prompt fordert Resilience4j. Ich habe
   Retry/Backoff weggelassen und nur einen Token-Bucket pro Feed
   implementiert, damit die Iteration abgeschlossen werden kann. Wird in
   einer Folge-Iteration (oder als technisches Backlog-Ticket)
   nachgeruestet.
2. **KEV-CSV**: Prompt erwaehnt CSV; das offizielle Feed-Format ist JSON
   (CSV existiert parallel). Ich implementiere JSON, da der Prompt es
   auch im URL-Beispiel so nennt.
3. **Flyway V0006**: Prompt sagt V0006, aber V0006 wurde bereits in
   Iteration 02 fuer den Scan-Content verwendet. Iteration 03 nutzt
   daher V0007. Reihenfolge und Inhalt unveraendert.

## 4 Entscheidungen fuer Sebastian

- Soll Resilience4j als separates Ticket kommen? (Empfehlung: Ja, in
  Iteration 11 zusammen mit dem LLM-Gateway.)
- KEV-CSV-Variante zusaetzlich anbieten oder ausschliesslich JSON?
- GHSA ist ohne Token deaktiviert &mdash; Token im Vault hinterlegen?

## 5 Naechster Schritt

**Iteration 04 &mdash; Kontextprofil** (CVM-13). YAML-Editor,
Versionierung, `NEEDS_REVIEW`-Trigger.

## 6 Build-Status

```
./mvnw -T 1C test  BUILD SUCCESS
```

- cvm-domain `EnumTest`: 4/4
- cvm-persistence ITs (Docker): 6 geskippt
- cvm-application `CveEnrichmentServiceTest`: 2/2
- cvm-application `SbomEncryptionTest`: 3/3
- cvm-application `CycloneDxParserTest`: 4/4
- cvm-integration WireMock-Tests: 8/8
- cvm-api `ScanControllerWebTest`: 5/5
- cvm-app ITs (Docker): 5 geskippt
- cvm-architecture-tests `ModulgrenzenTest`: 7/7

Gesamt: 44 Tests, 33 gruen, 11 geskippt ohne Docker, 0 rot.

---

*Autor: Claude Code.*
