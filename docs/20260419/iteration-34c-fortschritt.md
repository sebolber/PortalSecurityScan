# Iteration 34c – Fortschritt

**Thema**: LlmGateway zieht die aktive LlmConfiguration pro Mandant
**Jira**: CVM-78
**Datum**: 2026-04-19

## Umgesetzt

- **Neue Abstraktion** `TenantLlmSettings` (Record) +
  `TenantLlmSettingsProvider` (FunctionalInterface) im
  `cvm-llm-gateway`. Der Gateway bleibt frei von Persistenz-
  Abhaengigkeiten.
- **`LlmClient.provider()`** als Default-Methode (liefert
  `unknown`). Adapter ueberschreiben: `ClaudeApiClient = "anthropic"`,
  `OllamaClient = "ollama"`. Damit laesst sich eine Tenant-Konfig
  per Provider-Match einem Client zuordnen.
- **`ClaudeApiClient`** liest zum Call die Settings aus dem
  Provider. Wenn passend (provider=anthropic), werden baseUrl,
  Modell-Id und API-Key pro Call uebersteuert. Sonst laeuft der
  bisherige Spring-Value-Pfad (ANTHROPIC_API_KEY,
  cvm.llm.claude.*).
- **`OllamaClient`** analog: Tenant-Override fuer baseUrl + Modell.
  Ollama verwendet kein API-Key-Header-Schema, darum nur beides.
- **`LlmClientSelector`** nutzt jetzt den TenantLlmSettingsProvider
  als erste Quelle fuer die Adapter-Auswahl (Provider-Match).
  Unbekannter Provider => Fallback auf EnvironmentModelResolver mit
  Log-Warnung.
- **Bridge** `LlmConfigurationTenantSettingsProvider` im
  `cvm-ai-services`-Modul. Ruft
  `LlmConfigurationService.activeForCurrentTenant()` und
  `resolveSecret(id)`, uebersetzt die View in TenantLlmSettings.
  Fehler beim Resolve werden geschluckt (Fallback auf Default) -
  KI-Calls sollen nicht an einem kaputten Admin-Bereich scheitern.
- **Tests** neu:
  - `LlmConfigurationTenantSettingsProviderTest` (4 Faelle)
  - `LlmClientSelectorTest` (zwei neue Faelle: tenant-provider-
    gewinnt, tenant-provider-unbekannt-fallback)
  - `ClaudeApiClientTest#tenantOverride` (WireMock mit zweitem
    Tenant-Server, prueft URL + Header-Key + Modell)

## Nicht-Ziel

- OpenAI-/Azure-/Adesso-Adapter - noch keine HTTP-Implementation
  vorhanden. Fuer diese Provider greift der Fallback mit
  Log-Warnung, bis ein Adapter mit passendem `provider()`-Wert
  existiert.
- LLM-Rate-Limit-Tuning.

## Architekturpruefung

- ArchUnit: weiter gruen. `cvm-llm-gateway -> cvm-domain` bleibt
  erhalten; die neue Bridge liegt in `cvm-ai-services`, das sowohl
  Application als auch Gateway sehen darf.

## Test-Status

- `./mvnw -T 1C test`: BUILD SUCCESS (alle Module gruen).
- Keine Frontend-Aenderungen in 34c, keine Doppeltests fuer
  Angular noetig.

## Hinweise

- Neue JAR-Abhaengigkeiten in `cvm-ai-services`: vor dem naechsten
  `scripts/start.sh` einmal
  `./mvnw -T 1C clean install -DskipTests` laufen lassen.
