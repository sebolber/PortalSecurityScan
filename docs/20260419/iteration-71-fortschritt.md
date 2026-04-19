# Iteration 71 - Fortschritt: JGit-Adapter fuer Reachability

**Jira**: CVM-308
**Datum**: 2026-04-19

## Was wurde gebaut

- **Dependency**: `org.eclipse.jgit:org.eclipse.jgit:6.10.0` in
  `cvm-ai-services/pom.xml` aufgenommen.
- **Neue Klasse** `JGitGitCheckoutAdapter`
  (`com.ahs.cvm.ai.reachability`, Bean-Name
  `jgitGitCheckoutAdapter`):
  - Klont pro `(repoUrl, commitSha)` in ein Cache-Verzeichnis
    unterhalb `cvm.ai.reachability.git.cache-dir`
    (Default `${java.io.tmpdir}/cvm-reach-cache`).
  - Cache-Key ist ein 16-stelliger SHA-256-Prefix ueber URL+Sha
    plus die ersten 12 Sha-Zeichen, vermeidet Kollisionen.
  - Ist das Target schon vorhanden, wird ohne Netz-Call
    zurueckgegeben (Cache-Reuse).
  - Optionales HTTPS-Credentials-Token aus dem
    System-Parameter-Store
    (`cvm.ai.reachability.git.https-token`). Bei Fehlern wird
    der halb-geclonte Ordner aufgeraeumt.
  - `@Scheduled(cron = "0 0 3 * * *")` ruft `cleanupCache()`
    auf: loescht Cache-Eintraege, deren letzte Aenderung vor
    `cvm.ai.reachability.git.cache-ttl-hours` (Default 72)
    Stunden lag.
- **Bean-Verdraengung**: Der
  `NoopGitCheckoutAdapter`-Bean besitzt bereits die
  `@ConditionalOnMissingBean(name = "jgitGitCheckoutAdapter")`-
  Annotation. Sobald der echte Adapter im Context ist, wird der
  Noop-Adapter nicht mehr erzeugt. In Unit-Tests, die ohne
  Spring-Context laufen, bleibt der Noop-Adapter nutzbar.

### Katalog

Drei neue Eintraege in `SystemParameterCatalog` (AI_REACHABILITY,
Subkategorie `git`):

- `cvm.ai.reachability.git.cache-dir` (STRING, restartRequired=true).
- `cvm.ai.reachability.git.cache-ttl-hours` (INTEGER, 72,
  hotReload=true).
- `cvm.ai.reachability.git.https-token` (PASSWORD, sensitive,
  hotReload=true).

### Tests

`JGitGitCheckoutAdapterTest` (5 Tests):

- Erster Checkout klont das bare-Repo und liefert einen
  `.git`-Ordner.
- Zweiter Checkout desselben Commits nutzt den Cache (kein
  erneuter Clone, mtime unveraendert).
- Leerer commitSha wirft `IllegalArgumentException`.
- `cleanupCache()` entfernt abgelaufene Eintraege.
- Parameter-Store-Override fuer `cache-dir` greift und
  beeinflusst das Target-Verzeichnis.

**Sandbox-Hinweis**: Der Test legt lokal ein Bare-Repo per JGit an
und clont ueber `file://`. Es findet kein Netzwerk-Zugriff statt.
Vollstaendige HTTPS-/SSH-Tests gehoeren in eine CI mit Docker-
Sandbox; derartige Integrationstests bleiben offener Punkt.

### Lokale Git-Konfig

Im Test-Setup wird `gpg.format=openpgp` und `commit.gpgsign=false`
pro Repo-Config gesetzt, damit JGit nicht ueber die user-globale
`gpg.format=ssh`-Einstellung stolpert (bekanntes Issue in 6.10).

## Ergebnisse

- `./mvnw -T 1C test` -> **BUILD SUCCESS** in 02:07 min.
- `JGitGitCheckoutAdapterTest` 5 Tests PASS.
- `SystemParameterCatalogTest` gruen (Sensitive-Count von 4 auf 5
  aktualisiert).
- ArchUnit-Regeln gruen.

## Offen

- SSH-Credentials (Vault / ssh-agent) fuer JGit-Clones ueber
  `git@github.com:...`/`ssh://`. Momentan werden solche URLs ohne
  Credentials-Provider geclont, funktioniert nur bei bereits
  eingerichtetem ssh-agent.
- Network-Sandboxing fuer den Subprocess
  (cvm-frontend-unabhaengiger Punkt, bleibt offen).
- `ReachabilityAutoTriggerPort`-Adapter, der
  `ReachabilityAgent.analyze(...)` mit dem JGit-Workdir aufruft.
  Wird als Iteration 73+ fortgefuehrt, sobald SSH-Support steht.

## Migrations / Deployment

- Keine Flyway-Migration.
- Neue Maven-Dependency (JGit); vor dem naechsten
  `scripts/start.sh` einmal `./mvnw -T 1C clean install
  -DskipTests` laufen lassen.
- Drei neue Katalog-Eintraege werden vom Bootstrap pro Mandant
  gespiegelt.
