# Iteration 71 - Plan: JGit-Adapter fuer Reachability

**Jira**: CVM-308

## Ziel

`NoopGitCheckoutAdapter` durch einen echten `JGitGitCheckoutAdapter`
ersetzen. Der Adapter klont (bzw. fetched) einen Commit und liefert
den Arbeitsverzeichnis-Pfad. Cache pro Commit; alter Eintrag wird
vom Scheduler entfernt. SSH-Key aus dem System-Parameter-Store.

## Umfang

- Neue Dependency: `org.eclipse.jgit:org.eclipse.jgit:6.10.0`.
- Klasse `JGitGitCheckoutAdapter`
  (package `com.ahs.cvm.ai.reachability`):
  - Bean-Name `jgitGitCheckoutAdapter` (verdraengt den Noop).
  - Cache-Root aus `cvm.ai.reachability.git.cache-dir` (Default
    `${java.io.tmpdir}/cvm-reach-cache`).
  - Pro (normalisiertes-Repo, Commit) Unterordner. Bei
    Wiederverwendung wird der Pfad ohne Netz-Call zurueckgegeben.
  - Clone-Command nutzt `CredentialsProvider` mit
    `SystemParameterResolver`-Wert fuer
    `cvm.ai.reachability.git.https-token` (optional;
    fuer HTTPS-Repos). SSH bleibt Follow-up (braucht ssh-agent).
  - Maximale Cache-Groesse ueber `@Scheduled` (default
    `0 0 3 * * *` = 03:00 taeglich) wird auf
    `cvm.ai.reachability.git.cache-ttl-hours` (Default 72)
    gekuerzt.
- `application.yaml`-Defaults eintragen.
- Katalog-Eintraege (System-Parameter-Store):
  - `cvm.ai.reachability.git.cache-dir` (STRING, Default
    `tmp`-Pfad, hotReload=false).
  - `cvm.ai.reachability.git.https-token` (PASSWORD, sensitive,
    hotReload=true).
  - `cvm.ai.reachability.git.cache-ttl-hours` (INTEGER, 72,
    hotReload=true).
- Tests:
  - `JGitGitCheckoutAdapterTest` mit einem lokalen Bare-Repo
    (JGit `InitCommand`). Clone per `file://`-URL, Commit-
    Checkout, Cache-Reuse-Beweis, Cleanup-Job-Verhalten.
  - Sandbox-Hinweis: kein realer Netzwerk-Clone, kein SSH-Setup.
    Vollstaendiger HTTPS-/SSH-Pfad wird in Folge-Iteration mit
    Docker-Sandbox getestet.
- Doku-Hinweis in `iteration-71-fortschritt.md`, dass SSH-Support
  noch offen ist.

## Nicht-Umfang

- Kein Adapter fuer den `ReachabilityAutoTriggerPort` - das kommt
  als separate Iteration, sobald `JGitGitCheckoutAdapter` in
  Production validiert ist.
- Kein Network-Sandboxing fuer den Subprocess (bleibt offener Punkt).

## Abnahme

- `./mvnw -T 1C test` -> BUILD SUCCESS.
- ArchUnit gruen.
- JGit-Adapter-Test laeuft lokal (file:// repo), Sandbox-
  unabhaengig.
