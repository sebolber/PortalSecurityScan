# Iteration 82 - Fortschritt: Queue-Filter-URL-Persistenz + Status-Chips (U-02a)

**Jira**: CVM-322

## Was wurde gebaut

### Status-Chips
`queue-filter-bar.component.ts`: Status-Select durch eine
Chip-Gruppe ersetzt (ALLE / PROPOSED / NEEDS_REVIEW / APPROVED /
REJECTED / EXPIRED). ALLE entspricht `status=undefined`,
serverseitig dann PROPOSED+NEEDS_REVIEW-Default.

### URL-Persistenz
`queue.component.ts`: neuer `effect` schreibt beim
Filter-Wechsel `productVersionId`, `environmentId`, `status`,
`source` in `ActivatedRoute.queryParams` via `Router.navigate`
(replaceUrl=true, queryParamsHandling='merge'). Bestandseffekt
aus Iteration 80 liest die queryParams zurueck; Vergleich mit
`snapshot.queryParamMap` verhindert den Loop.

`severityIn` bleibt absichtlich Client-only: Schwere-Auswahl
wird lokal gefiltert und landet nicht in der URL.

### Reset
`Filter zuruecksetzen` ruft `store.resetFilter()`, der URL-Effect
loescht anschliessend alle queryParams.

## Tests

- `queue.component.spec.ts`:
  - neuer Case "Store-Filter-Aenderung navigiert zur URL".
  - Fake-ActivatedRoute um `snapshot.queryParamMap` erweitert.
- `queue-filter-bar.component.spec.ts`:
  - Drei neue Cases: Status-Chips sind sichtbar,
    APPROVED-Klick setzt Filter, ALLE-Klick setzt
    status=undefined.

## Ergebnisse

- `ng lint` OK.
- `ng build` OK.
- Karma: 109 Tests SUCCESS.
- `./mvnw -T 1C test` BUILD SUCCESS in 01:30 min.

## Migrations / Deployment

- Keine Flyway, keine Backend-Aenderung.
