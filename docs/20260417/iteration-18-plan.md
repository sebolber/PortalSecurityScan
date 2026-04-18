# Iteration 18 - KI-Anomalie-Check + Profil-Assistent - Plan

**Jira**: CVM-43
**Branch**: `claude/iteration-10-pdf-report-C9sA4`
**Abhaengigkeit**: Iteration 04 (Profil), 09 (Alerts), 13 (KI-Vorbewertung).

## Architektur

**Teil A - Anomalie-Check**
- `cvm-persistence`: neue Tabelle `anomaly_event` (Flyway V0019).
- `cvm-ai-services/anomaly`:
  - `AnomalyPatternCheck` Records fuer die vier Muster.
  - `AnomalyDetectionService`: Pattern-Checks deterministisch
    zuerst; LLM als Zweitpruefung nur bei Pattern-Treffern
    (use-case `ANOMALY_REVIEW`).
  - `AnomalyCheckJob` (Cron `0 0 * * * *`, stuendlich).
  - Integration ins Alert-System: bei Anomalie -&gt;
    `AlertEvaluator` mit Trigger `KI_ANOMALIE`.
- `AnomalyInvariantTest` prueft hart: der Service darf
  `AssessmentRepository.save(...)` **niemals** aufrufen (Mock-
  verify `never()`).

**Teil B - Profil-Assistent**
- `cvm-persistence`: `profile_assist_session` (stateful,
  TTL 24 h) - Session-Id, environment-Id, bisherige
  Antworten als JSON, Status, created_at, expires_at.
- `cvm-ai-services/profileassistant`:
  - `ProfileAssistantService` mit drei Methoden `start`,
    `reply`, `finalize`.
  - Fragen priorisiert nach (a) Feld aktuell unbesetzt,
    (b) Feld oft in letzten 100 `rationaleSourceFields`.
  - LLM (`profile-wizard.v1`) formuliert Frage; Service
    persistiert Antworten als Dialog-Historie.
  - `finalize` erzeugt einen neuen `ContextProfile`-Draft
    ueber den bestehenden Approve-Workflow aus Iteration 04.
- REST:
  - `POST /api/v1/environments/{id}/profile/assist`
  - `POST /api/v1/environments/{id}/profile/assist/{sessionId}/reply`
  - `POST /api/v1/environments/{id}/profile/assist/{sessionId}/finalize`

## Sicherheits-Invarianten

1. **Kein Auto-Change**: Anomalie-Service aendert keine
   Assessment-Felder (Invariante via Mock-verify).
2. **Kein Direkt-Schreib am Profil**: finalize legt nur einen
   Draft an, Aktivierung bleibt Vier-Augen.
3. **Feature-Flag** `cvm.ai.anomaly.enabled=false` und
   `cvm.ai.profile-assistant.enabled=false` Default.
4. **Session-TTL** 24 h; abgelaufene Sessions werden abgelehnt.

## Tests

1. `AnomalyDetectionServiceTest` (5): vier Muster + Invariante.
2. `AnomalyCheckJobTest` (3): Flag aus, Treffer -&gt; Alert,
   kein Treffer -&gt; kein Alert.
3. `ProfileAssistantServiceTest` (5): start, reply, finalize
   erzeugt Draft, finalize ruft keinen Direkt-Schreib-Pfad,
   Timeout.
4. `AnomalyControllerWebTest` (3): Liste, 403 ohne Rolle, 200.
5. `ProfileAssistantControllerWebTest` (4): start, reply,
   finalize, 404.

## Scope NICHT IN

- Retrospektive Anomalie-Analyse &gt; 30 Tage.
- Mehrbenutzer-Dialoge am selben Profil.
- UI-Widget im Dashboard (Backend-Vertrag steht).
- Cosine-Similarity-Check (Muster 3): nutzt heuristisch
  String-Distance statt RAG-Embeddings im ersten Durchlauf
  (Embeddings-Ergaenzung folgt).
