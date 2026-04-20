# Iteration 11 – LLM-Gateway + Audit

**Jira**: CVM-30
**Abhängigkeit**: Meilenstein M1 (Iterationen 00–10)
**Ziel**: Eigenständiges, auditierbares Modul für alle KI-Aufrufe. Nichts anderes
im System darf direkt ein LLM ansprechen.

---

## Kontext
Konzept v0.2 Abschnitt 4.4 und 12.2–12.5. Ab dieser Iteration ist KI aktiv.
**Jeder KI-Call** muss vollständig auditiert sein.

## Scope IN
1. Neues Modul `cvm-llm-gateway` (in Iteration 00 bereits als leeres Modul
   angelegt).
2. Kern-Interface:
   ```java
   public interface LlmClient {
       LlmResponse complete(LlmRequest request);
       record LlmRequest(String useCase, String systemPrompt, List<Message> messages,
                         JsonNode outputSchema, double temperature, int maxTokens,
                         Map<String,Object> metadata) {}
       record LlmResponse(JsonNode structuredOutput, String rawText,
                          TokenUsage usage, Duration latency, String modelId) {}
   }
   ```
3. Adapter:
   - `ClaudeApiClient` – Anthropic Messages API (HTTP, nicht SDK, für bessere
     Kontrolle), nutzt `anthropic-version`-Header und `tool_use` für
     strukturierte Ausgabe, konservative Defaults (Temperature 0.1).
   - `OllamaClient` – on-prem-Fallback (HTTP gegen `http://ollama:11434`),
     für Tests und für datensensible Mandanten.
   - Selector `LlmClientSelector.select(environment, useCase)` → wählt Adapter
     anhand des Modell-Profils der Umgebung.
4. Prompt-Template-System:
   - Templates in `cvm-llm-gateway/prompts/*.st` (StringTemplate oder
     simple `${...}`-Substitution; kein Velocity/Freemarker wegen
     Komplexität).
   - Jede Template hat: `id`, `version`, `useCase`, `system`, `userTemplate`,
     `outputSchema.json`.
   - Version im Audit mitgeschrieben.
5. Flyway `V0010__ki_audit.sql`:
   - `ai_call_audit`: `id`, `use_case`, `model_id`, `model_version`,
     `prompt_template_id`, `prompt_template_version`,
     `system_prompt`, `user_prompt`, `rag_context`, `raw_response`,
     `prompt_tokens`, `completion_tokens`, `latency_ms`, `cost_eur`,
     `triggered_by`, `environment_id`, `status` (`PENDING|OK|INVALID_OUTPUT|INJECTION_RISK|ERROR`),
     `injection_risk`, `invalid_output_reason`, `created_at`.
   - `ai_suggestion`, `ai_source_ref` gemäß Konzept (jetzt mit Daten
     gefüllt, nicht erst ab Iteration 13).
6. `AiCallAuditService` – verpflichtender Wrapper. Ablauf:
   1. Audit `PENDING` persistieren,
   2. Injection-Detektor auf User-Prompt (inkl. RAG-Content) laufen lassen,
   3. Call ausführen,
   4. Output-Validator (Schema-Validierung, Severity-Enum-Check, keine
      „Ignore previous"-Muster in Rationale),
   5. Audit finalisieren (`OK` oder Fehlerstatus).
7. Injection-Detektor (`cvm-llm-gateway/injection/`):
   - Regelbasiert: Marker-Listen („ignore previous", „system prompt:",
     Rollenvertauschung, Base64-verdächtige Blöcke, zero-width chars).
   - Erkennung ⇒ Call wird markiert, aber nicht zwingend blockiert
     (Konfigurations-Flag `cvm.llm.injection.mode=warn|block`).
8. Rate-Limiter (Bucket4j) pro Mandant und global.
9. Kosten-Tracking: Preise je Modell als Konfiguration (`application.yaml`,
   aktualisierbar ohne Deploy), pro Call `cost_eur` berechnet und im Audit.
10. Feature-Flag `cvm.llm.enabled=false` als Default. Wenn aus, wirft jeder
    `LlmClient.complete()` `LlmDisabledException`. Tests laufen ausschließlich
    mit Mock-Clients, **niemals gegen echte API** in CI.

## Scope NICHT IN
- Konkrete Anwendungsfälle (Auto-Assessment, Copilot, Summary) – ab
  Iteration 13.
- RAG (Iteration 12).

## Aufgaben
1. Modul-POM, Abhängigkeiten auf `cvm-domain` und `cvm-persistence` (für
   Audit-Persistenz via eigenes Repository).
2. HTTP-Client via `RestClient` (Spring 6.1+) mit Timeout-/Retry-Config.
3. Secrets-Zugriff über `@ConfigurationProperties` mit Vault-Placeholder
   `${ANTHROPIC_API_KEY:}`.
4. Separate Rolle `AI_AUDITOR` darf `ai_call_audit` lesen, sonst niemand
   (RLS oder Query-Filter).

## Test-Schwerpunkte
- `AiCallAuditServiceTest`: Audit-Datensatz vor und nach Call,
  PENDING→OK-Übergang, Fehlerpfad.
- `InjectionDetectorTest`: positive und negative Fälle, mindestens 10 Marker.
- `OutputValidatorTest`: Schema-Verletzung, unzulässige Severity,
  „Anweisung an den Nutzer"-Muster im Rationale.
- `ClaudeApiClientTest` (WireMock): Happy-Path, Timeout, 429, 500.
- `LlmDisabledExceptionTest`: Feature-Flag wirkt.
- `@DisplayName`: `@DisplayName("Audit: Call ohne Audit-Eintrag wird vom Gateway abgelehnt")`

## Definition of Done
- [ ] LlmClient + Audit komplett testabgedeckt.
- [ ] Injection-Detektor mit Marker-Datenbank.
- [ ] Output-Validator hart.
- [ ] Feature-Flag wirksam.
- [ ] Keine echten API-Calls in CI, nur WireMock.
- [ ] Coverage `cvm-llm-gateway` ≥ 90 % (Kernlogik).
- [ ] Fortschrittsbericht.
- [ ] Commit: `feat(llm-gateway): LLM-Abstraktion mit Audit, Injection-Schutz und Rate-Limit\n\nCVM-30`

## TDD-Hinweis
Der Gateway ist die sicherheitskritischste Komponente des Projekts.
Teste **zuerst** die Invarianten (kein Call ohne Audit, Injection-Marker
werden erkannt, Flag wirkt). Produktionscode danach. **Ändere NICHT die
Tests** bei Rot – das sind die Sicherheitsnetze.

## Abschlussbericht
Standard, plus Dokument `docs/konzept/llm-gateway-invarianten.md` mit der
endgültigen Liste der erzwungenen Invarianten (für Revision).
