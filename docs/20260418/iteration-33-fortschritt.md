# Iteration 33 – Fortschritt

**Thema**: PURL → CVE-Matching beim Scan-Ingest ueber OSV
**Jira**: CVM-77
**Datum**: 2026-04-18

## Umgesetzt

### Port (cvm-application)

- Neues Interface
  [`ComponentVulnerabilityLookup`](../../cvm-application/src/main/java/com/ahs/cvm/application/cve/ComponentVulnerabilityLookup.java)
  mit Methoden `isEnabled()` und `findCveIdsForPurls(List<String>)`.
  Liefert pro PURL eine Liste CVE-IDs; Netzfehler geben leere Maps
  zurueck, damit der Scan-Ingest nicht blockiert.
- Neuer Listener
  [`ComponentCveMatchingOnScanIngestedListener`](../../cvm-application/src/main/java/com/ahs/cvm/application/cve/ComponentCveMatchingOnScanIngestedListener.java):
  haengt sich an `ScanIngestedEvent`, sammelt alle PURLs der
  Component-Occurrences, ruft den Port, upsertet `Cve`-Eintraege
  (Source `OSV`) und speichert `Finding`-Eintraege - mit Dedup-Query.

### Persistenz (cvm-persistence)

- `FindingRepository` bekommt
  `existsByScanIdAndComponentOccurrenceIdAndCveCveId`. Nutzt den
  bestehenden Index aus Iteration 04, kein neuer Migration-Schritt noetig.

### Integration (cvm-integration)

- Neues Paket `integration/osv/`:
  - [`OsvProperties`](../../cvm-integration/src/main/java/com/ahs/cvm/integration/osv/OsvProperties.java):
    `cvm.enrichment.osv.{enabled,base-url,batch-size,timeout-ms}`.
  - [`OsvComponentLookup`](../../cvm-integration/src/main/java/com/ahs/cvm/integration/osv/OsvComponentLookup.java):
    implementiert den Port via `POST /v1/querybatch`. Chunkt die
    PURL-Liste in `batch-size`-Pakete (Default 500), extrahiert CVE-IDs
    und CVE-Aliase aus den Advisories. Kein Throw - Netz-/Protokoll-
    Fehler werden geloggt und als leere Maps zurueckgegeben.

### Konfiguration (cvm-app)

- `application.yaml` bringt den Block `cvm.enrichment.osv` mit
  sprechenden Env-Variablen (`CVM_OSV_ENABLED`, `CVM_OSV_BASE_URL`,
  `CVM_OSV_BATCH_SIZE`, `CVM_OSV_TIMEOUT_MS`). Default `enabled=false`.

## Tests

- `ComponentCveMatchingOnScanIngestedListenerTest` (+4 Tests, alle gruen):
  - skip bei deaktiviertem Lookup
  - Happy-Path (Cve-Upsert + Finding-Save)
  - Dedup bei vorhandenem Finding
  - Skip von Occurrences ohne PURL
- `OsvComponentLookupTest` (+5 Tests, alle gruen):
  - `isEnabled`-Flagge
  - Happy-Path via `MockRestServiceServer`
  - Leere Response
  - 5xx-Fehler -> leere Map
  - GHSA-Alias-Extraction

`./mvnw -T 1C test` → **BUILD SUCCESS** (9 Module, ArchUnit gruen).

## Wie testet man das lokal

1. OSV aktivieren:
   ```bash
   CVM_OSV_ENABLED=true ./mvnw -pl cvm-app spring-boot:run
   ```
2. SBOM ohne `vulnerabilities`-Section hochladen (z. B. das aus
   Iteration 32c demonstrierte `cyclonedx-npm`-BOM).
3. Im Scan-Status (`GET /api/v1/scans/{id}`) erscheint nach ein paar
   Sekunden `findingCount > 0`. Die Log-Zeile
   `"OSV: N neue Findings in Scan {} angelegt"` bestaetigt das Match.
4. Die nun entstandenen CVE-IDs durchlaufen automatisch den bestehenden
   `CveEnrichmentOnScanIngestedListener` und bekommen NVD/GHSA/KEV-Daten.

## Offene Punkte

- **Rate-Limit gegenueber OSV**: Aktuell keine Backoff-Strategie. OSV ist
  grosszuegig, aber fuer grosse SBOMs (>3000 Komponenten) sollte ein
  leichtes `Retry-After`-Respect-Feature in einer naechsten Iteration
  nachgerueckt werden.
- **PURL-Aliase**: Manche Generatoren schreiben `pkg:maven/...` mit
  und ohne `qualifier=type:jar`. OSV matcht eher die kanonische Form.
  Falls Trefferquote in Produktion zu gering ausfaellt: Canonicalize-
  Helper einbauen.
- **Offline-Installationen**: Air-gapped-Umgebungen brauchen einen
  OSV-Mirror (z. B. `gcr.io/osv-dev/osv-v1` self-hosted). Das wird in
  Iteration 34 skizziert.
