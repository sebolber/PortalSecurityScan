# Iteration 44 - Fortschritt

**Thema**: Parameter-Store-Lesepfad Teil 2
(FixVerificationConfig, RuleExtractionConfig, AlertConfig,
AssessmentConfig, AnomalyConfig) + `restartRequired`-Marker.

**Jira**: CVM-94.

## Was gebaut wurde

- `*Effective()`-Methoden (Resolver &rarr; Boot-Default) auf:
  - `FixVerificationConfig` (enabled, fullTextCommitCap,
    cacheTtlMinutes) + Clamp-Tests (Minimum 5).
  - `RuleExtractionConfig` (enabled, windowDays, clusterCap,
    overrideReviewThreshold) + Clamp-Tests.
  - `AlertConfig` (dryRun, t1, t2, fromAddress). T2 wird auf T1+1
    angehoben, wenn der Override sonst ungueltig waere.
    `dryRun()` akzeptiert jetzt sowohl `"real"` als auch `"live"` als
    Live-Modus (konsistent mit dem SELECT im Katalog: dry-run/log/live).
  - `AssessmentConfig` (defaultValidMonths).
  - `AnomalyConfig` (enabled, kevEpssThreshold, manyAcceptRisk,
    similarRejection, useLlmSecondStage).
- **`restartRequired`-Flag** als zusaetzlicher Parameter am
  `SystemParameterCatalogEntry` (Convenience-Konstruktor mit Default
  `false` ergaenzt, damit bestehende Block-A-Eintraege unveraendert
  funktionieren).
- **Markierte Keys** (Boot-zementiert):
  - LLM-Claude/Ollama/OpenAI/Embedding Modell- und Versions-Keys
    sowie Timeout-Seconds.
  - `cvm.llm.rate-limit.*` (Bucket4j).
  - `cvm.pipeline.gate.per-minute` (Bucket4j).
  - `cvm.security.cors.allowed-origins`.
  - Alle vier Scheduler-Crons.
- **`SystemParameterView`** um `restartRequired` erweitert. Das Flag
  wird beim Mapping aus dem `SystemParameterCatalog` gelesen
  (tenantunabhaengig, haengt am Bean-Code).
- **Frontend**:
  - `SystemParameterView.restartRequired?: boolean` in
    `system-parameter.service.ts`.
  - Neue Chip "Neustart noetig" (color=warn) in der Parameter-
    Tabelle von `admin-parameters.component.html`.
- **Tests**: 5 neue Effective-Tests + Catalog-Test fuer
  `restartRequired` + `SystemParameterViewTest` (4 Faelle).

## Build

- `./mvnw -T 1C -pl cvm-application -am test` &rarr; 311 Tests
  gruen.
- `./mvnw -T 1C -pl cvm-ai-services -am test` &rarr; 130 Tests gruen.
- `./mvnw -T 1C -pl cvm-app -am test` &rarr; 147 Web-Tests gruen,
  Testcontainers geskippt.
- `npx ng build` &rarr; ok (initial bundle ueber Budget &mdash;
  ist bereits als "Bundle-Budget-Reduktion" als eigener offener
  Punkt getrackt).
- `npx ng lint` &rarr; All files pass linting.

## Vier Leitfragen (Oberflaeche)

Die neue Chip "Neustart noetig" ist im Flags-Chipset neben "Pflicht/
Sensibel/Hot" sichtbar. Bedeutung per Tooltip ("Aenderung erfordert
Neustart der Anwendung"). Admins sehen damit auf den ersten Blick,
welche Keys nicht ohne Restart wirksam werden.

## Was bewusst offen bleibt

- Migration der Client-Callsites (`ClaudeApiClient` etc.) auf die
  `*Effective`-Methoden: der Restart-Marker macht dies heute
  unnoetig. Bei Bedarf in einer Folge-Iteration umstellen
  (Bean-Lazy-Builds).
- AES-GCM-Secrets: Iteration 45.
- ArchUnit + E2E: Iteration 46.
