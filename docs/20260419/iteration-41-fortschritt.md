# Iteration 41 - Fortschritt

**Thema**: Parameter-Katalog Block A.1 (AI_LLM-Fallbacks,
AI_REACHABILITY, RAG, ANOMALY, COPILOT).

**Jira**: CVM-91.

## Was gebaut wurde

- `cvm-application/parameter/SystemParameterCatalogEntry` -
  Deklarative Datenstruktur fuer Katalog-Eintraege.
- `cvm-application/parameter/SystemParameterCatalog` - Statische
  Liste aller Block-A.1-Schluessel mit Label, Beschreibung,
  Default-Wert, Typ, Optionen und Flags. Default-Werte sind 1:1
  mit den `@Value`-Fallbacks in `LlmGatewayConfig`,
  `ReachabilityConfig`, `AnomalyConfig`, `AutoAssessmentConfig`,
  `ClaudeApiClient`, `OllamaClient`, `OpenAiCompatibleClient`,
  `OllamaEmbeddingClient` abgeglichen.
- `cvm-application/parameter/SystemParameterCatalogBootstrap` -
  `ApplicationReadyEvent`-Listener, der pro aktivem Mandant fehlende
  Katalog-Eintraege anlegt. Idempotent: bestehende Werte und Keys
  werden unveraendert gelassen.

## Was bewusst NICHT in dieser Iteration kam

- Secrets (`cvm.llm.claude.api-key`, `cvm.feed.nvd.api-key`,
  `cvm.feed.ghsa.api-key`, `cvm.ai.fix-verification.github.token`,
  `cvm.encryption.sbom-secret`) - folgen in Iteration 45 mit
  AES-GCM-Verschluesselung.
- Zugriffs-Wrapper `getEffective(...)` in den `*Config`-Beans -
  folgen Iteration 43/44.
- ArchUnit-Regel + E2E-Test - folgt Iteration 46.
- ENRICHMENT/RATE_LIMIT/PIPELINE_GATE/MAIL/SCAN/SCHEDULER/SECURITY -
  Iteration 42.

## Hinweise fuer den naechsten Start

- Keine Flyway-Migration, keine Dependency-Aenderung. Ein erneutes
  `./mvnw -T 1C clean install -DskipTests` vor `scripts/start.sh` ist
  **nicht** noetig.

## Vier Leitfragen (Oberflaeche)

Keine UI-Aenderung in dieser Iteration. Der bestehende
`admin-parameters`-Screen zeigt die neuen Keys nach dem ersten Start
automatisch an (Seeding beim `ApplicationReadyEvent`).

## Offene Punkte

- Verifizierung in einer vollen PostgreSQL-Umgebung, dass der
  Bootstrap idempotent bleibt, wenn mehrere Anwendungsinstanzen
  parallel starten (sollte durch das `unique (tenant_id, param_key)`-
  Constraint abgesichert sein; Race-Condition-Test folgt mit
  Iteration 46 End-to-End-Test).
