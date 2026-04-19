# Iteration 44 - Parameter-Store-Lesepfad Teil 2

## Ziel

Restliche `*Config`-Beans auf `*Effective()`-Methoden umstellen und den
`restartRequired`-Marker am Katalog einfuehren (fuer Keys, deren
aktueller Wert beim Boot in einen `RestClient.Builder` oder Rate-
Limit-Bucket zementiert wird).

## Vorgehen

1. **`restartRequired`-Flag am Katalog**: `SystemParameterCatalogEntry`
   um den booleschen Parameter erweitern. Defaults: `false`. Gesetzt
   fuer Keys mit bekannter Boot-Time-Verwendung:
   - `cvm.llm.claude.version`
   - `cvm.llm.claude.timeout-seconds`
   - `cvm.llm.claude.model`
   - `cvm.llm.ollama.base-url`
   - `cvm.llm.ollama.model`
   - `cvm.llm.embedding.ollama.base-url`
   - `cvm.llm.embedding.ollama.model`
   - `cvm.llm.openai.default-model`
   - `cvm.llm.rate-limit.global-per-minute`
   - `cvm.llm.rate-limit.tenant-per-minute`
   - `cvm.pipeline.gate.per-minute`
   - `cvm.security.cors.allowed-origins`
   - Cron-Ausdruecke (Spring liest sie beim Bean-Build per
     `@Scheduled(cron=...)`).
2. **`SystemParameterView`** um `restartRequired` erweitern. Der Wert
   stammt aus dem Katalog (nicht aus der DB), wird im
   `SystemParameterService` beim Mapping aufgeloest.
3. **Frontend** (`admin-parameters`): Chip "Neustart noetig" neben
   den anderen Flags.
4. **`*Effective()`-Methoden** in den folgenden Beans:
   - `FixVerificationConfig`
   - `RuleExtractionConfig`
   - `AlertConfig`
   - `AssessmentConfig`
   - `AnomalyConfig`
5. **Tests** (Mockito, unit):
   - Catalog-Test: `restartRequired` ist fuer die markierten Keys
     gesetzt, fuer die anderen nicht.
   - `SystemParameterViewRestartFlagTest` (im
     `SystemParameterServiceTest`-Stil, Mocks).
   - Je ein `*ConfigEffectiveTest` pro Bean.

## Nicht-Ziele

- DB-Migration fuer `restart_required`: bewusst vermieden; der Marker
  lebt im Katalog-Code, nicht in der Tabelle. Begruendung:
  Eigenschaft haengt am Bean-Code, nicht am Mandanten.
- Secret-Migration: Iteration 45.

## Testerwartung

- `./mvnw -T 1C -pl cvm-application,cvm-ai-services -am test` &rarr;
  BUILD SUCCESS.
- `./mvnw -T 1C -pl cvm-app -am test` &rarr; BUILD SUCCESS.

## Jira

`CVM-94` - Parameter-Store-Lesepfad Teil 2 + restartRequired-Marker.
