# Iteration 11 - LLM-Gateway + Audit - Plan

**Jira**: CVM-30
**Branch**: `claude/iteration-10-pdf-report-C9sA4` (Folgearbeit auf bestehendem Branch)
**Ziel**: Auditierbarer, sicherheitskritischer Zugang zu LLMs.
Kein direkter API-Aufruf aus anderen Modulen.

## Architektur-Grundsatz

CLAUDE.md Abschnitt 3 schreibt vor: `llm-gateway -> domain`. Das
`cvm-llm-gateway` darf also **keine** Persistenz-Abhaengigkeit haben.
Der Prompt-Hinweis "Modul-POM Abhaengigkeit auf cvm-persistence" wird
ueber einen Port/Adapter-Split umgesetzt:

- `cvm-llm-gateway`: Interface `AiCallAuditPort` (plus `AiSuggestionPort`
  bei Bedarf), `LlmClient`, `AiCallAuditService`, Injection-Detector,
  Output-Validator, Adapter gegen Claude/Ollama. Kein Spring Data.
- `cvm-persistence`: Entities `AiCallAudit`, `AiSuggestion`,
  `AiSourceRef` + JPA-Repos + Flyway `V0013__ki_audit.sql` (V0010 ist
  durch Bewertungs-Workflow belegt).
- `cvm-ai-services`: `JpaAiCallAuditAdapter` (bridge).

Damit ist ArchUnit zufrieden und die Sicherheits-Invariante wird nicht
aufgeweicht.

## Scope IN

1. **Basis-Records**
   - `LlmClient` mit `LlmRequest`/`LlmResponse`/`Message`/`TokenUsage`.
   - `LlmDisabledException`.
   - `LlmClientSelector.select(environment, useCase)`.
2. **Adapter**
   - `ClaudeApiClient` ueber Spring `RestClient`. Anthropic
     Messages-API (`/v1/messages`) mit `anthropic-version` Header und
     `tool_use` fuer strukturierte Ausgabe.
   - `OllamaClient` ueber Spring `RestClient` gegen
     `http://ollama:11434/api/chat`.
3. **Prompt-Templates**
   - Classpath `cvm/llm/prompts/*.st` mit leichter `${...}`-
     Substitution (kein Velocity/FreeMarker).
   - Metadaten: `promptTemplateId` + `promptTemplateVersion` werden in
     den Audit-Eintrag uebernommen.
4. **Injection-Detector**
   - Marker-Katalog (min. 10 Heuristiken) inklusive Zero-Width-Chars,
     Base64-Bloecke, Rollenwechsel-Phrasen, HTML-/Markdown-Header-
     Injection.
   - Modus `warn|block` ueber `cvm.llm.injection.mode`.
5. **Output-Validator**
   - Schema-Validierung (Jackson gegen erwartetes JSON-Schema in
     Request).
   - Severity-Enum-Check gegen `AhsSeverity`.
   - "Anweisung-an-den-Nutzer"-Muster in `rationale`.
6. **AiCallAuditService**
   - Schritt 1: `AiCallAuditPort.persistPending(...)` (PENDING).
   - Schritt 2: Injection-Check.
   - Schritt 3: LlmClient-Call (via Selector).
   - Schritt 4: Output-Validator.
   - Schritt 5: `AiCallAuditPort.finalize(...)` (OK/Fehlerstatus).
   - Kein Call ohne persistierten PENDING-Eintrag.
7. **Rate-Limiter** ueber Bucket4j pro Mandant und global.
8. **Kosten-Tracking** ueber `cvm.llm.pricing.<modelId>.<metric>` aus
   `application.yaml`.
9. **Feature-Flag** `cvm.llm.enabled=false` (Default). Wenn aus, wirft
   das Gateway `LlmDisabledException`. Tests mit Mock-Clients.
10. **Persistenz** (cvm-persistence)
    - Flyway `V0013__ki_audit.sql`.
    - Entities + Repos: `AiCallAudit`, `AiSuggestion`, `AiSourceRef`.
11. **JPA-Adapter** (cvm-ai-services)
    - `JpaAiCallAuditAdapter` implementiert `AiCallAuditPort`.

## Scope NICHT IN

- Konkrete KI-Use-Cases (Auto-Assessment, Copilot, Summary) - ab
  Iteration 13.
- RAG/Embeddings - Iteration 12.
- Einsatz in Tests gegen echte Anthropic-API. CI laeuft ausschliesslich
  gegen WireMock + Mocks.

## Tests

1. `LlmRequestRecordTest` - Gueltigkeit der Invarianten (Pflichtfelder).
2. `InjectionDetectorTest` - mind. 10 Marker; Warn-/Block-Modus.
3. `OutputValidatorTest` - Schema-Verletzung; unzulaessige Severity;
   Anweisungs-Muster im Rationale.
4. `AiCallAuditServiceTest` - PENDING->OK, PENDING->INJECTION_RISK,
   PENDING->INVALID_OUTPUT, PENDING->ERROR, Rate-Limit-Fehler.
5. `LlmDisabledTest` - `cvm.llm.enabled=false` wirft
   `LlmDisabledException`.
6. `ClaudeApiClientTest` (WireMock) - Happy-Path, Timeout, 429, 500.
7. `OllamaClientTest` (WireMock) - Happy-Path, Fehler.
8. `LlmClientSelectorTest` - ueber Modell-Profil der Umgebung.
9. `PromptTemplateLoaderTest` - Substitution, fehlende Variable wirft.
10. `CostCalculatorTest` - Preise ueber Configuration.

## Invarianten (werden hart als Tests geprueft)

1. Kein Call ohne PENDING-Audit-Eintrag.
2. Kein Call bei `cvm.llm.enabled=false`.
3. Kein Call, wenn Injection-Detector im Block-Modus anschlaegt.
4. Kein Output-Propagation, wenn Schema verletzt.
5. Rate-Limit pro Mandant wird durchgesetzt.
6. Audit-Eintrag ist write-only (Immutable-Listener - analog
   `AssessmentImmutabilityListener`).

## Stopp-Kriterien

- Keine Invariante darf gelockert werden.
- Keine Secrets im Klartext im Repo.
- Keine CI-Aufrufe gegen echte Anthropic-API.
