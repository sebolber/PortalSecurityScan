# Iteration 68 - Fortschritt: GitHubApiProvider auf Parameter-Resolver

**Jira**: CVM-305
**Datum**: 2026-04-19

## Was wurde gebaut

### GitHubApiProvider

- Konstruktor nimmt jetzt einen optionalen
  `SystemParameterResolver` entgegen. Fallback-Token bleibt als
  `@Value`-Wert erhalten.
- `RestClient` wird **ohne** `Authorization`-Default-Header
  gebaut; der Token wird pro Call via `headers(h ->
  authorize(...))` hinzugefuegt, sodass Aenderungen im
  System-Parameter-Store sofort wirken.
- Alle drei oeffentlichen Methoden (`releaseNotes`, `compare`,
  `postMergeRequestComment`) nutzen jetzt `resolveToken()`
  mit Reihenfolge: Resolver → `@Value`-Fallback.
- Zusatz-Konstruktor fuer Tests: WireMock + Resolver injizierbar;
  der Bestands-Einzeiler fuer "nur WireMock" bleibt erhalten.

### Katalog

- `cvm.ai.fix-verification.github.token`: hotReload=true,
  restartRequired=false. Damit sind alle vier Secrets
  (`cvm.llm.claude.api-key`, `cvm.feed.nvd.api-key`,
  `cvm.feed.ghsa.api-key`, `cvm.ai.fix-verification.github.token`)
  live-reloadable. AES-GCM-Verschluesselung bleibt bei allen
  erhalten.

### Tests

- `GitHubApiProviderTest#tokenOverrideGreiftOhneRestart`:
  Mockito-Resolver liefert zunaechst `store-token-1`, dann
  `store-token-2`. Zwei `releaseNotes`-Aufrufe, zwei WireMock-
  Requests, jeweils mit unterschiedlichem `Authorization: Bearer ...`
  Header.
- `SystemParameterCatalogTest`:
  - `secrets_korrekt_konfiguriert` prueft jetzt einheitlich fuer
    alle vier Secret-Keys, dass `restartRequired=false` und
    `hotReload=true` gesetzt sind.
  - `restart_required_markiert_richtige_keys` nimmt
    `cvm.ai.fix-verification.github.token` in die
    live-reloadable-Liste auf.

## Ergebnisse

- `./mvnw -T 1C test` -> **BUILD SUCCESS** in 02:22 min.
- `GitHubApiProviderTest` 6 Tests PASS (5 alt + 1 neu).
- `SystemParameterCatalogTest` 13 Tests PASS.
- ArchUnit unveraendert gruen.

## Stand der Callsite-Migration

Alle vier Callsites der Nicht-LLM-Entity-Schalter laufen jetzt
ueber den `SystemParameterResolver`:

| Adapter                 | Modul               | Iteration |
| ----------------------- | ------------------- | --------- |
| ClaudeApiClient         | cvm-llm-gateway     | 66        |
| NvdFeedClient           | cvm-integration     | 67        |
| GhsaFeedClient          | cvm-integration     | 67        |
| GitHubApiProvider       | cvm-integration     | 68        |

Damit ist die Callsite-Migrations-Task aus
`docs/20260419/offene-punkte.md` abgeschlossen. Offen bleibt
ausschliesslich die Deployment-Doku
(`cvm.encryption.parameter-secret`, Iteration 69).

## Migrations / Deployment

- Keine Flyway-Migration, keine neuen Dependencies.
- Konstruktor-Signatur des `GitHubApiProvider` hat sich erweitert;
  vor dem naechsten `scripts/start.sh` einmal
  `./mvnw -T 1C clean install -DskipTests` laufen.
