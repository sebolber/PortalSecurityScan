# Iteration 17 – KI-Regel-Extraktion aus Bewertungshistorie

**Jira**: CVM-42
**Abhängigkeit**: 05, 13
**Ziel**: Wiederkehrende Bewertungsmuster werden als deterministische Regeln
extrahiert – das System lernt aus eurer Praxis, ohne die Revisionsfestigkeit
zu verlieren.

---

## Kontext
Konzept v0.2 Abschnitt 6.6 und Anhang B. Die entstehenden Regeln sind
**gewöhnliche JSON-Regeln** wie in Iteration 05 – nur ihre Entstehung ist
KI-unterstützt. Die Regel-Engine selbst bleibt deterministisch.

## Scope IN
1. `RuleExtractionService` in `cvm-ai-services/ruleextraction/`.
2. Nightly-Job `RuleExtractionJob` (täglich 02:30):
   - Liest alle `APPROVED`-Assessments der letzten N Tage (konfigurierbar,
     Default 180).
   - Bildet Pattern-Kandidaten: Assessments, die trotz unterschiedlicher
     CVE dieselbe `ahsSeverity`, ähnliche Rationale und übereinstimmende
     Profilfelder in `rationaleSourceFields` haben.
   - Clustering: semantisch via RAG-Embeddings (aus Iteration 12) +
     deterministische Merkmals-Matrix (KEV-Status, Komponenten-Typ,
     Strategy).
3. LLM-Call `rule-extraction.v1`:
   - Eingabe: Cluster (z. B. 14 Assessments), Beispiel-Begründungen,
     Profilfelder, die gemeinsam vorkamen.
   - Output-Schema:
     ```json
     {
       "proposedRule": {
         "name": "...",
         "condition": { "all": [ ... ] },
         "proposedSeverity": "NOT_APPLICABLE",
         "rationaleTemplate": "..."
       },
       "clusterRationale": "...",
       "exampleAssessmentIds": ["...","..."]
     }
     ```
4. **Dry-Run-Auswertung** (deterministisch):
   - Die vorgeschlagene Regel wird gegen die **komplette Historie** der
     letzten N Tage ausgewertet:
     - `historicalMatchCount` – Assessments, die die Bedingung erfüllen.
     - `wouldHaveCovered` – davon solche, deren tatsächliche Einstufung
       mit der vorgeschlagenen übereinstimmt.
     - `coverageRate = wouldHaveCovered / historicalMatchCount`.
     - `conflicts` – Liste der Assessments, bei denen Regel und
       tatsächliche Einstufung abweichen.
5. Persistenz:
   - `RuleSuggestion`-Entity + Flyway `V0012__regel_extraktion.sql`
     (`rule_suggestion`, `rule_suggestion_conflict`).
   - Statusmaschine: `PROPOSED` → `APPROVED` | `REJECTED` | `EXPIRED`.
   - Aktivierung erzeugt einen regulären `Rule`-Eintrag mit
     `origin=AI_EXTRACTED` und `extractedFromAiSuggestionId` (aus
     Iteration 05).
6. REST:
   - `GET /api/v1/rules/suggestions`
   - `POST /api/v1/rules/suggestions/{id}/approve` – Vier-Augen,
     aktiviert die Regel.
   - `POST /api/v1/rules/suggestions/{id}/reject` – Kommentar pflicht.
7. UI-Ergänzung (Regel-Editor aus Iteration 05, Admin-Bereich):
   - neuer Tab „Vorschläge" mit den extrahierten Regeln.
   - Detailansicht zeigt Cluster, Begründung, Dry-Run-Ergebnis,
     Konfliktliste mit Links auf die Einzelfälle.

## Scope NICHT IN
- Automatische Aktivierung extrahierter Regeln. Aktivierung ist **immer**
  eine manuelle Vier-Augen-Handlung eines CVE-Admins.
- Modifikation bestehender Regeln per KI (Nice-to-have, später).

## Aufgaben
1. Mindestgrößen für Cluster (Default: ≥ 5 Assessments, ≥ 3 verschiedene CVEs).
2. Kostenkontrolle: maximal 10 Cluster pro Nacht, Rest wird protokolliert
   und nächste Nacht drangenommen.
3. Rückkopplung: wenn eine aktivierte Regel innerhalb 30 Tagen mehrfach
   overridden wird, erzeugt das System einen Warnhinweis und setzt die
   Regel auf Review.
4. Audit-Chain: `Rule.extractedFromAiSuggestionId` → `RuleSuggestion` →
   `ai_suggestion` → `ai_call_audit`. Damit vollständige Nachvollziehbarkeit,
   warum eine Regel existiert.

## Test-Schwerpunkte
- `RuleExtractionServiceTest` mit Fake-LLM und kuratierten Assessment-
  Clustern.
- `DryRunEvaluatorTest`: Coverage-Berechnung, Konflikt-Erkennung.
- Integrationstest: 180 synthetische Assessments → Nightly-Job läuft →
  ein Vorschlag erscheint in Queue.
- Rückkopplungs-Test: Regel aktiviert, 4 Overrides in 30 Tagen → Review-
  Flag gesetzt.
- `@DisplayName`: `@DisplayName("Regel-Extraktion: Cluster mit weniger als fuenf Assessments erzeugt keinen Vorschlag")`

## Definition of Done
- [ ] Nightly-Job läuft und erzeugt Vorschläge bei genügend Historie.
- [ ] Dry-Run-Metriken vollständig.
- [ ] Aktivierung nur via Vier-Augen möglich.
- [ ] Rückkopplung bei zu vielen Overrides greift.
- [ ] Coverage `cvm-ai-services/ruleextraction` ≥ 85 %.
- [ ] Fortschrittsbericht.
- [ ] Commit: `feat(ai): Regel-Extraktion aus Bewertungshistorie mit Dry-Run-Auswertung\n\nCVM-42`

## TDD-Hinweis
Die Dry-Run-Auswertung ist der Vertrauensanker. Teste die
Coverage-Berechnung mit vielen Eckfällen (leere Historie, 0 Treffer,
100 % Treffer, 100 % Konflikte) zuerst. **Ändere NICHT die Tests**
bei Rot.

## Abschlussbericht
Standard, plus Liste der im Test erzeugten Regelvorschläge.
