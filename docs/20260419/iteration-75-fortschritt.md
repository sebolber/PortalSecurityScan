# Iteration 75 - Fortschritt: OSV-Mirror Reload-Button

**Jira**: CVM-312
**Datum**: 2026-04-19

## Was wurde gebaut

- Neuer `OsvMirrorService` in
  `cvm-frontend/src/app/core/osv-mirror/osv-mirror.service.ts`
  mit `reload()`-Methode ueber `ApiClient.post`. Typisiert auf
  `OsvMirrorReloadResponse { reloaded, indexSize }`.
- `AdminCveImportComponent` erweitert:
  - Service-Injection + Signals `osvReloadLaedt`,
    `osvReloadErgebnis`, `osvReloadFehler`.
  - Methode `osvMirrorNeuLaden()` ruft `OsvMirrorService.reload()`.
    HTTP 503 (`osv_mirror_inactive`) wird als Warnhinweis
    angezeigt, sonstige Fehler als Freitext.
  - Template-Card mit Button + Loading-State + Banner +
    Ergebnisanzeige.
- Karma-Spec `osv-mirror.service.spec.ts`:
  - 200 -> `reloaded=true`, `indexSize=42`.
  - 503 -> Promise rejected, `HttpErrorHandler.show` wird
    gerufen.

## Ergebnisse

- `npx ng lint` -> "All files pass linting."
- `npx ng build` -> Bundle OK.
- Karma (`**/osv-mirror/**/*.spec.ts`) 2 Tests PASS.
- Backend unveraendert.

## Migrations / Deployment

- Keine Flyway-Migration, keine neuen Dependencies.
