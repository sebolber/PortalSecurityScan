# Iteration 41 - Parameter-Katalog (Block A.1)

## Ziel

Den neu gebauten System-Parameter-Store mit seinem ersten Schwung
Katalog-Eintraegen befuellen. Abgedeckt werden die Kategorien

- `AI_LLM` (globale Fallbacks, **nicht** `LlmConfiguration`-Entities)
- `AI_REACHABILITY` (Feature-Flag, Timeout, Binary)
- `RAG` (Indexing-Enabled, Top-K, Chunk-Groesse, Fake-Embedding-Flag)
- `ANOMALY` (Feature-Flag, Schwellwerte, LLM-Zweit-Check)
- `COPILOT` (Feature-Flag, Modell-Kategorie)

## Vorgehen

1. **Katalog-Definition** als in-memory Struktur
   (`SystemParameterCatalogEntries`) in `cvm-application/parameter`.
   Pro Eintrag: `paramKey`, `label`, `category`, `subcategory`, `type`,
   `defaultValue`, `options`, `unit`, `required`, `hotReload`,
   `adminOnly`, `sensitive=false` (Secrets kommen in Iteration 45),
   `description`, `handbook`.
2. **Bootstrap-Komponente** `SystemParameterCatalogBootstrap`:
   `ApplicationReadyEvent`-Listener, legt pro aktivem Mandant fehlende
   Katalog-Eintraege an, ohne bestehende Werte zu ueberschreiben.
   Ergebnis: nach dem ersten Start zeigt die `admin-parameters`-Seite
   alle Fallback-Schalter mit Default-Werten.
3. **Unit-Tests**:
   - `SystemParameterCatalogEntriesTest`: Katalog enthaelt die
     erwarteten Keys, Typen und Defaults; keine Dubletten;
     Secrets-Flag auf `false` (AI_LLM-Adapter-Tokens werden erst in
     Iteration 45 migriert).
   - `SystemParameterCatalogBootstrapTest`: bei leerer Tabelle wird
     pro Mandant je ein Eintrag pro Katalog-Schluessel angelegt;
     bereits vorhandene Keys werden nicht doppelt angelegt;
     Wert-Aenderungen eines Admins werden nicht ueberschrieben.
4. **Keine Flyway-Migration** in dieser Iteration: Seeding passiert
   mandanten-spezifisch zur Laufzeit, damit auch nach nachtraeglichem
   Anlegen eines Tenants der Katalog automatisch gefuellt wird.

## Nicht-Ziele

- Zugriffs-Wrapper `getEffective(...)` in den bestehenden
  `*Config`-Beans: folgt in Iteration 43.
- Secret-Behandlung (AES-GCM, Hint-Anzeige): folgt in Iteration 45.
- ArchUnit-Regel und End-to-End-Test: folgt in Iteration 46.
- ENRICHMENT/RATE_LIMIT/PIPELINE_GATE/MAIL/SCAN/SCHEDULER/SECURITY:
  folgen in Iteration 42.

## Testerwartung

- `./mvnw -T 1C -pl cvm-application test` gruen.
- `./mvnw -T 1C -pl cvm-app test` nach Einhaengen des Bootstraps
  gruen (kein bestehender Test kippt, weil der Bootstrap nur auf
  `ApplicationReadyEvent` lauscht).

## Jira

`CVM-91` - System-Parameter-Katalog Block A.1.
