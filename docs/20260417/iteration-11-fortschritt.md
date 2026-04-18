# Iteration 11 - LLM-Gateway + Audit - Fortschritt

**Jira**: CVM-30
**Datum**: 2026-04-18
**Branch**: `claude/iteration-10-pdf-report-C9sA4` (Folgearbeit;
Branches wurden nicht getrennt, um den Context-Strang durchgaengig
zu halten).

## Zusammenfassung

Das `cvm-llm-gateway`-Modul ist jetzt die einzige Stelle im System,
die LLMs ansprechen darf. Es erzwingt zehn Invarianten (Doku:
`docs/konzept/llm-gateway-invarianten.md`), die durch 45 Gateway-
Tests + 4 JPA-Adapter-Tests abgesichert sind. Das Feature-Flag
`cvm.llm.enabled=false` ist Default, damit niemand aus Versehen
gegen die Anthropic-API ruft. ArchUnit-Regel
`llm_gateway_greift_nur_auf_domain_zu` bleibt strikt; die Persistenz
lebt im Adapter-Modul `cvm-ai-services`.

## Umgesetzt

### Domain
- Enums `AiCallStatus` und `AiSuggestionStatus`.

### Persistenz (`cvm-persistence/ai`)
- Entity `AiCallAudit` + `AiCallAuditImmutabilityListener` + Repo.
- Entity `AiSuggestion` + Repo.
- Entity `AiSourceRef` + Repo.
- Flyway `V0013__ki_audit.sql` (drei Tabellen, Check-Constraints auf
  Status und Severity, Indizes fuer Queries).

### LLM-Gateway (`cvm-llm-gateway`)
| Klasse | Zweck |
|---|---|
| `LlmClient` | Interface + Records (`LlmRequest`, `LlmResponse`, `Message`, `TokenUsage`). Invarianten im Compact-Constructor. |
| `LlmDisabledException` | Feature-Flag aus. |
| `InvalidLlmOutputException` | Schema-/Severity-/Anweisungs-Verstoss. |
| `InjectionRiskException` | Block-Modus. |
| `LlmGatewayConfig` | Feature-Flag, Injection-Mode, Default-Modell, Rate-Limits. |
| `InjectionDetector` | Min. 10 Heuristiken (u.a. `ignore previous`, Zero-Width, Base64-Bloecke, `<system>`-Tag, `{{ }}`-Template-Injection). |
| `OutputValidator` | Schema-Check, Severity-Enum-Check, Anweisungs-Muster im Rationale. |
| `LlmRateLimiter` | Bucket4j, global + per-tenant. |
| `LlmCostCalculator` | Euro-Preis aus `cvm.llm.pricing.*`. |
| `AiCallAuditPort` | Persistenz-Port (Pending/Finalize). |
| `AiCallAuditService` | Verpflichtender Wrapper. 7-Schritte-Flow mit Audit, Injection, Call, Validator, Finalisierung. |
| `LlmClientSelector` | Pro (Umgebung, Use-Case) -&gt; LlmClient. |
| `PromptTemplate` + `PromptTemplateLoader` | `${var}`-Substitution, fehlende Variable wirft. |
| `ClaudeApiClient` | RestClient gegen Anthropic Messages API (`/v1/messages`), `anthropic-version`-Header, strukturierte JSON-Antwort. |
| `OllamaClient` | RestClient gegen `/api/chat`, `format:json` + `options`. |

### Prompt-Templates
- `cvm/llm/prompts/assessment.propose.st` mit System-Prompt, Daten-
  Block und strikter Ausgabe-Vorgabe.

### AI-Services (`cvm-ai-services`)
- `JpaAiCallAuditAdapter` bridget den Port gegen das
  `AiCallAuditRepository`. Setzt `finalizingAllowed=true` nur beim
  Uebergang `PENDING -&gt; Final`.

### Konfiguration
- Abhaengigkeiten: `openhtmltopdf` bleibt nur in Application. Neu
  im Gateway: `spring-web`, `jackson-databind`, `bucket4j-core`,
  `wiremock-standalone` (test).

## Pragmatische Entscheidungen

- **Flyway-Nummer V0013 statt V0010**: V0010 wurde in Iteration 06
  vergeben (Bewertungs-Workflow), V0011 in Iteration 09 (Alerts),
  V0012 in Iteration 10 (PDF-Report). V0013 ist die naechste freie
  Nummer.
- **RestClient + `SimpleClientHttpRequestFactory` in Tests**: WireMock
  supportet kein HTTP/2, der JDK-Default des RestClient schon. Wir
  setzen in den Tests explizit `SimpleClientHttpRequestFactory`
  (HTTP/1.1), damit WireMock sauber antworten kann.
- **Port-Adapter-Split**: Die CLAUDE.md-Modulgrenze
  `llm-gateway -&gt; domain` wurde strikt gehalten. Der Prompt hat
  "Abhaengigkeit auf cvm-persistence" vorgeschlagen, wir setzen das
  ueber einen Port (`AiCallAuditPort`) und einen JPA-Adapter in
  `cvm-ai-services` um. So bleibt das Gateway ArchUnit-konform.
- **Injection-Mode Default `warn`**: Erfassen ohne Abbruch. Produktive
  Umgebungen koennen auf `block` umstellen.
- **Prompt-Template-Syntax**: Einfache `${var}`-Substitution, kein
  Velocity/FreeMarker (gemaess Prompt). Fehlende Variable ist
  Pflichtfehler, um leere Prompts auszuschliessen.
- **Rate-Limit-Audit**: Rate-Limit-Ablehnung fuehrt zu einem
  auditiertem `RATE_LIMITED`-Eintrag - Konzept 12.2 "jeder Call
  auditiert", auch abgelehnte.
- **SonarCloud Token-Handling**: API-Key wird ueber `@Value` mit
  Vault-Fallback `${ANTHROPIC_API_KEY:}` gelesen; landet nie im Repo.
- **Keine echte HTTP-Tests gegen Anthropic/Ollama**: ausschliesslich
  WireMock. Adapter sind via `@ConditionalOnProperty(cvm.llm.enabled=true)`
  gesichert, sodass sie in der Default-Konfiguration gar nicht
  geladen werden.

## Tests (neu)

### cvm-llm-gateway (45)
- `InjectionDetectorTest` (12): 11 Marker-Treffer + `checkAll`-Sammel.
- `OutputValidatorTest` (7).
- `AiCallAuditServiceTest` (7): Happy-Path, Flag-aus, Block, Warn,
  Invalid-Output, Client-Fehler, Rate-Limit.
- `LlmClientSelectorTest` (3).
- `LlmRateLimiterTest` (3).
- `LlmCostCalculatorTest` (3).
- `PromptTemplateLoaderTest` (4).
- `ClaudeApiClientTest` (4): Happy, 429, 500, kaputter JSON.
- `OllamaClientTest` (2).

### cvm-ai-services (4)
- `JpaAiCallAuditAdapterTest`: Pending, Finalize-OK, Doppelfinalize,
  unbekannte Id.

### Gesamt-Testlauf
```
./mvnw -T 1C test  BUILD SUCCESS  (~59 s)
```

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

Iteration 11 bringt **49 neue Tests** ins System.

## Invarianten-Dokument

`docs/konzept/llm-gateway-invarianten.md` listet die zehn hart
getesteten Invarianten (I1 bis I10). Die ArchUnit-Regel
`SpringBeanKonstruktorTest` hat nach Einfuehrung der beiden Adapter
einmal ausgeschlagen; beide Adapter haben jetzt einen expliziten
`@Autowired`-Hauptkonstruktor.

## Nicht im Scope

- Konkrete KI-Use-Cases (Auto-Assessment, Copilot, Summary) -
  Iteration 13.
- RAG/pgvector - Iteration 12.
- Retry / Circuit-Breaker ueber Resilience4j - bleibt offener Punkt,
  folgt sobald der Gateway produktiv laeuft.
- Row-Level-Security fuer `ai_call_audit` (Leserolle `AI_AUDITOR`) -
  Security-Rollen werden zentral in einer spaeteren Iteration
  verdrahtet.

## Offene Punkte (fuer naechste Iterationen)

- **Resilience4j**: Retry/Circuit-Breaker um `ClaudeApiClient` und
  `OllamaClient`.
- **AI_AUDITOR-Rolle**: Lese-Endpunkt fuer `ai_call_audit`,
  idealerweise in einem spaeteren `cvm-api/ai-audit`-Controller.
- **`modelVersion`-Erkennung**: Adapter liest aktuell nur `model`
  aus der Antwort. Sobald Anthropic/Ollama eine explizite Version
  liefern, mappen.
- **Persistenz-Integrationstest fuer V0013**: Docker-skip wie bei den
  anderen Flyways.
- **Trigger in Postgres fuer Audit-Immutability**: Aktuell nur JPA-
  Listener. DB-seitig spaeter absichern.

## Ausblick Iteration 12
RAG mit pgvector - Embeddings von CVE-Beschreibungen, Kontextprofilen
und historischen Bewertungen. Der LLM-Gateway bekommt einen
`ragContext`-Feldwert, den er bereits heute in das Audit mitfuehrt.

## Dateien (wesentlich, neu)

### Domain
- `cvm-domain/src/main/java/com/ahs/cvm/domain/enums/AiCallStatus.java`
- `cvm-domain/src/main/java/com/ahs/cvm/domain/enums/AiSuggestionStatus.java`

### Persistenz
- `cvm-persistence/src/main/resources/db/migration/V0013__ki_audit.sql`
- `cvm-persistence/src/main/java/com/ahs/cvm/persistence/ai/AiCallAudit.java`
- `cvm-persistence/src/main/java/com/ahs/cvm/persistence/ai/AiCallAuditImmutabilityListener.java`
- `cvm-persistence/src/main/java/com/ahs/cvm/persistence/ai/AiCallAuditRepository.java`
- `cvm-persistence/src/main/java/com/ahs/cvm/persistence/ai/AiSuggestion.java`
- `cvm-persistence/src/main/java/com/ahs/cvm/persistence/ai/AiSuggestionRepository.java`
- `cvm-persistence/src/main/java/com/ahs/cvm/persistence/ai/AiSourceRef.java`
- `cvm-persistence/src/main/java/com/ahs/cvm/persistence/ai/AiSourceRefRepository.java`

### LLM-Gateway
- `cvm-llm-gateway/pom.xml` (neu verdrahtet)
- `cvm-llm-gateway/src/main/java/com/ahs/cvm/llm/LlmClient.java`
- `cvm-llm-gateway/src/main/java/com/ahs/cvm/llm/LlmDisabledException.java`
- `cvm-llm-gateway/src/main/java/com/ahs/cvm/llm/InvalidLlmOutputException.java`
- `cvm-llm-gateway/src/main/java/com/ahs/cvm/llm/InjectionRiskException.java`
- `cvm-llm-gateway/src/main/java/com/ahs/cvm/llm/LlmGatewayConfig.java`
- `cvm-llm-gateway/src/main/java/com/ahs/cvm/llm/AiCallAuditService.java`
- `cvm-llm-gateway/src/main/java/com/ahs/cvm/llm/LlmClientSelector.java`
- `cvm-llm-gateway/src/main/java/com/ahs/cvm/llm/audit/AiCallAuditPort.java`
- `cvm-llm-gateway/src/main/java/com/ahs/cvm/llm/injection/InjectionDetector.java`
- `cvm-llm-gateway/src/main/java/com/ahs/cvm/llm/validate/OutputValidator.java`
- `cvm-llm-gateway/src/main/java/com/ahs/cvm/llm/rate/LlmRateLimiter.java`
- `cvm-llm-gateway/src/main/java/com/ahs/cvm/llm/cost/LlmCostCalculator.java`
- `cvm-llm-gateway/src/main/java/com/ahs/cvm/llm/prompt/PromptTemplate.java`
- `cvm-llm-gateway/src/main/java/com/ahs/cvm/llm/prompt/PromptTemplateLoader.java`
- `cvm-llm-gateway/src/main/java/com/ahs/cvm/llm/adapter/ClaudeApiClient.java`
- `cvm-llm-gateway/src/main/java/com/ahs/cvm/llm/adapter/OllamaClient.java`
- `cvm-llm-gateway/src/main/resources/cvm/llm/prompts/assessment.propose.st`

### AI-Services
- `cvm-ai-services/pom.xml` (persistence-Abhaengigkeit)
- `cvm-ai-services/src/main/java/com/ahs/cvm/ai/audit/JpaAiCallAuditAdapter.java`

### Docs
- `docs/konzept/llm-gateway-invarianten.md`
- `docs/20260417/iteration-11-plan.md`
- `docs/20260417/iteration-11-fortschritt.md`
- `docs/20260417/iteration-11-test-summary.md`
