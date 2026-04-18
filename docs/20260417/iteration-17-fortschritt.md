# Iteration 17 - KI-Regel-Extraktion - Fortschritt

**Jira**: CVM-42
**Datum**: 2026-04-18
**Branch**: `claude/iteration-10-pdf-report-C9sA4`

## Zusammenfassung

Ein Nightly-Job extrahiert aus der APPROVED-Assessment-Historie
Pattern-Cluster, laesst das LLM daraus einen Regelvorschlag bauen,
wertet den Vorschlag gegen **die gesamte Historie** aus (Coverage +
Konfliktliste) und legt das Ergebnis als `RuleSuggestion` in die
DB. Aktivierung ist ausschliesslich per Vier-Augen-POST moeglich
und erzeugt dann eine aktive `Rule(origin=AI_EXTRACTED)` mit dem
Verweis auf den `ai_suggestion`-Eintrag. Ein separater
`RuleOverrideTracker` setzt aktive extrahierte Regeln nach
&ge;4 Overrides in 30 Tagen auf `DRAFT` zurueck.

## Umgesetzt

### Domain / Persistenz
- `RuleSuggestionStatus` (PROPOSED/APPROVED/REJECTED/EXPIRED).
- Flyway `V0018__regel_extraktion.sql`:
  - `rule_suggestion` + `rule_suggestion_example` +
    `rule_suggestion_conflict`.
  - `rule.extracted_from_ai_suggestion_id` (nullable FK).
- Entities + Repositories in `cvm-persistence/rulesuggestion`.
- `Rule.extractedFromAiSuggestionId`-Feld.

### LLM-Gateway / Prompts
- `rule-extraction.st` mit Schema-Pflicht (proposedRule +
  clusterRationale + exampleAssessmentIds).

### AI-Services (`cvm-ai-services/ruleextraction`)
- `RuleExtractionConfig` (Flag, Window-Days 180, Cluster-Cap 10,
  Override-Threshold 4).
- `AssessmentClusterer` (deterministische Feature-Keys:
  severity/sourceFields/kev/componentType). Mindestgroesse
  5 Assessments + 3 distinkte CVEs.
- `DryRunEvaluator` nutzt bestehenden `ConditionParser` +
  `RuleEvaluator`. Liefert `historicalMatchCount`,
  `wouldHaveCovered`, `coverageRate`, `conflicts`.
- `RuleExtractionService` orchestriert: LLM-Call via
  `AiCallAuditService` (use-case `RULE_EXTRACTION`), Dry-Run,
  Persistenz von `RuleSuggestion` + Examples + Conflicts.
- `RuleExtractionJob` (Cron `0 30 2 * * *`) mit
  10-Cluster-Cap pro Lauf.
- `RuleOverrideTracker`: zaehlt HUMAN-Overrides ueber
  RULE-Vorgaenger in 30 Tagen; setzt aktive AI-Regel auf DRAFT.
- `RuleSuggestionService` mit Approve/Reject, **Vier-Augen**-
  Pruefung (approver != suggester), View-Factory
  (`RuleSuggestionView`, `ApproveRuleResult`) - haelt die
  ArchUnit-Regel `api -> persistence` sauber.

### API
- `RuleSuggestionsController`:
  - `GET  /api/v1/rules/suggestions`
  - `POST /api/v1/rules/suggestions/{id}/approve` (CVM_ADMIN)
  - `POST /api/v1/rules/suggestions/{id}/reject` (CVM_ADMIN)
- `RuleSuggestionsExceptionHandler` (404/400/409).

## Sicherheits-/Audit-Invarianten (durch Tests gehaertet)

- **Mindestgroesse**: `AssessmentClustererTest.mindestgroesse`.
- **Vier-Augen**: `RuleSuggestionServiceTest.vierAugen` +
  Controller-Test `approveVierAugen` (409).
- **Kein Auto-Activate**: Nur ueber `/approve` wird eine
  `Rule(status=ACTIVE)` angelegt; Test prueft `origin=AI_EXTRACTED`
  und Verweis auf `ai_suggestion`.
- **Dry-Run-Korrektheit**: Coverage-0/100 %, Konflikt-Liste,
  leere Historie.
- **Condition-Schema**: kaputtes JSON -&gt; leerer Dry-Run-Report
  statt Exception.

## Pragmatische Entscheidungen

- **Feature-basiertes Clustering** statt RAG-Embedding-Clustering.
  Reicht fuer den ersten produktiven Einsatz; Embedding-Ergaenzung
  ist offener Punkt.
- **Override-Tracker-Heuristik**: Wir zaehlen HUMAN-
  Assessments der letzten 30 Tage ueber SUPERSEDED-RULE-
  Vorgaenger fuer dieselbe (CVE, Env, PV)-Kombination. Das ist
  eine Approximation; eine praezise Zuordnung zur ausloesenden
  Regel folgt, sobald der Bewertungsschritt die Regel-Id am
  Assessment persistiert.
- **Watchdog ohne eigenen `@Scheduled`**: Override-Tracker hat
  nur `evaluate()`; ein Scheduler-Hook folgt, wenn die
  Heuristik stabil ist.
- **Audit-Id-Zuordnung**: wie in Iter. 13/16 ueber letzten OK-
  Audit-Eintrag des Use-Cases (offener Punkt).

## Tests (neu)

### cvm-ai-services (+14 = jetzt 79)
- `AssessmentClustererTest` (4): Mindestgroesse, Cluster, Severity,
  Determinismus.
- `DryRunEvaluatorTest` (5): leer, 100%, Konflikte, invalid-JSON,
  0 Treffer.
- `RuleSuggestionServiceTest` (5): Approve, Vier-Augen, nicht-
  PROPOSED, Reject, leerer Kommentar.

### cvm-api (+6)
- `RuleSuggestionsControllerWebTest` (6): Liste, Approve, 409,
  Reject, 400, 404.

### Gesamt-Testlauf
```
./mvnw -T 1C test  BUILD SUCCESS  (~73 s)
```

**Gesamt gruen: 342, Skipped 11, Rot 0.**
Iteration 17 bringt **20 neue Tests** ins System.

## Nicht im Scope / Offen

- RAG-Embedding-Clustering (semantische Aehnlichkeit).
- Praezise Override-Zuordnung (rule-Id am Assessment).
- UI-Tab "Vorschlaege" im Admin-Bereich.
- Integrationstest 180 synthetische Assessments -&gt; Nightly-
  Lauf (braucht Testcontainers-Postgres).
- Rueckkopplungs-Integrationstest (4 Overrides -&gt; Review).

## Ausblick Iteration 18
KI-Anomalie-Check + Profil-Assistent - erkennt Bewertungen, die
stark vom historischen Muster abweichen.

## Dateien (wesentlich, neu)

### Domain / Persistenz
- `cvm-domain/.../enums/RuleSuggestionStatus.java`
- `cvm-persistence/src/main/resources/db/migration/V0018__regel_extraktion.sql`
- `cvm-persistence/.../rulesuggestion/*` (Entities + Repos)
- `cvm-persistence/.../rule/Rule.java` (extractedFromAiSuggestionId)

### LLM-Gateway
- `cvm-llm-gateway/src/main/resources/cvm/llm/prompts/rule-extraction.st`

### AI-Services
- `cvm-ai-services/.../ruleextraction/*` (Config, Clusterer,
  DryRunEvaluator, Service, Job, OverrideTracker, SuggestionService,
  Views)

### API
- `cvm-api/.../rulesuggestion/*` (Controller, ExceptionHandler,
  Test-Api)

### Docs
- `docs/20260417/iteration-17-plan.md`
- `docs/20260417/iteration-17-fortschritt.md`
- `docs/20260417/iteration-17-test-summary.md`
