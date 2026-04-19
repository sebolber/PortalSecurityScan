# Iteration 40 – Fortschritt

**Thema**: OpenAI-kompatibler LLM-Adapter (openai/azure/adesso-ai-hub)
**Jira**: CVM-84
**Datum**: 2026-04-19

## Umgesetzt

- Neuer Adapter `OpenAiCompatibleClient` (`cvm-llm-gateway/adapter`)
  spricht `POST /chat/completions` mit
  `response_format: json_object`. Laeuft nur, wenn
  `cvm.llm.enabled=true`.
- Provider-Schluessel:
  - `openai` + `adesso-ai-hub` schicken den Key als
    `Authorization: Bearer ...`.
  - `azure` schickt den Key als `api-key`-Header. Die Base-URL ist
    bei Azure verpflichtend (sonst OpenAiException).
- Ohne aktive `LlmConfiguration` fuer einen dieser Provider bleibt
  der Adapter auf Eis. Default-Spring-Properties sind bewusst
  nicht hinterlegt, weil wir keine Credentials im Repo haben.
- **`LlmClient`** bekommt eine Default-Methode
  `supportsProvider(String)`. Adapter, die mehrere Provider
  bedienen, ueberschreiben sie. `LlmClientSelector` nutzt jetzt
  diesen Match-Punkt statt direkten String-Vergleich.

## Tests

- `OpenAiCompatibleClientTest` (5 Faelle) mit WireMock:
  - OpenAI Happy-Path (Bearer-Auth, choices+usage parsen).
  - Azure-Variante (api-key-Header).
  - adesso-ai-hub (Bearer, beliebige baseUrl).
  - Exception ohne aktives Tenant-Setting.
  - `supportsProvider` deckt openai/azure/adesso-ai-hub ab,
    nicht aber ollama/unknown.

## Test-Status

- `./mvnw -T 1C test`: BUILD SUCCESS.
- ArchUnit: gruen.

## Offene Punkte

- Provider-Defaults (`cvm.llm.openai.*` in
  `application.yaml`) fuer Nicht-Tenant-Konfiguration: ausgelassen,
  weil es im Sandbox-Betrieb keine echten Credentials gibt. Kann
  in spaeterer Iteration ergaenzt werden.
- `EnvironmentModelResolver`-Integration fuer OpenAI-Modelle.
  Aktuell nur ueber `LlmConfiguration`.
