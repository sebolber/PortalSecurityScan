# Iteration 17 - KI-Regel-Extraktion - Plan

**Jira**: CVM-42
**Branch**: `claude/iteration-10-pdf-report-C9sA4`
**Abhaengigkeit**: Iteration 05 (Regel-Engine), 13 (KI-Vorbewertung).

## Architektur

- **Domain**: `RuleSuggestionStatus` (PROPOSED/APPROVED/REJECTED/EXPIRED).
  `RuleOrigin.AI_EXTRACTED` ist bereits da.
- **Persistenz** (Flyway `V0018`):
  - `rule_suggestion` (FK auf `ai_suggestion`; Felder `name`,
    `condition_json`, `proposed_severity`, `rationale_template`,
    `cluster_rationale`, Metriken `historical_match_count`,
    `would_have_covered`, `coverage_rate`, `conflict_count`,
    Status, Zeitstempel, Approver-IDs).
  - `rule_suggestion_example` (Link zu beispielhaften
    Assessment-Ids).
  - `rule_suggestion_conflict` (Assessment-Id + Abweichungs-
    Notiz).
  - `rule` bekommt `extracted_from_ai_suggestion_id` (FK).
- **cvm-llm-gateway / prompts**: `rule-extraction.st`.
- **cvm-ai-services/ruleextraction**:
  - `AssessmentClusterer`: Features (severity, sourceFields,
    kev, komponenten-Typ). Clustering deterministisch ueber
    Feature-Vergleich; semantisches RAG-Clustering ist
    optional (offener Punkt).
  - `DryRunEvaluator`: wertet Kandidaten-Condition gegen die
    Assessment-Historie aus - `historicalMatchCount`,
    `wouldHaveCovered`, `coverageRate`, `conflicts`.
  - `RuleExtractionService`: pro Cluster LLM-Call, danach
    Dry-Run, Persistenz von RuleSuggestion + Conflicts.
  - `RuleExtractionJob` (cron `0 30 2 * * *`, Flag +
    scheduler-enabled). 10-Cluster-Cap pro Nacht.
  - `RuleOverrideTracker`: zaehlt Overrides einer aktiven
    extrahierten Regel in den letzten 30 Tagen; ab Schwellwert
    setzt die Regel auf `DRAFT` (Review-Flag).
- **cvm-api/rules**: Slash-Endpunkte unter
  `/api/v1/rules/suggestions` (GET, POST approve, POST reject).

## Sicherheits-/Audit-Invarianten

1. **Kein Auto-Activate**: `RuleSuggestion.approve()` erzeugt einen
   `Rule(status=ACTIVE)` nur nach expliziter Admin-Entscheidung.
2. **Vier-Augen** bei Approve: `approvedBy` muss von
   `suggestedBy`/`proposedBy` abweichen (Test-gehaertet).
3. **Mindestgroessen**: Cluster mit &lt; 5 Assessments oder &lt; 3
   distinkten CVEs werden verworfen.
4. **10-Cluster-Cap** pro Nacht.
5. **Audit-Kette**: `Rule.extractedFromAiSuggestionId -&gt;
   RuleSuggestion -&gt; ai_suggestion -&gt; ai_call_audit`.
6. **Rueckkopplung**: >=4 Overrides in 30 Tagen -> Regel auf
   DRAFT (kein Wegloeschen).

## Tests

1. `AssessmentClustererTest`: Features, Mindestgroesse,
   Deterministik.
2. `DryRunEvaluatorTest`: leere Historie, 0/100 % Treffer,
   Konfliktliste, Coverage-Rate.
3. `RuleExtractionServiceTest` (Fake-LLM): ein Cluster -&gt;
   Suggestion angelegt; Cluster zu klein -&gt; kein
   Vorschlag; LLM-Fehler -&gt; kein Vorschlag.
4. `RuleExtractionJobTest`: Cap wirkt, Rest wird verschoben.
5. `RuleOverrideTrackerTest`: 3 Overrides OK, 4 triggert
   Review.
6. `RuleSuggestionsControllerWebTest`: GET, Approve (Vier-Augen),
   Approve-Same-User 409, Reject.

## Scope NICHT IN

- Modifikation bestehender Regeln per KI.
- Auto-Aktivierung.
- RAG-Embedding-Clustering - Feature-basiertes Clustering reicht
  fuer den ersten produktiven Einsatz (Semantik-Ergaenzung ist
  offener Punkt).
- UI-Tab fuer Vorschlaege (Backend-Vertrag steht; UI-Nachzug
  separat).
