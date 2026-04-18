# Iteration 11 - LLM-Gateway + Audit - Test-Summary

**Jira**: CVM-30
**Datum**: 2026-04-18

## Testlauf

```
./mvnw -T 1C test  BUILD SUCCESS  (~59 s)
```

## Neue Tests (Iteration 11)

### `cvm-llm-gateway` (45)

| Testklasse | Anzahl | Kurz |
|---|---:|---|
| `InjectionDetectorTest` | 12 | 11 Marker + `checkAll`-Sammel. |
| `OutputValidatorTest` | 7 | Schema, Severity, Anweisungs-Muster, Typen, null, Laenge. |
| `AiCallAuditServiceTest` | 7 | Happy-Path, Flag aus, Block-Injection, Warn-Injection, Invalid-Output, Client-Fehler, Rate-Limit. |
| `LlmClientSelectorTest` | 3 | Per-Resolver, Fallback, unbekanntes Modell. |
| `LlmRateLimiterTest` | 3 | Tenant, Global, ohne Tenant. |
| `LlmCostCalculatorTest` | 3 | Berechnung, keine Tabelle, null-Tokens. |
| `PromptTemplateLoaderTest` | 4 | Laden, fehlendes Template, fehlende Variable, Parse-Struktur. |
| `ClaudeApiClientTest` | 4 | Happy, 429, 500, kaputter JSON. |
| `OllamaClientTest` | 2 | Happy, 5xx. |

### `cvm-ai-services` (4)

| Testklasse | Anzahl | Kurz |
|---|---:|---|
| `JpaAiCallAuditAdapterTest` | 4 | Pending, Finalize-OK, Doppelfinalize, unbekannte Id. |

### Gesamt-Testlage

| Modul | Gruen | Skipped | Rot |
|---|---:|---:|---:|
| cvm-domain | 4 | 0 | 0 |
| cvm-persistence | 0 | 6 | 0 |
| cvm-application | 122 | 0 | 0 |
| cvm-integration | 8 | 0 | 0 |
| cvm-llm-gateway | **45** | 0 | 0 |
| cvm-ai-services | **4** | 0 | 0 |
| cvm-api | 29 | 0 | 0 |
| cvm-app | 0 | 5 | 0 |
| cvm-architecture-tests | 8 | 0 | 0 |
| **Gesamt** | **220** | 11 | 0 |

## Coverage

- Kernlogik im Gateway (`AiCallAuditService`, `InjectionDetector`,
  `OutputValidator`, `LlmRateLimiter`, `LlmCostCalculator`,
  `PromptTemplateLoader`) ist durch zielgenaue Unit-Tests hoch abgedeckt.
  Ein exakter JaCoCo-Prozentwert ist nicht geloggt; Pitest bleibt
  Iteration 12 als Add-on.
- Die beiden Adapter (`ClaudeApiClient`, `OllamaClient`) werden ueber
  WireMock-Tests abgedeckt. Die negativ-Pfade (429, 500, kaputter
  Payload) sind abgedeckt.

## Architektur

- `ModulgrenzenTest` (7) + `SpringBeanKonstruktorTest` (1) gruen.
- ArchUnit-Regel `llm_gateway_greift_nur_auf_domain_zu` bleibt
  erzwungen; das `cvm-llm-gateway` hat **keine** Abhaengigkeit auf
  `cvm-persistence`.
- Bean-Konstruktor-Regel: Beide HTTP-Adapter haben einen expliziten
  `@Autowired`-Hauptkonstruktor (neben dem Test-Konstruktor).

## Determinismus / Sicherheit

- Keine echten API-Calls in CI. Alle Adapter-Tests laufen gegen
  `WireMockServer` mit dynamischem Port und
  `SimpleClientHttpRequestFactory` (HTTP/1.1).
- Feature-Flag-Default `cvm.llm.enabled=false` heisst, dass die beiden
  Adapter in Prod ohne explizite Freigabe nicht einmal als Spring-
  Beans existieren.
