# Iteration 13 - KI-Vorbewertung (Cascade-Stufe 3) - Plan

**Jira**: CVM-32
**Branch**: `claude/iteration-10-pdf-report-C9sA4` (laufender Strang).
**Abhaengigkeit**: Iteration 11 (LLM-Gateway), Iteration 12 (RAG).
**Ziel**: KI uebernimmt die Vorbewertung von Findings, fuer die
weder REUSE noch Regel greift. Vorschlaege sind im Status PROPOSED
mit Quellen-Refs.

## Architektur

CLAUDE.md-Modulgrenzen verbieten `application -> llm-gateway`. Der
`CascadeService` darf den AI-Pfad also nicht direkt kennen. Wir
loesen das ueber einen Port:

- `cvm-application/cascade`:
  - Neues Interface `AiAssessmentSuggesterPort` (optional injizierbar).
  - `CascadeOutcome.ai(...)` Factory + Felder fuer
    `aiSuggestionId`, Quellen, Confidence.
  - `CascadeService` ruft den Port als Stufe 3, **wenn**
    Bean da und der Use-Case aktiviert.
- `cvm-ai-services/autoassessment`:
  - `AutoAssessmentOrchestrator` implementiert den Port.
  - Reihenfolge: RAG-Query (top-K Assessments + Advisory-Chunks)
    -&gt; LLM-Call ueber `AiCallAuditService` -&gt; Halluzinations-
    Check -&gt; konservativer Default -&gt; Persistenz von
    `AiSuggestion` + `AiSourceRef`.
- `cvm-llm-gateway`:
  - Neues Prompt-Template `auto-assessment.v1.st` mit
    Daten/Anweisungs-Trennung (`<data>...</data>`).
  - Output-Schema:
    `{ severity, rationale, confidence, usedProfileFields,
       sources:[{kind,ref,excerpt}], proposedFixVersion }`.

## Status- und Persistenz-Erweiterungen

- `AssessmentStatus.NEEDS_VERIFICATION` (neu): wird gesetzt, wenn die
  Halluzinations-Pruefung anschlaegt (z.B. Versionsangabe nicht im
  Advisory belegt). UI zeigt entsprechendes Badge.
- `Assessment.aiSuggestionId` ist seit Iteration 01 vorhanden;
  Cascade befuellt ihn nun realer.
- `AiSuggestion`/`AiSourceRef` aus Iteration 11 werden persistiert.

## Konservative Default-Regel

Wenn (a) keine Profilfelder uebereinstimmen UND (b) kein RAG-Treffer
mit Score &gt;= 0.6 vorhanden, faellt der Vorschlag auf die
**Original-Severity** zurueck (CVSS-Mapping aus Iteration 03). Kein
Downgrade. Test verhindert spaeteres Aufweichen durch
Prompt-Tuning.

## Halluzinations-Check

Wenn das LLM `proposedFixVersion` setzt und das Advisory eine
abweichende `fixedInVersion` enthaelt, bekommt der Vorschlag den
Status `NEEDS_VERIFICATION`. Audit-Eintrag im Gateway bleibt OK
(Output ist schema-konform), aber der Cascade-Outcome trampoliniert
das Verifikations-Flag.

## Feature-Flags

- `cvm.llm.enabled=true` notwendig (Gateway-Flag).
- `cvm.ai.auto-assessment.enabled=false` Default; pro Mandant
  einschaltbar.

## Tests

1. `AutoAssessmentOrchestratorTest`:
   - Happy-Path mit Fake-LLM und FakeEmbeddingClient.
   - Halluzinations-Fall (LLM erfindet Fix-Version).
   - Konservativer Default (duenne Faktenlage).
   - Injection-Risk im Input - Orchestrator schiebt das ans
     Gateway weiter.
   - Ungueltige Schema-Antwort - Cascade faellt auf MANUAL zurueck.
2. `CascadeServiceAiTest`: mit injiziertem Port-Stub bekommt
   Cascade Stufe 3, ohne Port bleibt es bei MANUAL.
3. `AssessmentStateMachineTest`: ergaenzt Uebergaenge fuer
   NEEDS_VERIFICATION.

## Stopp-Kriterien

- Halluzinations-Check darf nicht abgeschaltet werden.
- Konservativer Default darf nicht weichen.
- Kein Auto-APPROVED. Vorschlag bleibt PROPOSED bzw.
  NEEDS_VERIFICATION.

## Scope NICHT IN

- UI-Anpassungen (Queue zeigt schon Severity + Source, AI-Badge
  folgt mit Iteration 14).
- Reachability (Iteration 15).
- Delta-Summary (Iteration 14).
