# Iteration 35 – Fortschritt

**Thema**: OSV Rate-Limit und Retry-After
**Jira**: CVM-79
**Datum**: 2026-04-19

## Umgesetzt

- **`OsvProperties`** um zwei Felder erweitert:
  - `retryOn429` (Default `true`)
  - `maxRetryAfterSeconds` (Default 30)
- **`OsvComponentLookup`**:
  - Neue Helper-Methode `mitRetryAuf429(label, call)`, die einen
    `HttpClientErrorException.TooManyRequests` genau einmal retried.
  - Wartezeit aus `Retry-After`-Header (Sekundenwert) wird durch
    `maxRetryAfterSeconds` gedeckelt. Ohne Header faellt der Wert
    auf 1 s. HTTP-Date-Form wird aktuell als 1 s behandelt.
  - Sowohl der Batch-Call `/v1/querybatch` als auch der Detail-Call
    `/v1/vulns/{id}` nutzen den Retry-Helper.
  - `Sleeper`-FunctionalInterface als Test-Hook, damit Tests nicht
    echte Sekunden warten muessen.
- Startup-Log um `retryOn429` und `maxRetryAfterSeconds` ergaenzt.

## Tests

- `OsvComponentLookupTest` um fuenf Faelle erweitert:
  - `retryAfter429WirdEinmaligWiederholt` (Header `2`).
  - `retryAfter429OhneHeader` (Default 1 s).
  - `retryAfterObergrenze` (Header `999`, Deckel `3` greift).
  - `retryDeaktiviert` (`retryOn429=false` => leere Map).
  - `retryZweimal429LeereMap` (zweiter Versuch auch 429 => leere
    Map, kein throw).

## Test-Status

- `./mvnw -T 1C test`: BUILD SUCCESS.
- Keine Frontend-Aenderungen.

## Offene Punkte

- HTTP-Date-Form im `Retry-After` noch nicht decodiert. OSV nutzt
  laut Doku nur Sekunden; Aufwand derzeit nicht gerechtfertigt.
- Client-Seite-Throttle (`max-requests-per-minute` mit Bucket4j)
  aus dem Plan rausgenommen, weil OSV im normalen Scan-Profil
  deutlich unter den publizierten Limits bleibt. Wenn reale Daten
  ein Throttle verlangen, folgt eine Iteration 35b.
