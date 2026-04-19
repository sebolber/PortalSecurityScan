# Iteration 42 - Parameter-Katalog (Block A.2)

## Ziel

Die in Iteration 41 begonnene Katalog-Befuellung mit den restlichen
Block-A-Kategorien abschliessen:

- `ENRICHMENT` (OSV + Feed-Quellen NVD, GHSA, KEV, EPSS; **ohne**
  `base-url`/`api-key`)
- `PIPELINE_GATE` (Rate-Limit pro Minute, MR-Kommentar-Flag)
- `MAIL` (CVM-Alerts, **ohne** `spring.mail.*`)
- `SCAN` (Assessment-Laufzeit, KI-Workload-Flags wie Summary,
  NL-Query, Rule-Extraction, Fix-Verification, Profile-Assistant)
- `SCHEDULER` (globales Flag und Cron-Ausdruecke)
- `SECURITY` (CORS-Origins)

Secrets (`api-key`, `token`, `sbom-secret`) bleiben wie in
Iteration 41 ausgespart und werden in Iteration 45 gesondert mit
AES-GCM eingepflegt.

## Vorgehen

1. Katalog-Eintraege in `SystemParameterCatalog` erweitern. Default-
   Werte 1:1 mit den `@Value`-Fallbacks bzw. den Defaults in
   `OsvProperties` und `FeedProperties.FeedConfig` abgleichen.
2. Neue Kategorie-Konstanten (`CATEGORY_ENRICHMENT`,
   `CATEGORY_PIPELINE_GATE`, `CATEGORY_MAIL`, `CATEGORY_SCAN`,
   `CATEGORY_SCHEDULER`, `CATEGORY_SECURITY`).
3. Test-Erweiterung in `SystemParameterCatalogTest`:
   - Vorhandensein der neuen Keys.
   - Defaults gegen die `@Value`-Fallbacks.
   - Typkonsistenz (Cron &rarr; STRING, boolean &rarr; BOOLEAN,
     Integer &rarr; INTEGER).
   - `sensitive=false` weiterhin fuer alle Eintraege.
4. Bootstrap-Test bleibt unveraendert (skaliert mit der groesseren
   Liste).

## Nicht-Ziele

- Wrapper / `getEffective(...)`: Iteration 43/44.
- AES-GCM-Secrets: Iteration 45.
- ArchUnit + E2E: Iteration 46.

## Testerwartung

- `./mvnw -T 1C -pl cvm-application -am test` &rarr; BUILD SUCCESS.

## Jira

`CVM-92` - System-Parameter-Katalog Block A.2.
