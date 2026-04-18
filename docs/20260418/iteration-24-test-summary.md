# Iteration 24 - Test-Summary

## Backend

`./mvnw -T 1C test` -> **BUILD SUCCESS** (475 Tests,
11 Docker-skipped, unveraendert gegenueber Iteration 23).
Keine Backend-Aenderungen in dieser Iteration.

## Frontend

`npx ng lint` -> **All files pass linting**.
`npx ng build` -> **Application bundle generation complete**
(11.8 s, 2.05 MB initial - bekannte Budget-Warnung, keine
Regression).

### Neue Karma-Specs (in CI mit Headless-Chrome auszufuehren)

- `core/auth/role-menu.service.spec.ts` (10 Tests):
  Viewer, Approver, Reviewer, Profile-Author,
  Rule-Approver, Reporter, AI-Auditor, Admin,
  keine Rolle, `hasAccess` pro Pfad.
- `core/theme/theme.service.spec.ts` (4 Tests):
  init setzt `data-theme`, toggle wechselt, persistiert
  in `localStorage`, liest persistenten Modus beim Start.

Lokale Ausfuehrung scheitert weiterhin an fehlendem
Headless-Chrome in der Sandbox; die Specs kompilieren
sauber via `ng build`. Offen: Karma-Lauf in CI.

## Architektur-/Linting-Invarianten

- `npx ng lint` bleibt gruen (unveraenderte Regeln).
- Keine neuen zirkulaeren Imports.
- `ThemeService` injiziert `DOCUMENT` ueber die
  Angular-DI, kein direkter `document`-Zugriff im
  Produktionscode -> testfreundlich.
