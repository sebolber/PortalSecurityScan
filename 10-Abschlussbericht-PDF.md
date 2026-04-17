# Iteration 10 – Abschlussbericht PDF

**Jira**: CVM-19
**Abhängigkeit**: 06
**Ziel**: Ersatz für die heutige manuell gepflegte Wiki-Seite – automatisch generierter
PDF-Abschlussbericht als Entscheidungsgrundlage für den Weiterbetrieb.

---

## Kontext
Konzept v0.2 Abschnitt 10.1. Vorbild: die vorliegende Wiki-Seite „Analyse
Schwachstellen & Härtung 2025/02". Corporate Design adesso health solutions
(Header/Footer, Logo-Platzhalter, Typografie).

## Scope IN
1. `ReportGeneratorService.generateHardeningReport(productVersionId, environmentId)`
   → `GeneratedReport` mit PDF-Bytes, Metadaten, Audit-Eintrag.
2. Thymeleaf-Templates in `cvm-application/report/templates/`:
   - `hardening-report.html` (Hauptdokument)
   - Sub-Fragmente: `header`, `kennzahlen`, `cve-liste`, `offene-punkte`,
     `anhang-profil`, `anhang-vex` (Platzhalter)
3. Rendering via openhtmltopdf, Schriftarten als TTF-Resourcen mitgeliefert.
4. Report-Inhalt:
   - Kopf (Produkt, Version, Commit, Umgebung, Stichtag, Bewerter,
     Freigeber, Stichtag der Profilversion)
   - **Gesamteinstufung** als vorausgewählte Ampel, vom Freigeber manuell
     bestätigt oder geändert (über Checkbox-Endpunkt vor PDF-Erzeugung)
   - Kennzahlen-Tabelle (Plattform/Docker/Java/NodeJS/Python,
     Spalten Severity)
   - CVE-Liste mit Spalten wie Wiki (CVE-Link, Original-Severity,
     ahs-Einstufung, geplante Behebung, Hinweise). Gruppierung nach Kategorie.
   - Offene kritische Punkte (nicht freigegebene / nicht bewertete Findings)
   - Anhang: Kontextprofil-Snapshot, Liste verwendeter Regeln,
     (Platzhalter für VEX)
5. REST:
   - `POST /api/v1/reports/hardening` mit Body
     `{productVersionId, environmentId, gesamteinstufung, freigeberKommentar}`
     → Async-Generierung, Rückgabe `reportId`.
   - `GET /api/v1/reports/{reportId}` → PDF-Download.
6. Archivierung: generierter Report persistiert (verschlüsselt), nicht
   überschreibbar (Immutable).

## Scope NICHT IN
- Executive-/Board-Variante (Iteration 19).
- VEX-Export (Iteration 20).
- KI-Delta-Text (Iteration 14).

## Aufgaben
1. PDF-Engine Setup mit openhtmltopdf, Font-Embedding (z. B. Inter/Roboto
   oder adesso-CI-Schrift als Platzhalter).
2. Template-Rendering deterministisch (gleiche Eingaben → byte-identisches
   PDF, wichtig für Audit). Keine Zeitstempel im PDF-Header ausser
   `creationDate`.
3. Signature-Block: Fußzeile mit Bewerter und Freigeber, Hash des PDFs als
   Anhang-Eintrag im Audit-Log.
4. Branding über CSS-Variablen (später austauschbar pro Mandant).

## Test-Schwerpunkte
- `ReportGeneratorTest`: kompletter Report generiert, PDF nicht leer,
  enthält erwartete Abschnitte (Text-Extraktion via Apache PDFBox prüft
  Marker-Strings).
- Goldmaster-Test: Referenz-PDF in `src/test/resources/reports/hardening-golden.pdf`,
  Vergleich modulo `creationDate`.
- Determinismus-Test: gleiche Eingabe zweimal, identische Bytes (nach
  Normalisierung).
- `@DisplayName`: `@DisplayName("Report: Kennzahlentabelle zeigt Severity-Counts pro Komponentenkategorie")`

## Definition of Done
- [ ] PDF generierbar, archivierbar, auditiert.
- [ ] Layout nahe an adesso-CI (Platzhalter-Logo akzeptiert).
- [ ] Determinismus-Test grün.
- [ ] Coverage `cvm-application/report` ≥ 80 %.
- [ ] Fortschrittsbericht.
- [ ] Commit: `feat(report): PDF-Abschlussbericht mit Gesamteinstufung und Audit\n\nCVM-19`

## TDD-Hinweis
Goldmaster zuerst anlegen (leer akzeptieren, dann iterativ Abschnitte
ergänzen, Goldmaster aktualisieren). Bei Rot Produktionscode fixen,
**nicht das Goldmaster unreflektiert überschreiben** – wenn der Unterschied
fachlich gewollt ist, aktualisiere mit Commit-Begründung.

## Abschlussbericht
Standard, plus PDF-Beispielausgabe unter `docs/YYYYMMDD/iteration-10-beispiel.pdf`.
