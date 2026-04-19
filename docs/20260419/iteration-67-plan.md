# Iteration 67 - Plan: Feed-Clients auf Parameter-Resolver

**Jira**: CVM-304

## Ziel

Die Feed-Adapter im Modul `cvm-integration` nutzen heute einen
statisch gebauten `FeedProperties`-Snapshot (api-keys inklusive).
Aenderungen am System-Parameter-Store greifen erst nach Neustart.

Migration pro Adapter:

- **NvdFeedClient**: `cvm.feed.nvd.api-key` pro Call aus dem
  `SystemParameterResolver` lesen, Fallback auf
  `FeedProperties.getNvd().getApiKey()`. Base-URL bleibt
  application.yaml (Non-Migrate-Liste).
- **GhsaFeedClient**: `cvm.feed.ghsa.api-key` pro Call.
  `isEnabled()` darf bei leerem Property greifen, solange der
  Resolver einen Wert liefert.
- **KevFeedClient / EpssFeedClient**: kein api-key, keine Call-
  Site-Aenderung noetig. Im Progress-Report als "nichts zu tun"
  dokumentieren.

ArchUnit: `cvm-integration` darf laut `ModulgrenzenTest`
`cvm.application..` kennen; direkte Dependency ist erlaubt.

## TDD

- `NvdFeedClientTest` + `GhsaFeedClientTest` je um einen Case
  "override-aus-resolver-greift-ohne-restart" erweitern.
- `SystemParameterCatalogTest`: `cvm.feed.nvd.api-key` und
  `cvm.feed.ghsa.api-key` in die "live-reloadable"-Gruppe
  einhaengen.

## Katalog

- `cvm.feed.nvd.api-key`: hotReload=true, restartRequired=false.
- `cvm.feed.ghsa.api-key`: hotReload=true, restartRequired=false.

## Abnahme

- Backend `./mvnw -T 1C test` -> BUILD SUCCESS.
- ArchUnit unveraendert gruen.
- Keine Modul-/Dependency-Aenderung in pom-Dateien (cvm-integration
  kennt `cvm-application` schon).
