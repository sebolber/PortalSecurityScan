# Iteration 13 - KI-Vorbewertung (Cascade-Stufe 3) - Fortschritt

**Jira**: CVM-32
**Datum**: 2026-04-18
**Branch**: `claude/iteration-10-pdf-report-C9sA4`

## Zusammenfassung

Die Cascade hat jetzt eine echte KI-Stufe 3. Wenn weder REUSE noch
Regel greift und das Feature-Flag `cvm.ai.auto-assessment.enabled=true`
gesetzt ist, fragt der `AutoAssessmentOrchestrator` (Modul
`cvm-ai-services`) das LLM-Gateway: RAG-Top-K wird abgeholt,
Prompt gerendert, Audit-Service ruft den `LlmClient`,
`OutputValidator` validiert das Schema, ein **konservativer Default**
verhindert Downgrades bei duenner Faktenlage, ein
**Halluzinations-Check** vergleicht eine vom Modell genannte
`proposedFixVersion` gegen die Advisory und triggert
`AssessmentStatus.NEEDS_VERIFICATION` wenn die Angabe nicht
belegbar ist. Der Vorschlag landet als `ai_suggestion`
(plus `ai_source_ref`) in der DB und durch den `CascadeOutcome` als
`Assessment(PROPOSED|NEEDS_VERIFICATION, source=AI_SUGGESTION,
aiSuggestionId=...)` in der Bewertungs-Queue.

Architektur-konform: `cvm-application` kennt nur das
`AiAssessmentSuggesterPort`-Interface; die Implementierung lebt in
`cvm-ai-services`. Modulgrenzen unverletzt.

## Umgesetzt

### Domain
- `AssessmentStatus.NEEDS_VERIFICATION` (neu).

### Persistenz
- Flyway `V0015__assessment_needs_verification.sql`: erweitert den
  Status-Check + Queue-Index um den neuen Status.
- `AssessmentRepository.findeOffeneQueue` zeigt
  NEEDS_VERIFICATION ebenfalls.

### Application
- `AiAssessmentSuggesterPort` (Interface, optional injizierbar).
- `CascadeOutcome` um `aiSuggestionId`, `confidence`, `targetStatus`
  erweitert; Factory `CascadeOutcome.ai(...)`.
- `CascadeService` ruft Port als Stufe&nbsp;3, faengt alle Fehler ab
  (faellt auf MANUAL).
- `AssessmentStateMachine` ergaenzt um Uebergaenge aus
  `NEEDS_VERIFICATION`.
- `AssessmentWriteService.ProposeCommand` um `aiSuggestionId` und
  `targetStatus` erweitert; backwards-kompatibler Konstruktor fuer
  Iterationen 06-12. `propose(...)` setzt
  `aiSuggestionId` und beachtet `targetStatus`.
- `approve`/`reject` akzeptieren NEEDS_VERIFICATION als Ausgangsstatus.
- `FindingsCreatedListener` propagiert die neuen Felder.
- `HardeningReportDataLoader.geplanteBehebung(...)` kennt den neuen
  Status.

### LLM-Gateway
- Prompt-Template `auto-assessment.st` (System-Prompt mit
  Daten/Anweisungs-Trennung, JSON-Schema-Pflicht, konservativer
  Default-Hinweis).

### AI-Services
- `AutoAssessmentConfig` (Flag, `topK`, `minRagScore`).
- `AutoAssessmentOrchestrator` implementiert `AiAssessmentSuggesterPort`:
  RAG -&gt; Prompt -&gt; LLM -&gt; konservativer Default -&gt;
  Halluzinations-Check -&gt; Persistenz `AiSuggestion`/`AiSourceRef`.

## Pragmatische Entscheidungen

- **Konservativer Default als Test-geschuetzte Invariante**: wenn
  kein RAG-Score &gt;= 0.6 und keine `usedProfileFields`, bleibt die
  Severity auf der CVSS-abgeleiteten Original-Severity (kein
  Downgrade). Wer den Default je weglassen will, muss den Test
  bewusst loeschen - das macht die Aenderung sichtbar.
- **Halluzinations-Check** ist zunaechst auf
  `proposedFixVersion` beschraenkt. Vergleich gegen
  `Finding.fixedInVersion`. Wenn Modell eine Version nennt, das
  Advisory aber keine fuehrt, gilt das ebenfalls als verdaechtig.
  Der Fall landet auf NEEDS_VERIFICATION, **nicht** stillschweigend
  durch.
- **Audit-Eintrag-Zuordnung**: Der `AiCallAuditService` liefert die
  Audit-Id derzeit nicht zurueck (Iteration 11 hat den Port als
  `void finalize(uuid, ...)` definiert). Der Orchestrator findet
  daher den korrekten OK-Eintrag ueber die Repository-Abfrage
  (letzter `OK` mit useCase=AUTO_ASSESSMENT). Sauberer waere ein
  Rueckgabewert; das ist offener Punkt.
- **Kein Auto-APPROVED**: Konzept 4.4 wird hart durchgesetzt -
  KI-Vorschlaege landen ausschliesslich als PROPOSED oder
  NEEDS_VERIFICATION. Ein Approve braucht weiterhin einen Menschen.
- **Cascade-Fehlertoleranz**: Wenn der AI-Pfad eine Exception wirft,
  fallt die Cascade auf MANUAL. So verhindert ein KI-Ausfall keinen
  Scan.

## Tests

### cvm-application (+4)
- `CascadeServiceAiStageTest` (3): Port liefert / leer / Fehler.
- `CascadeServiceTest` weiterhin gruen (Optional.empty als 3.
  Konstruktor-Argument).

### cvm-ai-services (+7)
- `AutoAssessmentOrchestratorTest` (7): deaktiviert, Happy-Path,
  konservativer Default, Halluzination, Service-Fehler,
  Halluzinations-Check ohne Treffer, Halluzination ohne
  Advisory-Version.

### Gesamt-Testlauf
```
./mvnw -T 1C test  BUILD SUCCESS  (~70 s)
```

| Modul | Gruen | Skipped | Rot |
|---|---:|---:|---:|
| cvm-domain | 4 | 0 | 0 |
| cvm-persistence | 0 | 6 | 0 |
| cvm-application | **126** | 0 | 0 |
| cvm-integration | 8 | 0 | 0 |
| cvm-llm-gateway | 52 | 0 | 0 |
| cvm-ai-services | **27** | 0 | 0 |
| cvm-api | 30 | 0 | 0 |
| cvm-app | 0 | 5 | 0 |
| cvm-architecture-tests | 8 | 0 | 0 |
| **Gesamt** | **255** | 11 | 0 |

Iteration 13 bringt **11 neue Tests** ins System.

## Synthetisches Mini-Auswertungsszenario

Mit Fake-LLM und 3 Test-Findings (CVSS 9.5 / 7.5 / 5.0) wuerde der
Orchestrator (Annahme: `cvm.ai.auto-assessment.enabled=true`):

- CVSS 9.5 + RAG-Treffer 0.85 + `usedProfileFields` belegt -&gt; AI
  schlaegt CRITICAL/HIGH vor (PROPOSED).
- CVSS 7.5 ohne RAG-Treffer -&gt; konservativer Default greift,
  Severity bleibt HIGH.
- CVSS 5.0 + LLM erfindet Fix-Version -&gt; NEEDS_VERIFICATION.

In allen drei Faellen entstehen `ai_suggestion`-Eintraege, die in
der Queue erscheinen. Mensch entscheidet weiterhin.

## Nicht im Scope

- UI-Anpassung (AI-Badge, Confidence-Anzeige) folgt Iteration 14.
- Reachability-Analyse (Iteration 15).
- Delta-Summary (Iteration 14).
- Mandanten-spezifische Modell-Profile (Iteration 21).

## Offene Punkte

- **Audit-Id-Zuordnung**: `AiCallAuditService.execute(...)` liefert
  nur den `LlmResponse` zurueck, nicht die UUID des Audit-Eintrags.
  Der Orchestrator umgeht das ueber Repository-Lookup; sauberer
  waere ein `AuditedLlmResponse(LlmResponse response, UUID auditId)`.
- **Batch-/Bucket-Verarbeitung**: Aktuell verarbeitet der
  `FindingsCreatedListener` jeden Finding sequenziell. Konzept-
  Vorgabe (10 parallel + Bucket4j) bleibt fuer Iteration 21 (KPIs)
  oder eine spaetere Performance-Iteration.
- **Halluzinations-Check** prueft nur `proposedFixVersion`. Weitere
  Faktenangaben (z.B. Quellen-URL) lassen sich analog ergaenzen.
- **Mandanten-Filter im RAG-Lookup**: aktuell ohne Filter; sobald
  Mandanten-Schluessel im Embedding stehen, hier ergaenzen.

## Ausblick Iteration 14
Copilot + Delta-Summary: KI-gestuetzte Erklaerung der
Veraenderungen zwischen zwei Scan-Runs, plus eine
Bewertungsoberflaeche fuer Auto-Vorschlaege.

## Dateien (wesentlich, neu)

- `cvm-domain/.../AssessmentStatus.java` (NEEDS_VERIFICATION)
- `cvm-persistence/.../db/migration/V0015__assessment_needs_verification.sql`
- `cvm-application/.../cascade/AiAssessmentSuggesterPort.java`
- `cvm-application/.../cascade/CascadeOutcome.java` (Felder)
- `cvm-application/.../cascade/CascadeService.java` (Stufe 3)
- `cvm-application/.../assessment/AssessmentStateMachine.java` (Uebergaenge)
- `cvm-application/.../assessment/AssessmentWriteService.java` (ProposeCommand)
- `cvm-application/.../assessment/FindingsCreatedListener.java` (Felder)
- `cvm-application/.../report/HardeningReportDataLoader.java` (Status-Mapping)
- `cvm-llm-gateway/src/main/resources/cvm/llm/prompts/auto-assessment.st`
- `cvm-ai-services/.../autoassessment/AutoAssessmentConfig.java`
- `cvm-ai-services/.../autoassessment/AutoAssessmentOrchestrator.java`
- `cvm-ai-services/src/test/.../autoassessment/AutoAssessmentOrchestratorTest.java`
- `cvm-application/src/test/.../cascade/CascadeServiceAiStageTest.java`
- `docs/20260417/iteration-13-plan.md`
- `docs/20260417/iteration-13-fortschritt.md`
- `docs/20260417/iteration-13-test-summary.md`
