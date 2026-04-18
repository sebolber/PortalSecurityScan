# Iteration 33 – Plan

**Thema**: PURL &rarr; CVE-Matching beim Scan-Ingest ueber OSV
**Jira**: CVM-77
**Datum**: 2026-04-18

## Hintergrund

Der aktuelle `ScanIngestService` entstammt aus dem SBOM ausschliesslich
`vulnerabilities`-Eintraege. Ein Build-Tool wie `cyclonedx-maven-plugin`
oder `cyclonedx-npm` liefert aber nur `components` - entsprechend bleibt
`findingCount=0`, obwohl im Repo offensichtlich verwundbare npm-Pakete
liegen (siehe `npm install`-Ausgabe mit 55 vulnerabilities im
PortalCore-Frontend).

## Ziel

Nach erfolgreichem Scan-Ingest wird pro `ComponentOccurrence` zusaetzlich
**Open Source Vulnerabilities (OSV)** nach Treffern gefragt. Funde
landen als Findings im Store, CVE-IDs werden fuer die bestehende
Anreicherung (NVD/GHSA/KEV/EPSS) registriert.

OSV ist gewaehlt, weil:
- keine API-Keys noetig
- Batch-Endpoint (`/v1/querybatch`) unterstuetzt PURL direkt
- kennt npm, maven, pypi, go, nuget, cargo, crates, packagist nativ
- liefert CVE-Aliase in den Advisories, so dass wir sie mit der
  bestehenden `Cve`-Tabelle verknuepfen koennen

## Scope

### Neu

- `cvm-integration/src/main/java/com/ahs/cvm/integration/osv/`
  - `OsvComponentLookup` - PURL-basierte Batch-Abfrage
  - `OsvProperties` - Config-Binding
- `cvm-application/src/main/java/com/ahs/cvm/application/cve/`
  - Port-Interface `ComponentVulnerabilityLookup` (Hexagonal-Grenze)
  - Neuer Listener
    `ComponentCveMatchingOnScanIngestedListener` (laeuft nach dem
    bestehenden `CveEnrichmentOnScanIngestedListener`).
- Spring-Config:
  ```yaml
  cvm:
    enrichment:
      osv:
        enabled: false         # Opt-In, damit Offline-Setups nichts tun
        base-url: https://api.osv.dev
        batch-size: 500        # OSV akzeptiert bis 1000 Queries/Request
        timeout-ms: 15000
  ```
- Feature-Flag-Default **false** fuer Testbarkeit und Prod-Entscheidung.

### Geaendert

- `ScanIngestedEvent` wird bereits gefeuert - reicht aus, keine Aenderung.
- `FindingRepository` bekommt `existsByScanIdAndComponentOccurrenceIdAndCveId`,
  damit der Listener nicht doppelt einfuegt.

## Test-Disziplin (TDD verbindlich)

### Unit

- `OsvComponentLookupTest`:
  - liefert `List<String>` CVE-IDs bei Batch-Treffer
  - leere Liste bei HTTP 200 ohne Vulns
  - leere Liste bei HTTP 404
  - wirft `FeedClientException` bei 5xx
  - respektiert Batch-Size (splittet bei grossen SBOMs)
- `ComponentCveMatchingOnScanIngestedListenerTest`:
  - ruft Lookup pro `ComponentOccurrence` auf, die einen PURL hat
  - erzeugt `Cve` + `Finding` pro Treffer
  - Dedup: Zweiter Aufruf erzeugt keine Duplikate
  - ueberspringt Occurrences ohne PURL
  - broker-Fail (`FeedClientException`) wird geloggt, kein Abbruch

### Integration

- `OsvComponentLookupIntegrationTest` (Testcontainers nicht noetig,
  aber `MockRestServiceServer`): echter RestClient gegen canned Response.
- `ScanIngestOsvMatchingIntegrationTest` mit Spring Boot Test Slice -
  Scan wird ingestiert, Mock-OSV liefert eine CVE, ein Finding landet
  in der DB.

### ArchUnit

- `application` darf `integration` nicht sehen.
- Listener ruft nur Ports aus `cvm-application`, nicht direkt den
  Integration-Client - `ComponentVulnerabilityLookup` lebt in
  `application`, Implementierung in `integration`.

## Stopp-Kriterien

- `cvm.enrichment.osv.enabled` darf im Default `false` bleiben, sonst
  schlaegt jede Test-Umgebung auf OSV raus.
- Kein Hard-Fail bei OSV-Outage: Lookup liefert im Fehlerfall
  `List.of()`, Listener loggt und fertig.
- `FindingRepository` darf fuer Dedup keine full-table-scan-Query
  hinzubekommen - prefix-Index ueber `(scan_id, component_occurrence_id, cve_id)`
  existiert bereits (Iteration 04), reicht aus.

## Liefergrad

- `docs/20260418/iteration-33-fortschritt.md` + `test-summary.md`
- Config-Beispiel in `cvm-app/src/main/resources/application.yaml`
  (Opt-In mit Kommentar)
- Release-Hinweis: Wenn aktiviert, braucht das CVM Internet-Zugriff
  auf `api.osv.dev` - fuer air-gapped Installationen muss ein
  OSV-Mirror gestellt werden (spaetere Iteration).
