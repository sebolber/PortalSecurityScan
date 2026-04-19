# Iteration 35 – Plan

**Thema**: OSV Rate-Limit und Retry-After
**Jira**: CVM-79
**Datum**: 2026-04-19

## Ziel

Der OSV-Client soll ueber 429-Antworten nicht stolpern:
- Wenn OSV mit `HTTP 429 Too Many Requests` antwortet, liest der
  Client den `Retry-After`-Header (Sekundenwert), wartet kurz und
  setzt den Call genau einmal neu ab.
- Optional: harte Client-Seite-Throttle (`max-requests-per-minute`)
  als weiche Stromkabelsicherung, Default aus.

## Scope

- Erweiterung `OsvProperties` um
  - `maxRetryAfterSeconds` (Default 30) - Obergrenze fuer das Warten.
  - `retryOn429` (Default `true`).
- `OsvComponentLookup` faengt im `abfragen` und
  `resolveAliasesFromDetail` eine `HttpClientErrorException` auf
  HTTP-429 ab. Er liest den `Retry-After`-Header (nur Sekunden-
  Variante) und wartet `Math.min(retryAfterSec, maxRetryAfterSeconds)`
  Sekunden, bevor genau einmal retried wird.
- Spring hat dafuer `RestClient`+`ResponseErrorHandler` oder besser
  `onStatus(...)`-Handler. Ich nehme `onStatus` fuer 429, weil das
  die `response.getHeaders()` direkt in die Hand gibt.
- Der Retry verbraucht genau einen weiteren Call; schlaegt er
  wieder fehl (egal ob 429 oder 5xx), landet das Ergebnis als
  leere Map mit Warnung im Log.

## Tests

- `OsvComponentLookupTest`
  - `retryAfter429WirdEinmaligWiederholt`: erste Antwort 429 mit
    `Retry-After: 0`, zweite Antwort 200 + Body; Map gefuellt.
  - `retryAfterZweimal429GibtLeereMap`: 429 gefolgt von 429 =>
    leere Map, keine Exception.
  - `retryAfterLimit`: `Retry-After: 999` wird durch
    `maxRetryAfterSeconds` auf den konfigurierten Maximalwert
    beschnitten (der Test misst nicht die Zeit, sondern prueft,
    dass der Retry durchlaeuft).

## Stopp-Kriterien

- Kein neues Thread-Blocken ueber 30 Sekunden.
- Bestehende Tests bleiben gruen.
- Keine externen Abhaengigkeiten.
