# Iteration 19 - NL-Query-Dashboard + Executive-Reports - Plan

**Jira**: CVM-50
**Branch**: `claude/iteration-10-pdf-report-C9sA4`
**Abhaengigkeit**: Iteration 10 (PDF-Report), 14 (Copilot/Summary).

## Architektur

**Teil A - NL-Query**
- `cvm-ai-services/nlquery`:
  - `NlFilterSchema` = fachliche Whitelist der zulaessigen
    Felder (productVersion, environment, severityIn,
    statusIn, minAgeDays, hasUpstreamFix, kevOnly, sortBy).
  - `NlQueryRequest` (nlQuestion + triggeredBy).
  - `NlFilter` record (validierte Felder).
  - `NlFilterValidator`: parsed JSON aus LLM-Antwort, prueft
    jedes Feld gegen Whitelist; unbekanntes Feld -&gt;
    Ablehnung.
  - `NlQueryService`: LLM-Call via `AiCallAuditService`
    (use-case `NL_QUERY`), Validator, dann deterministischer
    Filter-Runner ueber das `AssessmentRepository`.
  - **Keine SQL vom LLM** - Service baut Query via JPA-
    Filter.
- `cvm-api/nlquery`: `POST /api/v1/dashboard/query`.

**Teil B - Executive-Report**
- `cvm-application/report/executive`:
  - `ExecutiveReportData` (Kennzahlen + Delta + offene Punkte).
  - `ExecutiveSummary` Record (5 Bullet Points, Ampel,
    Headline).
  - `ExecutiveReportService.generate(productVersionId,
    environmentId, audience)` - nutzt
    `HardeningReportDataLoader` aus Iteration 10 und
    `ScanDeltaCalculator` aus Iteration 14.
  - Thymeleaf-Templates:
    `cvm/reports/executive/board.html` (1-pager),
    `cvm/reports/executive/audit.html` (ausfuehrlich).
  - LLM-Call `executive-summary.v1` generiert die
    Bullet-Points; Audit via `AiCallAuditService`
    (use-case `EXECUTIVE_SUMMARY`).
- `cvm-api/reports`: Endpoint erweitert um
  `GET /api/v1/reports/executive?audience=board|audit`.

## Sicherheits-Invarianten (durch Tests gehaertet)

1. NL-Query: unbekannte Felder in der LLM-Antwort -&gt;
   Ablehnung, **keine** Query-Ausfuehrung.
2. NL-Query: Prompt-Injection in der Frage wird vom
   `InjectionDetector` (Iter. 11) erkannt, der LLM-Call
   laeuft trotzdem ueber den Validator - Ergebnis wird nur
   akzeptiert, wenn Whitelist passt.
3. Executive-Report: Board-Variante strikt auf 5 Bullet-Points
   &amp; max. 140 Zeichen (Test pruft).
4. Kein Auto-Mail-Versand.

## Tests

1. `NlFilterValidatorTest`: Whitelist, unbekanntes Feld -&gt;
   reject, gueltige Filter gehen durch.
2. `NlQueryServiceTest`: drei Beispielfragen -&gt; erwarteter
   Filter, Prompt-Injection -&gt; keine Query.
3. `NlQueryControllerWebTest`: 200, 422 bei invalid, 400 leer.
4. `ExecutiveReportServiceTest`: board vs audit, 5 Bullets-
   Limit, 140-Zeichen-Limit, Ampel.
5. Goldmaster-Text fuer Board (Markerstrings via PDFBox).

## Scope NICHT IN

- Semantisches Embedding fuer NL-Query (nur LLM-Prompt mit
  Schema-Excerpt).
- Auto-Mail-Versand.
- UI-Eingabezeile (Backend-Vertrag steht).
