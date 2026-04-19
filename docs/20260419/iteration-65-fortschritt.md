# Iteration 65 - Fortschritt: Karma-Specs stabilisiert

**Jira**: CVM-302
**Datum**: 2026-04-19

## Was wurde gebaut

- `role-menu.service.spec.ts`: Kinderliste von 6 auf 9 Eintraege
  aktualisiert; Iteration-41 (`admin-parameters`), Iteration-56
  (`admin-tenants`) und Iteration-61 (`admin-cve-import`)
  nachgezogen.
- `queue-api.service.spec.ts` (Case "list mit Filter"): zusaetzliche
  explizite `expect`-Aufrufe auf `req.request.method` und
  `req.request.urlWithParams`, damit Jasmine den Test als
  expectation-fuehrend zaehlt.
- `ai-audit.service.spec.ts` (Case "liste() mit Filter"): analog
  `expect(req.request.method)` + `expect(req.request.urlWithParams)`
  ergaenzt.

## Ergebnisse

- `npx ng test --watch=false --browsers=ChromeHeadlessNoSandbox`
  → 91 Tests, 0 Failures, 0 "has no expectations"-Warnungen.
- `npx ng lint` → "All files pass linting."

## Kein weiterer Migrations-Bedarf

Die Sammel-Warnung aus `iteration-61-test-summary.md` zu
`mat-*`-Selektoren in Feature-Specs ist heute nicht mehr
anwendbar. Grep nach `mat-` in `cvm-frontend/src/app/**/*.spec.ts`
liefert null Treffer; alle Specs arbeiten bereits mit nativem DOM,
Store-APIs oder Jasmine-Spies.
