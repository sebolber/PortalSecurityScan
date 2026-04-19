# Iteration 83 - Fortschritt: CVE/KPI/FixVerif Filter-URL (U-02b)

**Jira**: CVM-323

## Was wurde gebaut

### CVEs `/cves`
- `cves.component.ts` liest beim Init `q`, `severity`, `kev`,
  `page` aus `route.snapshot.queryParamMap`.
- `syncUrl()` schreibt den aktuellen Filter nach
  `router.navigate` mit `replaceUrl=true`. Aufgerufen aus
  `sucheAusloesen`, `nextPage`, `prevPage`.

### Fix-Verifikation `/fix-verification`
- `fix-verification.component.ts` liest `grade` aus queryParams.
- `gradeWechseln` schreibt per `router.navigate` mit
  `queryParams: { grade }` (`null` bei ALL = Param entfernt).

### Tenant-KPI `/tenant-kpi`
- `tenant-kpi.component.ts` liest `window` aus queryParams
  (30d/90d/180d).
- `fensterWechseln` schreibt `window` nach
  `router.navigate` (90d = Default = Param entfernt).

## Tests

- `cves.component.spec.ts` (neu): 2 Cases (Init aus params,
  Severity-Klick navigiert).
- `fix-verification.component.spec.ts`: 2 neue Cases (Init +
  gradeWechseln).
- `tenant-kpi.component.spec.ts` (neu): 2 Cases (Init,
  fensterWechseln).

## Ergebnisse

- `ng lint` OK. `ng build` OK.
- Karma: 115 Tests SUCCESS (6 neu).
- `./mvnw -T 1C test` BUILD SUCCESS in 01:27 min.

## Nicht-Umfang

- KPI-Presets "Heute/Quartal/Zeitraum" + Date-Range-Picker
  (Folge-Iteration).
- AI-Audit/Alert-History-Filter-URL.
