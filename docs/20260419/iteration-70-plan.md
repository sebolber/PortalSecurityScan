# Iteration 70 - Plan: Auto-Trigger Reachability bei niedriger AI-Confidence

**Jira**: CVM-307

## Ziel

Der `AutoAssessmentOrchestrator` liefert pro Finding eine
`confidence` zwischen 0 und 1. Bei niedriger Confidence ist ein
zusaetzlicher Reachability-Lauf angezeigt. Diese Iteration
verdrahtet die **Trigger-Logik** (Schwellwert + Rate-Limit);
der tatsaechliche Subprocess-Aufruf bleibt hinter einem Port
und wird in Iteration 71 (JGit-Adapter) konkretisiert.

## Scope

1. **Config**: Neuer Parameter-Store-Eintrag
   `cvm.ai.reachability.auto-trigger-threshold` (Typ DECIMAL,
   Default `0.6`). `ReachabilityConfig#autoTriggerThresholdEffective()`
   liest ihn per `SystemParameterResolver`.
2. **Event**: `LowConfidenceAiSuggestionEvent` (Record, package
   `com.ahs.cvm.ai.autoassessment`). Enthaelt findingId,
   productVersionId, cveKey, confidence, triggeredBy.
3. **Publisher**: `AutoAssessmentOrchestrator.suggest(...)`
   publiziert das Event via `ApplicationEventPublisher` **nach**
   erfolgreicher Persistenz der `AiSuggestion`, wenn
   `confidence < autoTriggerThreshold`.
4. **Port**:
   `com.ahs.cvm.ai.reachability.ReachabilityAutoTriggerPort`
   mit Methode `trigger(UUID findingId, String triggeredBy)`.
   Default-Impl `NoopReachabilityAutoTriggerAdapter` (loggt nur);
   die echte Implementierung folgt in Iteration 71 nach dem
   JGit-Adapter.
5. **Listener**:
   `ReachabilityAutoTriggerService` faengt das Event ab
   (`@TransactionalEventListener(AFTER_COMMIT)`):
   - prueft `ReachabilityConfig#enabledEffective()`;
   - prueft `confidence < autoTriggerThresholdEffective()`;
   - prueft In-Memory-Rate-Limit pro `(productVersionId, cveKey)`
     mit TTL `cvm.ai.reachability.auto-trigger-cooldown-minutes`
     (Default 60);
   - delegiert an `ReachabilityAutoTriggerPort`.

## Tests (TDD)

- `ReachabilityAutoTriggerServiceTest`:
  - **Confidence unterhalb Schwelle, kein Rate-Limit**:
    Port wird genau einmal aufgerufen.
  - **Confidence oberhalb Schwelle**: kein Port-Aufruf.
  - **Rate-Limit greift**: zweiter Event fuer selbes
    `(productVersionId, cveKey)` innerhalb Cooldown ruft den
    Port nicht.
  - **Unterschiedliche CVEs**: werden unabhaengig voneinander
    behandelt.
  - **Feature deaktiviert** (`enabledEffective=false`): kein
    Port-Aufruf.
- `AutoAssessmentOrchestratorTest` um einen Case erweitern, der
  verifiziert, dass bei niedriger Confidence das Event
  publiziert wird.

## Katalog-Updates

- `cvm.ai.reachability.auto-trigger-threshold` (DECIMAL, `0.6`,
  hotReload=true, restartRequired=false).
- `cvm.ai.reachability.auto-trigger-cooldown-minutes` (INTEGER,
  `60`, hotReload=true, restartRequired=false).

## Nicht-Umfang

- Der Noop-Adapter triggert noch keinen echten Subprocess. Das
  ist bewusst: ohne JGit-Checkout (Iteration 71) fehlt das
  Arbeitsverzeichnis. Stattdessen wird der Event nur geloggt
  und als Audit-Fuss-Abdruck gesichert. Port-Ersatz in
  Iteration 71 bleibt ein Einzeiler in Spring.

## Abnahme

- `./mvnw -T 1C test` -> BUILD SUCCESS, alle ArchUnit-Regeln
  gruen.
- Neue Service-Tests gruen.
- Kein Frontend-Touch.
