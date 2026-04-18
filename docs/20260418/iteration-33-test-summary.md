# Iteration 33 – Test-Summary

**Jira**: CVM-77
**Stand**: 2026-04-18 (Umsetzung) / 2026-04-19 (Verifikation)

## Backend

`./mvnw -T 1C test` → **BUILD SUCCESS** auf allen 9 Modulen.

### Neue Tests

| Modul | Klasse | Tests |
|---|---|---|
| cvm-application | `ComponentCveMatchingOnScanIngestedListenerTest` | 4 |
| cvm-integration | `OsvComponentLookupTest` | 5 |

Insgesamt +9 Tests. Keine bestehenden Tests angefasst.

### Architektur

- `ModulgrenzenTest`: gruen. `OsvComponentLookup` ist in
  `cvm-integration`, nutzt das Port-Interface
  `ComponentVulnerabilityLookup` aus `cvm-application` -
  keine verbotenen Durchgriffe.
- `SpringBeanKonstruktorTest`: gruen.

## Frontend

Keine Aenderungen in dieser Iteration.

## Verhaltenstest (optional)

Gegen das laufende CVM mit `CVM_OSV_ENABLED=true`:

```bash
curl -X POST http://localhost:8081/api/v1/scans \
  -H "Authorization: Bearer <token>" \
  -F "productVersionId=<UUID>" \
  -F "scanner=cyclonedx-npm" \
  -F "sbom=@frontend/bom.json"

# Nach 5-15 Sekunden:
curl http://localhost:8081/api/v1/scans/<scan-id> \
  -H "Authorization: Bearer <token>" | jq .findingCount
# erwartet: > 0
```
