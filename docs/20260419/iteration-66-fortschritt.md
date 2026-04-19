# Iteration 66 - Fortschritt: ClaudeApiClient auf Parameter-Resolver

**Jira**: CVM-303
**Datum**: 2026-04-19

## Was wurde gebaut

### Port + Adapter

- Neu: `cvm-llm-gateway/.../llm/config/LlmGlobalParameterResolver.java`
  (`@FunctionalInterface` mit `Optional<String> resolve(String key)`).
  Liegt im `cvm-llm-gateway`-Modul, damit die Adapter-Beans ihn
  injizieren koennen, ohne die ArchUnit-Regel
  `llm_gateway_greift_nur_auf_domain_zu` zu verletzen.
- Neu: `cvm-ai-services/.../ai/llmconfig/SystemParameterLlmGlobalResolver.java`
  - Implementierung, delegiert an
    `com.ahs.cvm.application.parameter.SystemParameterResolver`.
    Wohnt in `cvm-ai-services`, weil nur dieses Modul sowohl
    `cvm.application..` als auch `cvm.llm..` sieht (gleiches Muster
    wie `LlmConfigurationTenantSettingsProvider` aus Iteration 34c).

### ClaudeApiClient

- Neuer Konstruktor-Parameter `Optional<LlmGlobalParameterResolver>
  globalResolver`.
- `@Value`-Konstruktor bleibt als Fallback/Default-Werte erhalten;
  zusaetzlich `defaultTimeoutSeconds` persistiert.
- `complete(...)` loest jetzt `baseUrl`, `model`, `apiKey` und
  `timeoutSeconds` in dieser Reihenfolge auf:
  1. aktive `TenantLlmSettings`
  2. System-Parameter-Store ueber den neuen Port
  3. `@Value`-Default
- Neuer lazy RestClient-Cache (`AtomicReference<CachedClient>`):
  `clientFor(baseUrl, timeoutSeconds)` liefert den letzten Client,
  wenn sich beides nicht geaendert hat; sonst baut er einen neuen
  `RestClient` mit angepasstem `SimpleClientHttpRequestFactory`-
  Timeout. Kein Neustart noetig, damit Aenderungen greifen.
- Secret-Deckel: Der neue Adapter ruft nur
  `SystemParameterResolver.resolve(...)` auf, der Secret-Cipher
  bleibt hinter dem Resolver. Die ArchUnit-Regel
  `nur_parameter_modul_kennt_den_cipher` bleibt unverletzt.

### Katalog-Fortschreibung

- Neuer Eintrag `cvm.llm.claude.base-url` (String, Default
  `https://api.anthropic.com`, `hotReload=true`,
  `restartRequired=false`).
- `cvm.llm.claude.timeout-seconds` und `cvm.llm.claude.model` auf
  `hotReload=true`, `restartRequired=false` umgestellt.
- `cvm.llm.claude.api-key` auf `hotReload=true`,
  `restartRequired=false`. Bleibt Secret (AES-GCM via Cipher).
- `cvm.llm.claude.version` bleibt als RestClient-Header hart
  verdrahtet und weiterhin `restartRequired=true`.

### Tests

- Neuer Test `ClaudeApiClientTest#parameterResolverOverrideGreiftOhneRestart`:
  - Fake-Resolver aus `Map<String,String>`, zeigt zunaechst auf
    WireMock-A (Default-Server), dann auf WireMock-B.
  - Erster Call geht an WireMock-A, `x-api-key` = "key-default".
  - Nach Map-Aenderung geht Folgecall an WireMock-B,
    `x-api-key` = "key-override", Model-Id `claude-override`.
  - Kein Rebuild der Client-Bean noetig.
- `SystemParameterCatalogTest` angepasst:
  - `secrets_korrekt_konfiguriert`: differenziert jetzt Claude
    (live) und Feed/GitHub-Tokens (noch restartRequired).
  - `restart_required_markiert_richtige_keys`: Claude-Timeout,
    -Model, -ApiKey, -BaseUrl zaehlen zur "kein-Neustart"-Gruppe;
    `cvm.llm.claude.version` bleibt in der "muss-Neustart"-Gruppe.

## Ergebnisse

- `./mvnw -T 1C test` -> **BUILD SUCCESS** (alle 10 Reactor-Module).
- `ClaudeApiClientTest` -> 6 Tests PASS (5 alt + 1 neu).
- `SystemParameterCatalogTest` -> 13 Tests PASS (mit den
  aktualisierten Erwartungen).
- ArchUnit: `ParameterModulzugriffTest`, `ModulgrenzenTest`,
  `TenantScopeTest`, `SpringBeanKonstruktorTest` gruen.

## Migrations / Deployment

- Keine Flyway-Migration.
- Keine neuen Dependencies; es sind aber zwei neue Klassen
  hinzugekommen (Port + Adapter), daher vor dem naechsten
  `scripts/start.sh` einmal `./mvnw -T 1C clean install
  -DskipTests` laufen lassen, damit die neuen Klassen im lokalen
  Maven-Repo landen.

## Noch offen

- Iteration 67: analoge Callsite-Migration fuer `NvdFeedClient`,
  `GhsaFeedClient`, `KevFeedClient`, `EpssFeedClient`.
- Iteration 68: `GitHubApiProvider` (Fix-Verifikation).
- Iteration 69: Deployment-Doku fuer
  `cvm.encryption.parameter-secret`.
