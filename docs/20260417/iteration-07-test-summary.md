# Iteration 07 – Test-Summary

**Stand**: 2026-04-17.

## Build & Lint
- `npx ng build --configuration development` &rarr; SUCCESS
  (Initial 5.73 MB, lazy chunks pro Feature).
- `npx ng lint` &rarr; **alle Dateien gruen** (nach Ergaenzung von
  `@typescript-eslint/parser` und `prefix: ['cvm', 'ahs']`).
- Backend (`cvm-architecture-tests`) `./mvnw test` &rarr; SUCCESS,
  bestaetigt dass die Iteration-06-Aenderungen unveraendert tragen.

## Karma
- `ng test --browsers=ChromeHeadless --watch=false` schlaegt fehl mit
  `No binary for ChromeHeadless`. **Sandbox bietet keinen Browser**;
  Karma-Lauf muss in CI nachgeholt werden.
- Geschriebene Specs (kompilieren sauber via `ng build`):

| Spec | Faelle | Zweck |
|---|---|---|
| `RoleMenuServiceTest` | 6 | Rollen &rarr; Menue-Sichtbarkeit. |
| `AppConfigServiceTest` | 2 | Lazy-Load + sync `get()`-Wache. |
| `AuthInterceptorTest` | 3 | Bearer-Header, Asset-Schutz, 401-Logout. |
| `ApiClientTest` | 3 | URL-Bildung, Happy-Path, Errorhandler-Trigger. |
| `AppComponentTest` (Bestand) | 1 | Smoke. |

## Coverage
Karma-`coverage`-Reporter konnte nicht laufen (siehe oben). Sobald
Headless-Chrome verfuegbar ist:

```
npx ng test --browsers=ChromeHeadless --watch=false --code-coverage
```

## Smoke-Manuell
- Browser-Smoke gegen `npm start`: nicht ausgefuehrt (Sandbox ohne
  X11/Chrome). `index.html` wird durch `ng build` ohne Fehler gebaut,
  Bundle-Aufteilung sieht plausibel aus.
- E2E (Playwright) bleibt offen, war im Prompt als "nice-to-have"
  markiert.

## Naechste Test-Schritte
- ChromeHeadless in CI (z.&nbsp;B. `karma-chrome-launcher` mit
  `ChromeHeadlessNoSandbox`) verdrahten und Coverage gegen das
  70-%-Ziel pruefen.
- Playwright-Smoke gegen Keycloak-Dev-Realm (Login &rarr; Dashboard
  sichtbar &rarr; Logout) mit Iteration 08 nachreichen.
- Visual-Regression-Test auf den Shell-Header (optional).
