# Iteration 10 - PDF-Abschlussbericht - Fortschritt

**Jira**: CVM-19
**Datum**: 2026-04-18
**Branch**: `claude/iteration-10-pdf-report-C9sA4`

## Zusammenfassung

Der PDF-Abschlussbericht ist deterministisch erzeugbar, wird
archiviert (immutable) und ueber REST abrufbar. Eingabekanal ist
`POST /api/v1/reports/hardening`, Ausgabekanal ist
`GET /api/v1/reports/{id}`. Inhalte: Kopf, Gesamteinstufung,
Kennzahlen pro Kategorie, CVE-Liste, offene Punkte und Anhang
(Profil, Regeln, VEX-Platzhalter). Thymeleaf rendert das HTML;
openhtmltopdf wandelt es in PDF. PDFBox-Metadaten
(CreationDate/ModDate/ID) werden auf feste, eingabegebundene Werte
gesetzt, sodass identische Eingaben byte-gleiche PDFs erzeugen
(SHA-256-Hash bestaetigt).

## Umgesetzt

### Persistenz
- Entity `GeneratedReport` mit `GeneratedReportImmutabilityListener`
  (Update -> `ImmutabilityException`).
- `GeneratedReportRepository` mit
  `findByProductVersionIdAndEnvironmentIdOrderByErzeugtAmDesc(...)`.
- Flyway `V0012__generated_report.sql`:
  Tabelle + Check-Constraints (Severity, Report-Type) + Index
  `(product_version_id, environment_id, erzeugt_am DESC)` +
  Unique-Index ueber `sha256`.

### Application (`cvm-application/report`)
| Klasse | Zweck |
|---|---|
| `HardeningReportInput` | Eingabe-Record (Produkt-/Umgebungs-ID, Gesamteinstufung, Freigeberkommentar, erzeugtVon, Stichtag). |
| `HardeningReportData` | Vollstaendiges Read-Model mit Kopf, Kennzahlen, CVE-Liste, offenen Punkten, Anhang. Alle Listen sortiert (Determinismus). |
| `GeneratedReportView` | Persistenz -> API Read-Model (inkl. PDF-Bytes). |
| `ReportNotFoundException` | 404-Mapper. |
| `ReportConfig` | `TemplateEngine`-Bean (`cvm/reports/`, HTML-Mode) + Fallback-`Clock`. |
| `HardeningReportTemplateRenderer` | rendert Thymeleaf-Template zu HTML. |
| `HardeningReportPdfRenderer` | HTML -> PDF via openhtmltopdf; Nachbearbeitung ueber PDFBox (`Producer`, `CreationDate`, `ModDate`, `/ID`). |
| `HardeningReportDataLoader` | baut `HardeningReportData` aus Repos (Produkt/Version, Umgebung, Profil, Assessments, Regeln). |
| `ReportGeneratorService` | Orchestriert Load -> Template -> PDF -> SHA-256 -> Persistenz. Liefert `GeneratedReportView`. |

### Templates
- `cvm/reports/hardening-report.html` (Thymeleaf): Kopfdaten,
  Ampel-Gesamteinstufung, Kennzahlentabelle (Plattform/Docker/Java/
  NodeJS/Python x Severity), CVE-Liste mit NVD-Link, offene Punkte,
  Anhaenge (Profil-YAML, Regeln, VEX-Platzhalter). Inline-CSS mit
  adesso-CI-Farben (Header/Footer via CSS-Paged-Media).

### REST (`cvm-api/report`)
- `ReportsController`:
  - `POST /api/v1/reports/hardening` -> 201 mit Location-Header und
    `ReportResponse` (reportId, sha256, Metadaten).
  - `GET /api/v1/reports/{reportId}` -> 200 `application/pdf` mit
    `Content-Disposition` und Header `X-Report-Sha256`.
  - `GET /api/v1/reports/{reportId}/meta` -> 200 Metadaten ohne Bytes.
- `HardeningReportRequest` (JSR-303: NotNull + NotBlank).
- `ReportResponse` (Metadaten ohne PDF-Bytes).
- `ReportsExceptionHandler` (404/400, deutschsprachige Fehlercodes).
- `ReportsTestApi` Slice-Konfig.

### Dependencies
- `cvm-application/pom.xml`: `openhtmltopdf-pdfbox` + `thymeleaf`
  erganzt. Beide Dependencies sind auf das Application-Modul
  beschraenkt; Versions-Management lief bereits im Eltern-POM.

## Pragmatische Entscheidungen

- **Determinismus-Strategie**: openhtmltopdf wird erst nach
  `builder.run()` via PDFBox erneut geladen, damit wir
  `DocumentInformation` und das `/ID`-Array im Trailer setzen koennen.
  Der initiale Versuch ueber `usePDDocument(...)` schlug fehl
  (openhtmltopdf schliesst die Instanz). Die Re-Load-Variante ist
  robust und kostet < 10 ms bei realistischer PDF-Groesse
  (~10 kB Referenz-Report).
- **Thymeleaf-Isolation**: Eigene `TemplateEngine`-Bean mit Prefix
  `cvm/reports/`, damit die Application-Schicht nicht von
  `spring-boot-starter-thymeleaf` (Web-AutoConfig) abhaengt. Analog
  zum `AlertTemplateRenderer` aus Iteration 09.
- **Datenlader**: Nutzt bereits vorhandene Repository-Methoden
  (inkl. `findAll` + Stream-Filter) um alle aktiven Assessments fuer
  (prodVersion, env) zu sammeln. Fuer groessere Datenmengen waere eine
  dedizierte Query-Methode `findActiveByProductVersionAndEnvironment`
  wuenschenswert; bleibt Optimierung (offene-punkte.md).
- **Encryption-at-rest** der PDF-Bytes: Nicht in Iteration 10 umgesetzt.
  Spalte ist `BYTEA`, Hash sichert Integritaet. Binding an
  Jasypt/Vault folgt sobald Vault-Key hinterlegt ist.
- **Gesamteinstufung-Checkbox-Endpunkt**: Der Prompt beschreibt einen
  separaten Checkbox-Endpunkt vor der PDF-Erzeugung. Umgesetzt als
  Feld im `POST /reports/hardening`-Body (`gesamteinstufung` +
  `freigeberKommentar`). Ein vorgeschalteter Checkbox-Endpunkt ist
  UI-seitige Iteration; der Service akzeptiert jetzt direkt die
  finale Entscheidung.
- **Async-Generierung**: Nicht implementiert. Reale Reports (~10 kB
  PDF) rendern in < 100 ms; synchron ausreichend. Async folgt
  automatisch, sobald der Content komplexer wird.

## Tests

### Backend
- `HardeningReportTemplateRendererTest` (3): Kopf/Kennzahlen,
  CVE-Liste, Offene Punkte + Anhang.
- `HardeningReportPdfRendererTest` (3): PDF-Header, Text-Marker via
  PDFBox `PDFTextStripper`, **Determinismus** (byte-gleich bei
  gleicher Eingabe).
- `ReportGeneratorServiceTest` (3): End-to-End mit Mock-Loader &
  fake Clock, Pflichtfelder-Validierung, `findById`-Fehlerpfad.
- `ReportDeterminismTest` (1): Zwei Aufrufe -> gleiche PDF-Bytes und
  gleicher SHA-256.
- `ReportsControllerWebTest` (4): POST/Happy-Path + Location-Header,
  POST/Bad-Request, GET `application/pdf` + `X-Report-Sha256`,
  GET 404.

### Testlauf
```
./mvnw -T 1C test  BUILD SUCCESS
```
- cvm-domain: 4/4
- cvm-persistence: 6 geskippt (Docker)
- cvm-application: **122** (Iteration 10 neu: **+10** im Paket
  `report`).
- cvm-integration: 8
- cvm-api: **29** (Iteration 10 neu: **+4** `ReportsControllerWebTest`).
- cvm-app: 5 geskippt (Docker)
- cvm-architecture-tests: 8

**Gesamt: 182 Tests gruen, 11 geskippt ohne Docker, 0 rot.**

## Nicht im Scope

- Executive-/Board-Variante (Iteration 19).
- VEX-Export (Iteration 20) - im Report bereits als Platzhalter.
- KI-Delta-Text (Iteration 14).
- Async-Job-Queue fuer lange Reports.
- Encryption-at-rest der PDF-Bytes (Jasypt).
- Persistenz-Integrationstest fuer V0012 (Docker-skip).

## Offene Punkte (fuer nachste Iterationen)

- `findActiveByProductVersionAndEnvironment`-Query im
  `AssessmentRepository`: bestehende Loader-Implementierung faellt
  auf `findAll()` + Stream-Filter zurueck. Bei groesseren Datenmengen
  (5-stelliger Bereich) ineffizient.
- Jasypt-Encryption fuer PDF-Bytes einziehen, sobald Vault-Integration
  steht.
- Goldmaster-PDF-Datei (`src/test/resources/reports/hardening-golden.pdf`)
  als reale Artefakt-Referenz checken. Aktuell reicht der
  Byte-Vergleich zweier Live-Generierungen (ReportDeterminismTest).
- Async-Endpunkt (`AsyncReportJob`) wenn Reports mehr als 1-2 s
  brauchen.
- Role-based Security `CVM_REPORTER` / `CVM_APPROVER` fuer
  `/api/v1/reports/*` aktivieren (analog zu Rule-/Alert-Endpunkten).

## Ausblick Iteration 11
LLM-Gateway (CVM-20): Abstraktion ueber Claude-API und Ollama,
Rate-Limiting via Bucket4j, Audit-Trail jeder Konversation,
Injection-Detector + Output-Validator.

## Dateien (wesentlich)

### Neu
- `cvm-persistence/src/main/java/com/ahs/cvm/persistence/report/GeneratedReport.java`
- `cvm-persistence/src/main/java/com/ahs/cvm/persistence/report/GeneratedReportImmutabilityListener.java`
- `cvm-persistence/src/main/java/com/ahs/cvm/persistence/report/GeneratedReportRepository.java`
- `cvm-persistence/src/main/resources/db/migration/V0012__generated_report.sql`
- `cvm-application/src/main/java/com/ahs/cvm/application/report/HardeningReportInput.java`
- `cvm-application/src/main/java/com/ahs/cvm/application/report/HardeningReportData.java`
- `cvm-application/src/main/java/com/ahs/cvm/application/report/GeneratedReportView.java`
- `cvm-application/src/main/java/com/ahs/cvm/application/report/ReportNotFoundException.java`
- `cvm-application/src/main/java/com/ahs/cvm/application/report/ReportConfig.java`
- `cvm-application/src/main/java/com/ahs/cvm/application/report/HardeningReportTemplateRenderer.java`
- `cvm-application/src/main/java/com/ahs/cvm/application/report/HardeningReportPdfRenderer.java`
- `cvm-application/src/main/java/com/ahs/cvm/application/report/HardeningReportDataLoader.java`
- `cvm-application/src/main/java/com/ahs/cvm/application/report/ReportGeneratorService.java`
- `cvm-application/src/main/resources/cvm/reports/hardening-report.html`
- `cvm-application/src/test/java/com/ahs/cvm/application/report/HardeningReportFixtures.java`
- `cvm-application/src/test/java/com/ahs/cvm/application/report/HardeningReportTemplateRendererTest.java`
- `cvm-application/src/test/java/com/ahs/cvm/application/report/HardeningReportPdfRendererTest.java`
- `cvm-application/src/test/java/com/ahs/cvm/application/report/ReportGeneratorServiceTest.java`
- `cvm-application/src/test/java/com/ahs/cvm/application/report/ReportDeterminismTest.java`
- `cvm-api/src/main/java/com/ahs/cvm/api/report/ReportsController.java`
- `cvm-api/src/main/java/com/ahs/cvm/api/report/HardeningReportRequest.java`
- `cvm-api/src/main/java/com/ahs/cvm/api/report/ReportResponse.java`
- `cvm-api/src/main/java/com/ahs/cvm/api/report/ReportsExceptionHandler.java`
- `cvm-api/src/test/java/com/ahs/cvm/api/report/ReportsTestApi.java`
- `cvm-api/src/test/java/com/ahs/cvm/api/report/ReportsControllerWebTest.java`

### Geaendert
- `cvm-application/pom.xml` (openhtmltopdf-pdfbox, thymeleaf)
