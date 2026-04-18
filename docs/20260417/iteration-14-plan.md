# Iteration 14 - Copilot + KI-Delta-Summary - Plan

**Jira**: CVM-33
**Branch**: `claude/iteration-10-pdf-report-C9sA4`
**Abhaengigkeit**: Iteration 13 (KI-Vorbewertung), 11 (Gateway), 12 (RAG).

## Architektur

- `cvm-ai-services/copilot`:
  - `CopilotService` mit vier Use-Cases (`refine-rationale`,
    `similar-assessments`, `explain-commit`, `audit-tone`).
  - Stateless: jede Anfrage traegt Assessment-Id + Instruction.
  - Rueckgabe als `CopilotSuggestion` (Text + optionale Source-Refs);
    **niemals direkt Severity-Setzen** (Test-Invariante).
- `cvm-ai-services/summary`:
  - `ScanDeltaSummaryService` mit deterministischer Diff-Berechnung
    (neue/entfallene CVEs, Severity-Shifts, KEV-Aenderungen) und
    LLM-Call ueber den `delta-summary`-Prompt.
  - Mindestschwelle ueber `cvm.ai.summary.minDelta` (Default 1).
  - Initial-Run (kein Vorgaenger-Scan) liefert statischen Text ohne
    LLM-Call.
- REST in `cvm-api`:
  - `POST /api/v1/assessments/{id}/copilot` (Body: useCase,
    userInstruction, attachments). NDJSON-Stream mit einer Antwort-
    Zeile + optional Quellen.
  - `GET /api/v1/scans/{id}/delta-summary?audience=short|long`.
- LLM-Templates: `refine-rationale.st`, `similar-assessments.st`,
  `explain-commit.st`, `audit-tone.st`, `delta-summary.st`.

## Sicherheits-Invarianten

1. Copilot setzt **nie** Severity. UI zeigt Text als Vorschlag, der
   Bewerter entscheidet.
2. Copilot-Calls laufen ueber `AiCallAuditService` (Audit-Pflicht).
3. Delta-Summary fuegt LLM nur Daten-Block bei (kein rohes SBOM).

## Streaming

NDJSON statt SSE - wir liefern eine `application/x-ndjson`-
Streaming-Response mit zwei Zeilen: `{"type":"text","content":"..."}`
und `{"type":"sources","items":[...]}`. Damit ist der API-Vertrag
deterministisch und faerbungstestbar (Tests pruefen Zeilen).

## Tests

1. `CopilotServiceTest` (4 Use-Cases inkl. Severity-Invariante).
2. `CopilotControllerWebTest` (Slice + NDJSON-Validierung + 400 bei
   leerer Instruction).
3. `ScanDeltaCalculatorTest` (deterministischer Diff: neu/entfallen/
   shift, leere Vorlage).
4. `ScanDeltaSummaryServiceTest` (LLM nur ab Mindestschwelle,
   Initial-Run-Text, Audience short vs. long).
5. `DeltaSummaryControllerWebTest` (REST Happy + 404).

## Scope NICHT IN

- Angular-Komponente fuer Streaming-UI (Backend-Vertrag definiert,
  Frontend folgt aus Iteration 15+ pro UI-Iteration).
- SSE-Implementierung (NDJSON ist robuster fuer reine Backend-Tests).
- Reachability-Aufrufe aus Copilot (Iteration 15).
