# Iteration 66 - Plan: ClaudeApiClient auf Parameter-Resolver

**Jira**: CVM-303

## Ziel

`ClaudeApiClient` benutzt heute `@Value("${cvm.llm.claude.*}")`
im Konstruktor. Aenderungen im System-Parameter-Store greifen
daher erst nach Neustart. Diese Iteration macht die vier
Claude-Adapter-Schalter live-reloadable:

- `cvm.llm.claude.base-url`
- `cvm.llm.claude.model`
- `cvm.llm.claude.api-key`
- `cvm.llm.claude.timeout-seconds`

## Architektur-Constraint

`cvm-llm-gateway` darf nur `domain` kennen (ArchUnit-Regel
`llm_gateway_greift_nur_auf_domain_zu`). Direktes Injizieren von
`SystemParameterResolver` ist daher verboten. Wir nutzen das schon
etablierte Port/Adapter-Muster aus Iteration 34c
(`TenantLlmSettingsProvider`):

- **Port** (neu) `com.ahs.cvm.llm.config.LlmGlobalParameterResolver`
  im `cvm-llm-gateway`-Modul mit
  `Optional<String> resolve(String paramKey)`.
- **Adapter** (neu)
  `com.ahs.cvm.ai.llmconfig.SystemParameterLlmGlobalResolver`
  im `cvm-ai-services`-Modul, ruft den bestehenden
  `SystemParameterResolver` auf.

## ClaudeApiClient-Anpassung

- Zusaetzlicher Konstruktor-Parameter
  `Optional<LlmGlobalParameterResolver> globalResolver` (default-
  leer im Test-Konstruktor, damit bestehende Tests unberuehrt
  bleiben).
- Vorrang-Reihenfolge pro Call fuer model/api-key/base-url/timeout:
  1. aktive `TenantLlmSettings` (wie bisher),
  2. sonst Global-Resolver (= System-Parameter-Store),
  3. sonst `@Value`-Fallback aus `application.yaml`.
- RestClient wird pro Call wiederverwendet, wenn
  (baseUrl, timeoutSeconds) unveraendert. Bei Drift wird lazy ein
  neuer RestClient gebaut und in einem `AtomicReference` gecacht.
- Katalog-Eintraege `cvm.llm.claude.*` auf
  `restartRequired=false` aktualisieren; neue Basis-URL-Entry
  dazunehmen.

## Tests (TDD)

Neuer Test in `ClaudeApiClientTest`:

- `parameterResolverOverrideGreiftOhneRestart` - zwei Calls
  hintereinander mit sich aenderndem Resolver-Result:
  - erster Call mit fallback-URL A liefert Antwort von WireMock-A.
  - Resolver schwenkt auf URL B; ohne neuen Client werden
    Folgecalls an WireMock-B geschickt.

Bestehende Tests (`happyPath`, `rateLimited`, `serverError`,
`tenantOverride`, `kaputteAntwort`) bleiben unveraendert und
muessen weiterhin gruen sein.

## Katalog-Fortschreibung

`SystemParameterCatalog`:
- Neuer Eintrag `cvm.llm.claude.base-url`
  (String, Default `https://api.anthropic.com`,
  `restartRequired=false`).
- `cvm.llm.claude.model`, `cvm.llm.claude.timeout-seconds`,
  `cvm.llm.claude.version` behalten restartRequired (Version wird
  in RestClient-Builder hart verdrahtet, Model/Timeout wurden bis
  Iteration 65 ebenfalls nur beim Boot gelesen). Nach Iteration 66
  sind Model und Timeout live, Version bleibt restartRequired.

## Risiken

- Wenn `resolver.resolve(...)` im falschen Tenant-Kontext
  laeuft, liefert er `Optional.empty()` (per
  `SystemParameterResolver`-Semantik). Das ist das erwuenschte
  Verhalten - Hintergrund-Jobs ohne Tenant fallen auf `@Value`
  zurueck.
- Secret-Entschluesselung ist in `SystemParameterResolver`
  kapselt. Der neue Adapter leitet den entschluesselten Wert
  durch; im Audit-Log erscheint nichts, weil der Resolver nur
  liest.

## Abnahme

- Neuer Test gruen, bestehende Tests gruen.
- `./mvnw -T 1C test` -> BUILD SUCCESS.
- ArchUnit `ParameterModulzugriffTest`, `ModulgrenzenTest` weiter
  gruen (kein neuer Modul-Zugriff; Repository und Cipher werden
  nicht referenziert; `cvm.llm..` bleibt frei von
  `cvm.application..`).
