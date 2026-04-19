# Iteration 83 - Plan: Filter-URL-Persistenz CVE/KPI/FixVerif (U-02b)

**Jira**: CVM-323

## Ziel

Filter-State der Uebersichts-Seiten ueberlebt Reload und Share.

### CVEs `/cves`
- queryParams: `q`, `severity`, `kev`, `page`.
- Init: `ActivatedRoute.snapshot.queryParamMap` in Komponente.
- Aenderungen (Suche, Severity, KEV-Toggle, Page-Wechsel)
  schreiben per `router.navigate(...)` mit `replaceUrl=true`.

### Fix-Verifikation `/fix-verification`
- queryParams: `grade` (A/B/C/UNKNOWN/ALL).

### Tenant-KPI `/tenant-kpi`
- queryParams: `window` (30d/90d/180d).

## TDD

Karma-Specs pro Seite: Initialisierung aus queryParamMap +
State-Change ruft `router.navigate` mit dem erwarteten Key.

## Nicht-Umfang

- KPI-Presets "Heute"/"Quartal"/"Zeitraum ...": kommen spaeter
  mit einem Date-Range-Picker. Jetzt nur die bestehenden 3
  Zeitfenster persistieren.
- AI-Audit und Alert-History: analoges Pattern, aber
  Folge-Iteration.
