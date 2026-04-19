# Iteration 67 - Fortschritt: Feed-Clients auf Parameter-Resolver

**Jira**: CVM-304
**Datum**: 2026-04-19

## Was wurde gebaut

### NvdFeedClient

- Zusaetzlicher Konstruktor-Parameter
  `Optional<SystemParameterResolver> parameterResolver`; der
  Bestands-Konstruktor bleibt fuer Tests (Default: leer).
- Neue private Methode `resolveApiKey()` liest pro Call
  `cvm.feed.nvd.api-key` aus dem Parameter-Store und faellt auf
  `FeedProperties.getNvd().getApiKey()` zurueck.
- `abfragen(...)` nutzt den aufgeloesten Wert fuer den `apiKey`-
  Header. Kein Neustart mehr noetig, wenn sich der NVD-Key im
  Admin-UI aendert.

### GhsaFeedClient

- Analoge Konstruktor-Erweiterung. Zusaetzlich beruecksichtigt
  `isEnabled()` jetzt den resolvten Token (via
  `resolveApiKey()`), damit der Adapter live aktiv/inaktiv
  schalten kann, sobald ein Token gepflegt wird bzw. geloescht
  wurde.
- Authorization-Header nutzt pro Call den aufgeloesten Token.

### KevFeedClient / EpssFeedClient

- Kein api-key vorhanden, keine Callsite-Migration noetig. Die
  Adapter lesen base-url weiterhin aus `FeedProperties`, was laut
  "Nicht-Migrieren"-Liste (docs/20260419/offene-punkte.md) der
  Soll-Zustand ist.

### Katalog

- `cvm.feed.nvd.api-key` und `cvm.feed.ghsa.api-key`:
  `hotReload=true`, `restartRequired=false`. Beschreibung im
  Handbuch-Feld aktualisiert.

### Tests (TDD)

- `NvdFeedClientTest#parameterResolverOverrideGreiftOhneRestart`:
  Fake-Resolver via Mockito mit einer `Map<String,String>`.
  Zwei GET-Requests nach `/rest/json/cves/2.0`:
  - Erster Call `apiKey=store-key-1`.
  - Map wird auf `store-key-2` umgestellt, Folgecall sendet den
    neuen Wert.
- `GhsaFeedClientTest#parameterResolverOverrideGreiftOhneRestart`:
  analog, pro Call ein anderer `Authorization: Bearer <token>`.
- `SystemParameterCatalogTest`: NVD-/GHSA-Secret jetzt in der
  live-reloadable-Gruppe, `ai.fix-verification.github.token`
  bleibt bis Iteration 68 restartRequired.

## Ergebnisse

- `./mvnw -T 1C test` -> **BUILD SUCCESS** in 02:21 min.
- `NvdFeedClientTest` 4 Tests PASS (3 alt + 1 neu).
- `GhsaFeedClientTest` 3 Tests PASS (2 alt + 1 neu).
- ArchUnit: `ParameterModulzugriffTest`, `ModulgrenzenTest`,
  `TenantScopeTest`, `SpringBeanKonstruktorTest` gruen.

## Migrations / Deployment

- Keine Flyway-Migration, keine neuen Dependencies.
- Vor dem naechsten `scripts/start.sh` bitte
  `./mvnw -T 1C clean install -DskipTests` laufen lassen
  (Konstruktor-Signaturen der Adapter haben sich erweitert).
