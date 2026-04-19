# Iteration 77 - Plan: Echter ReachabilityAutoTriggerAdapter

**Jira**: CVM-314

## Ziel

Der in Iteration 70 eingefuehrte `ReachabilityAutoTriggerPort`
hatte bis jetzt nur einen Noop-Adapter. Mit Iteration 76 ist das
noetige Datenmodell (`Product.repoUrl`) vorhanden. Diese
Iteration verdrahtet den echten Adapter.

## Umfang

- Neue Klasse `ReachabilityAutoTriggerAdapter` in
  `cvm-ai-services/.../reachability`:
  - `@Component`, `@Primary` verdraengt den
    `NoopReachabilityAutoTriggerAdapter`.
  - Abhaengigkeiten: `FindingRepository`, `ReachabilityAgent`.
  - `trigger(findingId, triggeredBy)` laedt das Finding, zieht
    `repoUrl` aus `Product`, `commitSha` aus `ProductVersion`,
    und leitet Symbol/Language per `PurlSymbolDeriver` aus der
    PURL der Komponente ab. Fehlende Informationen fuehren zu
    einem Info-Log und kein Subprozess-Start.
  - Dann wird `ReachabilityAgent.analyze(request)` aufgerufen.
  - Das Ganze laueft {@code @Async}, damit der Event-Publish-
    Thread nicht blockiert.
- Neuer Property-Default:
  `cvm.ai.reachability.auto-trigger.branch=main` fuer den
  Branch-Hint, falls `ProductVersion.gitCommit` alleine nicht
  reicht.

## Nicht-Umfang

- Kein eigener Test, weil der Adapter den Subprozess-Lauf
  integral aufruft (Docker/Sandbox-abhaengig). Die
  Bestands-Tests von `ReachabilityAgent` und
  `ReachabilityAutoTriggerService` deckten die Pfade ab.
- Kein Caching beyond dem JGit-Adapter aus Iteration 71
  (dessen Cache reicht).

## Abnahme

- `./mvnw -T 1C test` gruen (Komponente wird vom Spring-Context
  erkannt, Bean-Auflosung bleibt intakt).
- ArchUnit gruen.
