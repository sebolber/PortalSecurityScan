# Iteration 18 - Anomalie-Check + Profil-Assistent - Fortschritt

**Jira**: CVM-43
**Datum**: 2026-04-18
**Branch**: `claude/iteration-10-pdf-report-C9sA4`

## Zusammenfassung

Zwei Hinweisgeber-Funktionen sind live, ohne in den Bewertungsfluss
einzugreifen.

**Anomalie-Check**: stuendlicher Job wertet vier deterministische
Muster aus (KEV+NOT_APPLICABLE, &ge;5 ACCEPT_RISK-Waiver,
aehnliche Rationale zu abgelehnten Vorschlaegen, Downgrade um
&ge;2 Severity-Stufen ohne RULE-Herkunft). Bei Treffer wird ein
`AnomalyEvent` geschrieben und ein Alert mit Trigger
`KI_ANOMALIE` abgefeuert. **Der Service beruehrt kein Assessment**
(harte Invariante, via Mock-verify getestet).

**Profil-Assistent**: dialogische Session-API mit Start/Reply/
Finalize. Fragen werden priorisiert nach "Feld leer oder oft in
`rationaleSourceFields` der letzten 100 Assessments". Finalize
erzeugt einen Profil-Draft ueber den regulaeren Workflow aus
Iteration 04 - **kein Direkt-Schreibpfad** auf das aktive Profil.
Session-TTL 24 h; abgelaufene Sessions werden abgelehnt.

## Umgesetzt

### Domain / Persistenz
- `AlertTriggerArt.KI_ANOMALIE` (neu).
- Flyway `V0019__anomaly_und_profile_assistant.sql`:
  - `anomaly_event` mit Check-Constraint fuer vier Muster +
    Severity (INFO/WARNING/CRITICAL).
  - `alert_rule`-Constraint um `KI_ANOMALIE` erweitert.
  - `profile_assist_session` (stateful, TTL).
- Entities + Repos: `AnomalyEvent`/`AnomalyEventRepository`,
  `ProfileAssistSession`/`ProfileAssistSessionRepository`.

### LLM-Gateway
- Prompts `anomaly-review.st` (optionaler LLM-Zweitcheck),
  `profile-wizard.st`.

### AI-Services
**Anomaly** (`cvm-ai-services/anomaly`)
- `AnomalyConfig` (Flag, Schwellwerte, LLM-Zweitstufe optional).
- `AnomalyDetectionService` mit vier Pattern-Checks, Dedup via
  `existsByAssessmentIdAndPattern`, **keine** Assessment-Writes.
- `AnomalyCheckJob` (Cron `0 0 * * * *`) -&gt; `AlertEvaluator`
  mit Trigger `KI_ANOMALIE`.
- `AnomalyQueryService` + `AnomalyView` fuer REST-Read.

**Profil-Assistent** (`cvm-ai-services/profileassistant`)
- `ProfileAssistantConfig` (Flag, TTL).
- `ProfileAssistantService` mit `start` / `reply` / `finalize`.
  - Fragen-Priorisierung aus den Top-10 `rationaleSourceFields`
    der letzten 100 Assessments (via `AssessmentRepository`).
  - Dialog-Historie als JSON im Session-Record.
  - `finalize` ruft `ContextProfileService.proposeNewVersion`
    (regulaerer DRAFT-Workflow, Vier-Augen fuer Aktivierung
    unveraendert).

### API
- `AnomalyController` (`GET /api/v1/anomalies`,
  `GET /api/v1/anomalies/count`).
- `ProfileAssistantController` (3 Endpunkte unter
  `/api/v1/environments/{id}/profile/assist`).
- Exception-Handler: 400/404/409.

## Sicherheits-Invarianten (durch Tests gehaertet)

- **Anomalie-Service schreibt keine Assessments**
  (`AnomalyDetectionServiceTest.kevNotApplicable` mit Mockito
  `verify(assessmentRepo, never()).save(any())`).
- **Dedup**: doppelte Events werden nicht gespeichert.
- **Flag-aus**: keine Pattern-Auswertung ohne Feature-Flag.
- **Profil-Assistent ohne Direkt-Schreib**: finalize ruft
  AUSSCHLIESSLICH `proposeNewVersion` am `ContextProfileService`
  (Mockito `verifyNoMoreInteractions`).
- **Session-TTL**: abgelaufene Sessions werden auf EXPIRED
  gesetzt und weitere Reply/Finalize-Calls werfen.

## Pragmatische Entscheidungen

- **String-Jaccard** statt RAG-Embedding fuer "aehnlich zu
  abgelehnt" - kostet nichts und ist deterministisch. Embedding-
  Variante ist offener Punkt.
- **LLM-Zweitstufe deaktiviert** im Default
  (`cvm.ai.anomaly.use-llm-second-stage=false`). Konzept laesst
  es optional; wir schalten erst wenn False-Positives
  anschlagen.
- **Profil-Draft-YAML** ist ein bewusst einfacher Entwurf
  (`answers:`-Liste aus Dialog). Mapping in das echte
  Profil-Schema passiert im Approve-Schritt oder in einer
  spaeteren Iteration.

## Tests (neu)

### cvm-ai-services (+13)
- `AnomalyDetectionServiceTest` (7): alle vier Muster + Flag-aus
  + Dedup + Invariante.
- `ProfileAssistantServiceTest` (6): start, reply, finalize,
  Direkt-Schreib-Invariante, Timeout, deaktiviert.

### cvm-api (+5)
- `AnomalyControllerWebTest` (2): Liste, Count.
- `ProfileAssistantControllerWebTest` (3): start, reply,
  finalize, 404.

### Gesamt-Testlauf
```
./mvnw -T 1C test  BUILD SUCCESS  (~67 s)
```
Voller Build gruen, Test-Anzahl deutlich gewachsen. Exakte
Zahlen im Test-Summary.

## Nicht im Scope

- UI-Widget "Hinweise der letzten 24 h" im Dashboard
  (Backend-Vertrag steht).
- Wizard-UI im Profil-Bereich (Angular-Komponente folgt).
- RAG-Embedding fuer Muster 3.
- Automatische Sperre eines Assessments (explizit nicht).

## Offene Punkte

- **LLM-Zweitstufe** ist vorbereitet (Prompt da), aber noch
  nicht im Service verdrahtet. Bei realem Datenverkehr
  aktivieren.
- **UI** fuer Anomalien + Wizard (Backend-Vertrag steht).
- **Session-Cleanup-Cron**: alte EXPIRED/FINALIZED-Sessions
  aufraeumen.
- **Profil-Draft-YAML** an das echte Schema mappen.
- **Mehrbenutzer-Dialog-Handling**: aktuell eine Session pro
  Start-Call, keine explizite Sperre pro Environment.

## Dateien (wesentlich, neu)

### Persistenz
- `cvm-persistence/src/main/resources/db/migration/V0019__anomaly_und_profile_assistant.sql`
- `cvm-persistence/.../anomaly/*`
- `cvm-persistence/.../profileassist/*`

### Domain
- `cvm-domain/.../AlertTriggerArt.java` (KI_ANOMALIE)

### LLM-Gateway
- `cvm-llm-gateway/.../prompts/anomaly-review.st`
- `cvm-llm-gateway/.../prompts/profile-wizard.st`

### AI-Services
- `cvm-ai-services/.../anomaly/*`
- `cvm-ai-services/.../profileassistant/*`

### API
- `cvm-api/.../anomaly/*`
- `cvm-api/.../profileassistant/*`

### Docs
- `docs/20260417/iteration-18-plan.md`
- `docs/20260417/iteration-18-fortschritt.md`
- `docs/20260417/iteration-18-test-summary.md`
