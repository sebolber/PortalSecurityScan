# Iteration 14 - Copilot + KI-Delta-Summary - Fortschritt

**Jira**: CVM-33
**Datum**: 2026-04-18
**Branch**: `claude/iteration-10-pdf-report-C9sA4`

## Zusammenfassung

Inline-Copilot pro Assessment ist live. Vier Use-Cases
(`refine-rationale`, `similar-assessments`, `explain-commit`,
`audit-tone`) erzeugen Vorschlagstext + Quellen, **niemals**
Severity oder Status. NDJSON-Streaming-Endpoint
`POST /api/v1/assessments/{id}/copilot` liefert zwei deterministische
Zeilen (text + sources). Die Delta-Summary fuer Scans nutzt einen
deterministischen Diff (`ScanDeltaCalculator`) und ruft das LLM nur
ueber der Mindestschwelle. Initial-Scans und Diff-arme Re-Scans
erhalten statischen Text ohne LLM-Call. REST-Endpoint
`GET /api/v1/scans/{id}/delta-summary?audience=short|long`.

## Umgesetzt

### LLM-Gateway
- Prompt-Templates `refine-rationale.st`, `similar-assessments.st`,
  `explain-commit.st`, `audit-tone.st`, `delta-summary.st`. Jeder
  Prompt hat Daten/Anweisungs-Trennung und JSON-Schema-Vorgabe;
  Copilot-Templates verbieten Severity-/Status-Aenderungen
  ausdruecklich.

### AI-Services / Copilot
- `CopilotUseCase` (4 Werte mit Template-Bindung).
- `CopilotRequest`, `CopilotSuggestion` (kein Severity-Feld!).
- `CopilotService` orchestriert RAG (nur fuer SIMILAR_ASSESSMENTS) +
  LLM-Call ueber `AiCallAuditService`. Use-Case-Label fuer Audit:
  `COPILOT_<USECASE>`.

### AI-Services / Summary
- `ScanDelta` (neue, entfallene, Severity-Shifts, KEV-Aenderungen).
- `ScanDeltaCalculator` deterministisch via FindingRepository.
- `ScanDeltaSummary` (Read-Model fuer beide Audiences + Diff).
- `ScanDeltaSummaryService`: findet vorherigen Scan derselben
  ProduktVersion+Umgebung, ruft Calculator, faellt auf statischen
  Text wenn Diff &lt; `cvm.ai.summary.min-delta` (Default 1).
  LLM-Call sonst.

### API
- `CopilotController` mit `application/x-ndjson`-Stream
  (StreamingResponseBody). Zwei Zeilen: text + sources.
- `CopilotExceptionHandler` (400 bei IllegalArgumentException).
- `DeltaSummaryController` mit `audience=short|long`.
- `DeltaSummaryExceptionHandler` (404 bei IllegalArgumentException).
- Slice-Konfigs `CopilotTestApi`, `DeltaSummaryTestApi`.

## Pragmatische Entscheidungen

- **NDJSON statt SSE**: Zwei deterministische Zeilen statt eines
  ungesteuerten Token-Streams. Damit ist der API-Vertrag testbar
  ohne Streaming-Test-Infrastruktur (MockMvc + asyncDispatch
  reicht).
- **Stateless Copilot**: keine Konversationshistorie. Pro Request
  alle Felder explizit. Konzept-Vorgabe "fokussierte Aktionen".
- **Kein Severity-Feld in `CopilotSuggestion`**: Compile-Time-
  Garantie fuer "Copilot setzt nie Severity". Ein Test prueft
  zusaetzlich, dass das LLM-JSON ein severity-Feld liefern darf,
  ohne dass es propagiert wird.
- **Delta-Summary ohne rohes SBOM**: Nur strukturierter Diff im
  Prompt. Konzept 6.3.
- **Audit-Use-Case-Label**: `COPILOT_<USECASE>` und
  `DELTA_SUMMARY` halten den Audit-Filter pro Funktion sauber.
- **REST-Reuse alter Slice-Pattern**: Jeder Controller bekommt einen
  eigenen `*TestApi` plus `@WebMvcTest(controllers=...)`-Slice.

## Sicherheits-Invarianten (durch Tests gehaerttet)

- `CopilotServiceTest.severityWirdNiemalsGesetzt`: Service
  ignoriert ein vom LLM eingeschmuggeltes severity-Feld.
- `CopilotServiceTest.refineRationale`: Reflection-Pruefung, dass
  `CopilotSuggestion` kein `severity`/`status`-Feld hat.
- `ScanDeltaSummaryServiceTest.unterMindestSchwelle` /
  `initialRun`: kein LLM-Call wenn Diff unter Schwelle oder
  initial.

## Tests (neu)

### cvm-ai-services (+15 = jetzt 42)
- `CopilotServiceTest` (7).
- `ScanDeltaCalculatorTest` (4).
- `ScanDeltaSummaryServiceTest` (4).

### cvm-api (+5 = jetzt 35)
- `CopilotControllerWebTest` (2): NDJSON-Stream, 400.
- `DeltaSummaryControllerWebTest` (3): short, long, 404.

### Gesamt-Testlauf
```
./mvnw -T 1C test  BUILD SUCCESS  (~74 s)
```

| Modul | Gruen | Skipped | Rot |
|---|---:|---:|---:|
| cvm-domain | 4 | 0 | 0 |
| cvm-persistence | 0 | 6 | 0 |
| cvm-application | 126 | 0 | 0 |
| cvm-integration | 8 | 0 | 0 |
| cvm-llm-gateway | 52 | 0 | 0 |
| cvm-ai-services | **42** | 0 | 0 |
| cvm-api | **35** | 0 | 0 |
| cvm-app | 0 | 5 | 0 |
| cvm-architecture-tests | 8 | 0 | 0 |
| **Gesamt** | **275** | 11 | 0 |

Iteration 14 bringt **20 neue Tests** ins System.

## Nicht im Scope

- Angular-Streaming-UI (Backend-Vertrag definiert).
- Slack-Webhook-Integration (Slack-Snippet wird via REST geliefert,
  Versand bleibt Iteration spaeter).
- Reachability-Aufrufe aus Copilot (Iteration 15).
- Board-Report-PDF (Iteration 19).
- Auto-Trigger der Delta-Summary nach Scan-Ingestion: Service ist
  da, Listener-Verdrahtung an `ScanIngestedEvent` folgt mit der
  Performance-Iteration.

## Offene Punkte

- **Auto-Trigger der DeltaSummary**: Listener auf `ScanIngestedEvent`
  -&gt; `summarize(...)`. Aktuell nur on-demand via REST.
- **Streaming-UI**: Angular-Komponente fehlt; Backend liefert
  NDJSON, das vom Frontend zeilenweise parsed werden muss.
- **Slack-Webhook**: Versand der `short`-Variante an einen Webhook.
- **Cost-Cap pro Umgebung**: Konzept nennt es; aktuell nur globaler
  Rate-Limiter aus Iteration 11. Per-Tenant-Bucket ist da, aber
  Kostenobergrenze waere zusaetzlich sinnvoll.
- **Anwendungsfall "Vorschlag uebernehmen"**: REST-Endpoint, der
  einen Copilot-Text in die Assessment-Begruendung uebernimmt;
  derzeit muss das Frontend selbst PUT auf das Assessment machen.

## Ausblick Iteration 15
Reachability-Agent: statische Codeanalyse, ob der verwundbare
Pfad ueberhaupt vom Anwendungscode erreichbar ist.

## Dateien (wesentlich, neu)

### LLM-Gateway / Prompts
- `cvm-llm-gateway/src/main/resources/cvm/llm/prompts/refine-rationale.st`
- `.../similar-assessments.st`
- `.../explain-commit.st`
- `.../audit-tone.st`
- `.../delta-summary.st`

### AI-Services
- `cvm-ai-services/.../copilot/CopilotUseCase.java`
- `.../copilot/CopilotRequest.java`
- `.../copilot/CopilotSuggestion.java`
- `.../copilot/CopilotService.java`
- `.../summary/ScanDelta.java`
- `.../summary/ScanDeltaCalculator.java`
- `.../summary/ScanDeltaSummary.java`
- `.../summary/ScanDeltaSummaryService.java`
- Test-Klassen entsprechend.

### API
- `cvm-api/.../copilot/CopilotController.java`
- `.../copilot/CopilotExceptionHandler.java`
- `.../summary/DeltaSummaryController.java`
- `.../summary/DeltaSummaryExceptionHandler.java`
- Slice-Configs + WebTests.

### Docs
- `docs/20260417/iteration-14-plan.md`
- `docs/20260417/iteration-14-fortschritt.md`
- `docs/20260417/iteration-14-test-summary.md`
