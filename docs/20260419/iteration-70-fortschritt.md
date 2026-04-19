# Iteration 70 - Fortschritt: Reachability-Auto-Trigger (Schwellwert + Rate-Limit)

**Jira**: CVM-307
**Datum**: 2026-04-19

## Was wurde gebaut

### Config

- Zwei neue Katalog-Eintraege in `SystemParameterCatalog`:
  - `cvm.ai.reachability.auto-trigger-threshold` (DECIMAL, Default
    `0.6`, hotReload=true).
  - `cvm.ai.reachability.auto-trigger-cooldown-minutes` (INTEGER,
    Default `60`, hotReload=true).
- `ReachabilityConfig` bekommt zwei neue `Effective`-Methoden
  (`autoTriggerThresholdEffective()`,
  `autoTriggerCooldownMinutesEffective()`), die den Store per
  `SystemParameterResolver` konsultieren und auf die `@Value`-
  Defaults zurueckfallen.
- Der neue 5-arg-Konstruktor ist `@Autowired`; der alte 3-arg-
  Konstruktor bleibt fuer Bestands-Unit-Tests erhalten (delegierend
  mit Defaults).

### Event, Port, Listener

- `LowConfidenceAiSuggestionEvent` (Record,
  `cvm.ai.autoassessment`): findingId, productVersionId, cveKey,
  confidence, triggeredBy.
- `ReachabilityAutoTriggerPort` (Interface, package
  `cvm.ai.reachability`): `trigger(UUID, String)`.
- `NoopReachabilityAutoTriggerAdapter`: Default-Bean, loggt nur.
  `@ConditionalOnMissingBean(ReachabilityAutoTriggerPort.class)`
  erlaubt es Iteration 71, einen JGit-Adapter einzuspeisen.
- `ReachabilityAutoTriggerService`: listener auf
  `@TransactionalEventListener(AFTER_COMMIT)` und Default
  `@EventListener` (Tests). Rate-Limit via
  `ConcurrentHashMap<RateLimitKey, Instant>` (Schluessel:
  productVersionId + cveKey). Delegiert an den Port, wenn
  - Feature aktiv,
  - Confidence unter Schwelle,
  - Cooldown ausgelaufen.

### Orchestrator-Anpassung

- `AutoAssessmentOrchestrator` bekommt zwei optionale Setter
  (`@Autowired(required=false)`) fuer `ApplicationEventPublisher`
  und `ReachabilityConfig`. Bestands-Konstruktor-Signatur bleibt
  unveraendert, Bestands-Tests laufen ohne Anpassung weiter.
- Am Ende von `suggest(...)` (nach Persistenz der AiSuggestion)
  publiziert die neue Methode `publiziereAutoTriggerEvent(...)`
  das Event, wenn Publisher und Config vorhanden sind UND
  `confidence < threshold`. cveKey und productVersionId werden
  aus dem Finding-Scan-Graphen bezogen.

### Tests

- `ReachabilityAutoTriggerServiceTest` (7 Cases):
  - Niedrige Confidence -> Port wird aufgerufen.
  - Hohe Confidence -> kein Port-Aufruf.
  - Cooldown im gleichen Service -> zweiter Trigger unterdrueckt.
  - Frisch gestarteter Service -> Cache leer, Trigger geht durch.
  - Unterschiedliche CVEs -> unabhaengig.
  - Feature deaktiviert -> kein Port-Aufruf.
  - Cooldown=0 -> Sofort-Folge-Trigger erlaubt.

## Ergebnisse

- `./mvnw -T 1C test` -> **BUILD SUCCESS** in 02:18 min.
- Alle 10 Reactor-Module gruen, ArchUnit-Regeln unveraendert.
- `SpringBeanKonstruktorTest` (Multi-Konstruktor-Check) bleibt
  gruen, weil die neuen Konstruktoren jeweils einen
  `@Autowired`-Default haben.

## Offen / bewusst verschoben

- **Keine echte Reachability-Analyse**: der Noop-Port loggt nur.
  Iteration 71 ersetzt ihn durch einen Adapter, der
  `ReachabilityAgent.analyze(...)` mit einem aus Finding
  abgeleiteten `ReachabilityRequest` aufruft (benoetigt
  JGit-Checkout fuer ein echtes Arbeitsverzeichnis).
- **Keine Audit-Persistenz** fuer "Trigger geplant" - der
  Event wird nur in-memory protokolliert. Falls spaeter eine
  Audit-Spur gewuenscht ist, folgt ein DB-Eintrag im
  JGit-Adapter.

## Migrations / Deployment

- Keine Flyway-Migration.
- Neue Katalog-Eintraege werden vom Bootstrap pro Mandant beim
  naechsten Start gespiegelt.
- Konstruktor-Signaturen haben sich erweitert; vor dem naechsten
  `scripts/start.sh` einmal `./mvnw -T 1C clean install
  -DskipTests` laufen lassen.
