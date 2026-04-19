# Iteration 77 - Fortschritt: Echter ReachabilityAutoTriggerAdapter

**Jira**: CVM-314
**Datum**: 2026-04-19

## Was wurde gebaut

- Neue Klasse `ReachabilityAutoTriggerAdapter`
  (`cvm-ai-services/.../reachability`), `@Component` +
  `@Primary`:
  - Ersetzt den `NoopReachabilityAutoTriggerAdapter` im
    Spring-Context.
  - Laedt das Finding per `FindingRepository`, zieht
    `repoUrl` aus `Product` (neu seit Iteration 76) und
    `commitSha` aus `ProductVersion.gitCommit`. Die PURL wird
    ueber `finding.componentOccurrence.component.purl` bezogen;
    `PurlSymbolDeriver.derive(purl)` liefert Symbol + Language.
  - Fehlende Daten (Repo, Commit, PURL, Symbol) fuehren zu
    einem Info-Log und keinem Reachability-Lauf.
  - Bei Treffer ruft der Adapter
    `ReachabilityAgent.analyze(request)` auf. `@Async` trennt
    den Subprozess-Lauf vom Event-Publisher-Thread.
- Neuer `@Value`-Default `cvm.ai.reachability.auto-trigger.branch`
  (Fallback `main`).

## Ergebnisse

- `./mvnw -T 1C test` -> **BUILD SUCCESS** in 02:38 min.
- Alle 10 Reactor-Module gruen.
- ArchUnit-Regeln unveraendert gruen.
- `SpringBeanKonstruktorTest` gruen (1 Konstruktor, kein
  Mehrdeutigkeit).

## Nicht-Umfang / Offen

- Kein neuer Test; die Reachability-Subprocess-Integration ist
  sandbox-abhaengig (Docker, Chromium).
- Sobald in der naechsten CI ein Subprozess-Runner verfuegbar
  ist, kann ein End-to-End-Test ergaenzt werden.

## Migrations / Deployment

- Keine Flyway-Migration, keine neuen Dependencies. Vor dem
  naechsten `scripts/start.sh` wegen der neuen Bean einmal
  `./mvnw -T 1C clean install -DskipTests` laufen.
