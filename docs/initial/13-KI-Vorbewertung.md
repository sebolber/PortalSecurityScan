# Iteration 13 – KI-Vorbewertung (Cascade-Stufe 3)

**Jira**: CVM-32
**Abhängigkeit**: 11, 12
**Ziel**: Die KI übernimmt die Vorbewertung aller Findings, für die weder REUSE
noch Regel greift. Der Bewerter findet fertige Vorschläge mit Begründung und Quellen
in der Queue.

---

## Kontext
Konzept v0.2 Abschnitt 4.3 (Cascade), 4.4 (Prinzip), 6.1 (Ingestion), Anhang A
(Beispiel) und Anhang C (System-Prompt). Dies ist die zentrale KI-Funktion.

## Scope IN
1. `AiAssessmentSuggester` in `cvm-ai-services/`.
2. Prompt-Template `auto-assessment.v1`:
   - System-Prompt nach Konzept Anhang C (Daten/Anweisungs-Trennung,
     JSON-Schema-Pflicht, konservativer Default).
   - User-Template mit `{cve_json}`, `{profile_json}`, `{component_json}`,
     `{rag_chunks}`.
   - Output-Schema: `{proposedSeverity, proposedRationale, proposedStrategy,
     proposedFixDate, confidence, sources: [{kind,url,excerpt}],
     usedProfileFields: [string]}`.
3. RAG-Query vor LLM-Call:
   - Top-K=5 ähnliche approved Assessments der letzten 180 Tage
     (Mandanten-gefiltert).
   - Zusätzlich Advisory-Chunks zur CVE.
4. LLM-Call via Gateway mit `useCase="AUTO_ASSESSMENT"`, strukturierte Ausgabe.
5. Speicherung:
   - `ai_call_audit` (aus Gateway, bereits da).
   - `ai_suggestion` pro Call (mit FK auf Audit).
   - `ai_source_ref` je Quelle.
   - `assessment` im Status `PROPOSED`, `proposalSource=AI`,
     `proposedByAiSuggestionId=<…>`, `rationaleSourceFields=usedProfileFields`.
6. Integration in Cascade aus Iteration 05: Stufe 3 wird aktiviert, wenn
   `cvm.llm.enabled=true` und Modell-Profil der Umgebung KI zulässt.
7. Halluzinations-Gegencheck:
   - Versionsnummern/Fix-Angaben, die das LLM nennt, werden gegen die
     strukturierten Quellen (NVD/GHSA) rückvalidiert.
   - Abweichung → Vorschlag wird gespeichert, aber mit
     `assessment.status=NEEDS_VERIFICATION` und Markierung im UI.
8. Konservative Default-Regel: wenn keine Profilfelder-Übereinstimmung und
   keine starken RAG-Treffer → Vorschlag = Original-Severity (kein Downgrade).

## Scope NICHT IN
- Copilot (Iteration 14).
- Delta-Summary (Iteration 14).
- Reachability-Analyse (Iteration 15).

## Aufgaben
1. `AutoAssessmentOrchestrator` orchestriert RAG → LLM → Validierung →
   Persistierung.
2. Batch-Verarbeitung (pro Scan): Findings ohne Assessment werden in
   kleinen Wellen (z. B. 10 parallel, Bucket4j-gesteuert) verarbeitet.
3. UI-Integration minimal: Queue aus Iteration 08 zeigt `AI`-Badge,
   Confidence-Wert, Quellenliste (API schon da, UI ergänzt).
4. Konfiguration `cvm.ai.auto-assessment.enabled=false` als Default;
   pro Mandant aktivierbar.

## Test-Schwerpunkte
- `AutoAssessmentOrchestratorTest` mit Mock-LlmClient: Happy-Path,
  Injection-Block, ungültige Ausgabe, Halluzinations-Fall.
- Integrationstest mit Fake-LLM: Scan-Ingest → Cascade → AI-Stage →
  Queue enthält `AI`-Vorschläge mit Source-Refs.
- Konservativer Default: Test bei dünner Faktenlage → Severity unverändert.
- `@DisplayName`: `@DisplayName("AI-Cascade: ohne starke RAG-Treffer bleibt Severity auf Original-Wert")`

## Definition of Done
- [ ] Cascade-Stufe 3 funktional.
- [ ] AI-Vorschläge mit Quellen landen in Queue.
- [ ] Halluzinations-Check blockiert falsche Versionsangaben.
- [ ] Konservativer Default getestet.
- [ ] Coverage `cvm-ai-services/autoassessment` ≥ 90 %.
- [ ] Fortschrittsbericht.
- [ ] Commit: `feat(ai): KI-Vorbewertung als Cascade-Stufe 3 integriert\n\nCVM-32`

## TDD-Hinweis
**Ändere NICHT die Tests** bei Rot – insbesondere nicht die, die den
konservativen Default prüfen. Diese Tests verhindern, dass das System durch
Prompt-Tuning plötzlich CVEs großflächig downgrade-t.

## Abschlussbericht
Standard, plus Auswertung auf einem synthetischen Scan-Datensatz:
„Wie viele der simulierten Findings werden automatisch vorbewertet,
wie viele bleiben für Menschen?"
