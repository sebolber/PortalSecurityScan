# Iteration 75 - Plan: OSV-Mirror Reload-Button im Admin-UI

**Jira**: CVM-312

## Ziel

Der Admin-Endpunkt aus Iteration 73
(`POST /api/v1/admin/osv-mirror/reload`) hat keinen UI-Button.
Admin-Nutzer muessten heute cURL bemuehen. Diese Iteration
haengt den Button an die bestehende Air-Gapped-Admin-Seite
(`/admin/cve-import`) an.

## Umfang

- Neuer Service `OsvMirrorService` in
  `core/osv-mirror/osv-mirror.service.ts` - duenner Wrapper um
  `ApiClient.post('/api/v1/admin/osv-mirror/reload', {})`.
- Einbindung in `AdminCveImportComponent` (selbe Route, weil
  thematisch air-gapped):
  - Neues Card-Widget "OSV-Mirror" mit Erklaerungstext und
    Button "Mirror neu laden" (Lucide-Refresh-Icon).
  - Fehlerbehandlung: 503 -> Banner mit Hinweis, dass der
    Mirror nicht aktiv ist; andere Fehler -> Freitext-Meldung.
  - Erfolgreicher Reload -> Toast + Anzeige der neuen
    `indexSize`.
- Spec-Tests fuer den Service (200, 503).

## Nicht-Umfang

- Keine neue Route, kein neuer Menueintrag.
- Kein Auto-Reload-Scheduler im Frontend.

## Abnahme

- `npx ng lint` / `npx ng build` gruen.
- Karma (`**/osv-mirror/**/*.spec.ts`) gruen.
- Backend bleibt unveraendert.
