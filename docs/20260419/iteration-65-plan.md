# Iteration 65 - Plan: Karma-Specs stabilisieren

**Jira**: CVM-302

## Ausgangslage

Der volle Karma-Lauf (`npx ng test --watch=false
--browsers=ChromeHeadlessNoSandbox`) nach Iteration 64 zeigte:

- 1 Failure (`RoleMenuService > Einstellungen steht am Ende und
  haelt die Konfigurations-Unterpunkte`) - die erwartete
  Kinderliste war nicht mit den in Iterationen 41 / 56 / 61
  hinzugekommenen Eintraegen `admin-parameters`, `admin-tenants`,
  `admin-cve-import` synchron.
- 2 Warnungen "Spec has no expectations":
  - `QueueApiService > list mit Filter serialisiert
    Query-Parameter` - benutzt `expectOne(predicate)`, das keine
    Jasmine-Erwartung zaehlt.
  - `AiAuditService > liste() mit Filter -> status und useCase
    werden angehaengt` - gleiche Ursache mit String-Matcher.

Hinweise aus `docs/20260419/iteration-61-test-summary.md` zu
Queue-/Filter-/Shortcut-Template-Specs auf `mat-*`-Selektoren sind
heute schon obsolet: die aktuellen Specs benutzen nativeElement /
Store-APIs ohne Material-Selektoren; Grep nach `mat-` in
`src/app/**/*.spec.ts` liefert keine Treffer. Kein
Migrations-Bedarf dort.

## Massnahmen

1. `role-menu.service.spec.ts`: Kinderliste auf 9 Eintraege
   aktualisieren (`admin-parameters`, `admin-tenants`,
   `admin-cve-import` ergaenzt).
2. `queue-api.service.spec.ts`: im Predicate-Match zusaetzlich
   `expect(req.request.method)` + `expect(urlWithParams).toContain`
   setzen (5 Jasmine-Expectations statt 0).
3. `ai-audit.service.spec.ts`: `expect(req.request.method)` +
   `expect(req.request.urlWithParams).toBe(...)` im Filter-Fall
   ergaenzen (2 Erwartungen statt 0).

## Definition of Done

- `npx ng test --watch=false --browsers=ChromeHeadlessNoSandbox`
  → alle 91 Tests SUCCESS, keine Warnungen "has no expectations".
- `npx ng lint` → gruen.
- Keine Backend-Aenderung, kein neues Modul.
