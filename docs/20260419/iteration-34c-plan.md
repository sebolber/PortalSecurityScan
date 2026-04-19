# Iteration 34c – Plan

**Thema**: LlmGateway liest die aktive LlmConfiguration pro Mandant
**Jira**: CVM-78
**Datum**: 2026-04-19

## Ziel

Die neuen `LlmConfiguration`-Datensaetze (Iteration 34 + 34b) sollen
zur Laufzeit vom LlmGateway beruecksichtigt werden. Statt nur die
Spring-Profile-Konfiguration zu verwenden, schlaegt pro Call die
mandantenspezifische Konfiguration durch:

- Provider (`anthropic` / `ollama` / ...) entscheidet den Adapter.
- `baseUrl`, `model` und `secret` werden zur Laufzeit als Override
  an den Adapter gereicht.
- Faellt nichts ein, bleibt das bisherige Verhalten (Spring-Profile)
  aktiv.

## Scope

- Neues Abstraktions-Paar in `cvm-llm-gateway`:
  - `TenantLlmSettings` (Record: provider, model, baseUrl, apiKey)
  - `TenantLlmSettingsProvider` (Interface mit
    `Optional<TenantLlmSettings> resolveCurrent()`)
- Erweiterung `LlmClient` um `provider()`-Methode mit Default
  (`unknown`) und Ueberschreibung in ClaudeApiClient ("anthropic")
  und OllamaClient ("ollama").
- `ClaudeApiClient` + `OllamaClient` ziehen beim Call - wenn ein
  Provider vorliegt - die Override-Werte aus einer per-Call
  `RestClient`-Neubildung. Der Basispfad bleibt gleich.
- `LlmClientSelector` bevorzugt fuer das Mandanten-Matching den
  Provider aus dem `TenantLlmSettings`. Ohne aktive Konfig laeuft
  weiter die `EnvironmentModelResolver`-Kette.
- Bridge-Bean in `cvm-ai-services`:
  `LlmConfigurationTenantSettingsProvider` nutzt den bestehenden
  `LlmConfigurationService` und `resolveSecret(id)`, liefert die
  Settings fuer den aktuell geloggten Mandanten.

## Architektur

```
cvm-llm-gateway/
  TenantLlmSettings.java                (Record)
  TenantLlmSettingsProvider.java        (Interface)
  LlmClient.java                        (+ provider())
  adapter/ClaudeApiClient.java          (+ Tenant-Override)
  adapter/OllamaClient.java             (+ Tenant-Override)
  LlmClientSelector.java                (beruecksichtigt Provider)

cvm-ai-services/
  llmconfig/LlmConfigurationTenantSettingsProvider.java
```

## Tests

- `ClaudeApiClientTenantOverrideTest`: Setzt einen Stub-Provider,
  prueft dass baseUrl + Model + Header-API-Key aus dem Settings
  kommen (WireMock).
- `OllamaClientTenantOverrideTest`: analog.
- `LlmClientSelectorTenantAwarenessTest`: bei aktiver Konfig wird der
  passende Adapter per `provider` gewaehlt; Fallback auf
  EnvironmentModelResolver bleibt bestehen.
- `LlmConfigurationTenantSettingsProviderTest`: liefert
  `Optional.empty()` bei fehlendem Mandanten / keinem aktiven Eintrag,
  sonst die Settings.

## Stopp-Kriterien

- ArchUnit-Regeln bleiben gruen (`cvm-llm-gateway -> cvm-domain`
  only).
- Bestehende Tests nicht rot.
- Secret verlaesst den Prozess nicht (Audit-Pfad unveraendert, nur
  HTTP-Header).

## Nicht-Ziel

- OpenAI/Azure/Adesso-Adapter - der Bridge-Provider-Wert aus der
  Konfig reicht jene Provider erst durch, wenn die Adapter in
  kommenden Iterationen existieren. Bis dahin greift der Fallback
  auf EnvironmentModelResolver mit Log-Warnung.
- Rate-Limit-Tuning - folgt Iteration 35 (OSV) und separate LLM-
  RateLimit-Iteration.
