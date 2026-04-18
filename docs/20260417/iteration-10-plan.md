# Iteration 10 - PDF-Abschlussbericht - Arbeitsplan

**Jira**: CVM-19
**Branch**: `claude/iteration-10-pdf-report-C9sA4`
**Abhaengigkeit**: Iteration 06 (Bewertungs-Workflow), 04 (Kontextprofil),
05 (Regel-Engine).

## Zielbild
`ReportGeneratorService.generateHardeningReport(input)` liefert einen
persistierten, immutablen PDF-Abschlussbericht fuer eine
(`productVersionId`, `environmentId`)-Kombination. Inhalte: Kopf
(Produkt/Version/Umgebung/Stichtag/Bewerter), Gesamteinstufung,
Kennzahlen-Tabelle, CVE-Liste, offene kritische Punkte, Anhang
(Profil-Snapshot, Regel-Liste). Rendering via Thymeleaf +
openhtmltopdf (deterministisch). Archiv: `generated_report`-Tabelle,
nicht ueberschreibbar.

## Architektur
- `cvm-persistence`:
  - neue Entity `GeneratedReport` (Immutable: Update wirft).
  - Repository `GeneratedReportRepository`.
  - Flyway `V0012__generated_report.sql`.
- `cvm-application/report`:
  - Record `HardeningReportInput` (productVersionId, environmentId,
    gesamteinstufung, freigeberKommentar, erzeugtVon, stichtag).
  - Record `HardeningReportData` (kopf, kennzahlen, cveListe,
    offenePunkte, anhang). Vollstaendiges Read-Model, fuer das Template.
  - `HardeningReportDataLoader`: baut `HardeningReportData` aus den
    Repositories (Assessments, ProductVersion, Environment, Profil,
    Rules).
  - `HardeningReportTemplateRenderer`: rendert Thymeleaf-Template
    `cvm/reports/hardening-report.html` zu HTML-String.
  - `HardeningReportPdfRenderer`: wandelt HTML ueber openhtmltopdf in
    PDF-Bytes.
  - `ReportGeneratorService.generateHardeningReport(...)`: orchestriert
    Loader -> Renderer -> Persistenz, liefert `GeneratedReportView`
    (Read-Model mit `reportId`, `sha256`, `pdfBytes`). Deterministisch
    (fixer Producer, fixe Creation-Date aus `Clock`, normalisierte
    Document-ID).
  - `ReportConfig`: Thymeleaf `TemplateEngine`-Bean + `Clock`-Bean
    (ConditionalOnMissingBean).
- `cvm-api/reports`:
  - `ReportsController`:
    - `POST /api/v1/reports/hardening` -> erzeugt, liefert 201 mit
      `{ reportId, sha256 }`.
    - `GET /api/v1/reports/{reportId}` -> streamt PDF (`application/pdf`).
  - DTO-Records.

## Determinismus-Strategie
- Clock-basierte CreationDate (Test injiziert `Clock.fixed(...)`).
- `PdfRendererBuilder.withProducer("CVM Report 1.0")`.
- `PDDocument.setDocumentId(...)` auf fixen Hash ueber Input.
- Test `ReportDeterminismTest`: zwei Aufrufe mit gleichem Input ->
  byte-gleich. Falls minimaler Jitter durch PDFBox: Normalisierung
  (Strip `/ID`, `/CreationDate`, `/ModDate` via Regex) vor Vergleich.

## Content Sicherheit / Audit
- `pdfBytes` in BYTEA, `sha256` als CHAR(64).
- Keine `@PreUpdate`-Aktualisierung erlaubt: `ReportImmutabilityListener`
  wirft bei Update (analog Assessment).
- Auditeintrag derzeit als Log + Tabellenzeile in `generated_report`;
  separate `audit_trail`-Tabelle folgt spaeter.

## Tests (TDD)
1. **Unit** `HardeningReportTemplateRendererTest` (application):
   - Template rendert Kopf, Kennzahlen, CVE-Liste, Anhang.
   - HTML enthaelt erwartete Marker-Strings.
2. **Unit** `HardeningReportPdfRendererTest` (application):
   - PDF beginnt mit `%PDF-1.7`.
   - PDF nicht leer, parse-bar via Apache PDFBox.
   - Text-Extraktion enthaelt Marker.
3. **Unit** `ReportGeneratorServiceTest` (application):
   - Mit Mock-Loader und fake Clock wird ein Report erzeugt,
     persistiert, `sha256` stimmt mit tatsaechlichem Hash ueberein.
4. **Determinismus** `ReportDeterminismTest`: zwei Aufrufe mit gleicher
   Fake-Data -> byte-gleich (nach Normalisierung).
5. **Slice** `ReportsControllerWebTest` (api):
   - `POST /api/v1/reports/hardening` liefert 201 mit `reportId`.
   - `GET /api/v1/reports/{id}` liefert 200 mit `application/pdf`.
   - 404 bei unbekannter `reportId`.

## Scope NICHT enthalten
- Executive/Board-Variante (Iteration 19).
- KI-Delta-Text (Iteration 14).
- VEX-Anhang (Iteration 20).
- Async-Job-Queue: Generierung ist synchron (sub-Sekunde fuer
  realistische Datenmengen).
- Encryption-at-rest der PDF-Bytes: Bleibt offener Punkt
  (Jasypt-Binding folgt, sobald Vault-Key verfuegbar).

## Stopp-/Grenzfaelle
- ProduktVersion oder Umgebung unbekannt -> `IllegalArgumentException`
  mit DE-Text.
- Kein Assessment im Scope -> Report wird trotzdem erzeugt, Tabelle
  leer, Gesamteinstufung ist Eingabe.
