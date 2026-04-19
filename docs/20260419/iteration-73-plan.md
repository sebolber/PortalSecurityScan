# Iteration 73 - Plan: OSV-Mirror Reload-Endpunkt

**Jira**: CVM-310

## Ziel

Der file-basierte OSV-Mirror aus Iteration 72 laedt die JSONL-
Datei beim Boot einmal. Fuer air-gapped-Setups braucht es einen
Admin-Endpunkt, der den Index ohne Neustart neu aufbaut, sobald
der Sidekick-Prozess die Datei aktualisiert hat.

## Umfang

- Neuer `OsvMirrorAdminController` in
  `cvm-api/.../api/admin`:
  - `POST /api/v1/admin/osv-mirror/reload`
  - Nur Rolle `CVM_ADMIN`.
  - Nur aktiv, wenn der `OsvJsonlMirrorLookup`-Bean im Context
    ist (via `Optional<OsvJsonlMirrorLookup>`-Injection, da er
    unter `@ConditionalOnProperty` steht).
  - Liefert ein Map-Body mit `reloaded=true/false` und der neuen
    `indexSize`.
- `@ControllerAdvice` nicht noetig (fehlender Bean -> 503 mit
  klarer Message).
- MockMvc-Test pro Endpunkt:
  - Mirror aktiv + reload erfolgreich -> HTTP 200 + JSON.
  - Mirror inaktiv -> HTTP 503 mit `error=osv_mirror_inactive`.

## Nicht-Umfang

- Kein UI-Button (wird spaeter in einem Admin-Panel ergaenzt).
- Kein Refresh-Scheduler; der Trigger ist manuell.

## Abnahme

- `./mvnw -T 1C test` gruen, ArchUnit gruen.
