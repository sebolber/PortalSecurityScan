# Iteration 02 – SBOM-Ingestion (CycloneDX-Parser)

**Jira**: CVM-11
**Abhängigkeit**: 01
**Ziel**: Trivy-/CycloneDX-Reports hochladen, parsen und als `Scan` + `Findings` persistieren.

---

## Kontext
Referenz: Konzept v0.2 Abschnitt 6.1 (Scan-Ingestion). Eingabeformat:
CycloneDX 1.6 JSON (`$schema: "http://cyclonedx.org/schema/bom-1.6.schema.json"`).
Typischer Scan hat ~13 000 Komponenten, ~400 Vulnerabilities.

## Scope IN
1. REST-Endpunkt `POST /api/v1/scans` (Multipart oder Raw-JSON, konfigurierbar).
2. Query-Parameter: `productVersionId`, `environmentId`, `scanner=trivy`.
3. Async-Verarbeitung via Spring `@Async` mit dediziertem Executor
   (`sbom-ingest`, 2–4 Worker, bounded queue).
4. Synchrone Validierung (CycloneDX-Schema, Pflichtfelder),
   `202 Accepted` mit `scanId` und Location-Header auf `GET /api/v1/scans/{id}`.
5. Parser in `cvm-application/scan`:
   - Komponenten nach PURL deduplizieren gegen `component`-Tabelle.
   - `component_occurrence` pro Vorkommen (inkl. FilePath aus
     `aquasecurity:trivy:FilePath`).
   - CVEs in Placeholder-Stubs anlegen (reine ID + Description + originalSeverity).
     Anreicherung erfolgt in Iteration 03.
   - `finding` pro (CVE × ComponentOccurrence × Scan).
6. Rohe SBOM verschlüsselt speichern (Jasypt via Feld `scan.rawSbom` als
   `bytea`, Schlüssel aus `application.yaml`/Vault-Platzhalter).
7. Idempotenz: gleicher Scan (gleiche SBOM-Hash) darf nicht doppelt verarbeitet
   werden. Dedup per `content-sha256`-Spalte, UniqueConstraint auf
   (`productVersionId`, `environmentId`, `contentSha256`).
8. Domain-Event `ScanIngestedEvent` publiziert (Spring ApplicationEventPublisher).
   Kein Listener in dieser Iteration, aber Event dokumentiert.

## Scope NICHT IN
- CVE-Details aus NVD laden (Iteration 03).
- Cascade-Logik / Regel-Auswertung (Iterationen 05, 06).
- UI (Iteration 08).

## Aufgaben
1. Library `org.cyclonedx:cyclonedx-core-java` einbinden.
2. DTO `ScanUploadRequest`, `ScanUploadResponse`, `ScanSummary`.
3. `ScanIngestService` mit klarer Trennung
   `parse → validate → normalize → persist → event`.
4. Testdaten: mindestens zwei CycloneDX-Beispiel-JSONs als Testressource,
   je einmal klein (5 Komponenten, 2 CVEs) und einmal realistisch
   (Auszug aus dem vorliegenden Trivy-Report, auf 100 Komponenten /
   30 Vulns gekürzt).
5. Fehlerklassifizierung: `SbomParseException`, `SbomSchemaException`,
   `ScanAlreadyIngestedException` → je eigener HTTP-Statuscode.

## Test-Schwerpunkte
- `ScanIngestServiceTest`: Happy-Path, Dedup, Fehlerfälle.
- `ScanControllerWebTest` (MockMvc): Multipart-Upload, `202`,
  `409 Conflict` bei Duplikat, `400 Bad Request` bei invalidem Schema.
- Integrationstest: komplettes CycloneDX wird verarbeitet, DB-Zustand
  geprüft (Anzahl Components, Occurrences, Findings).
- Performance-Smoke: 10 000 Komponenten in < 15 s auf Testcontainers-Setup
  (ein Benchmark-Test mit `@Tag("perf")`, läuft nicht per Default).

## Definition of Done
- [ ] Endpunkt dokumentiert in OpenAPI.
- [ ] Async-Konfiguration getestet (kein Re-entrance, kein Leak).
- [ ] Dedup über `contentSha256` funktioniert.
- [ ] Rohe SBOM verschlüsselt in DB.
- [ ] Coverage `cvm-application/scan` ≥ 85 %.
- [ ] Fortschrittsbericht.
- [ ] Commit: `feat(scan): CycloneDX-Ingestion mit Dedup und Async-Verarbeitung\n\nCVM-11`

## TDD-Hinweis
Starte mit dem `ScanControllerWebTest` (rot), dann Service-Test, dann Parser.
Die Reihenfolge Outside-In bewahrt die API-Form.

## Abschlussbericht
Standard gemäß `CLAUDE.md`.
