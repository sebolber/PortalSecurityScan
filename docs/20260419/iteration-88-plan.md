# Iteration 88 - Plan: Reachability-Detail + findingId-Deep-Link (U-05)

**Jira**: CVM-328

## Ziel

Ein Klick auf eine Zeile in `/reachability` oeffnet ein Slide-In
mit den Details (Rationale, Confidence, Status, Severity,
Finding-ID, Zeit). Deep-Link
`/reachability?findingId=<uuid>` oeffnet den Start-Dialog
vorbefuellt.

## Umfang

### Component
- `reachability.component.ts`:
  - Neues `selected = signal<ReachabilitySummaryView | null>(null)`.
  - Handler `auswaehlen(row)` und `schliessen()`.
  - `ngOnInit` liest `findingId` aus queryParamMap; wenn gesetzt
    und Feature verfuegbar, oeffnet Start-Dialog vorbefuellt.

### Template
- Tabellen-Zeilen werden klickbar (Button-artig mit cursor-
  pointer), rufen `auswaehlen(row)`.
- Seitliches Slide-In-Panel (vergleichbar queue-detail) mit den
  Detaildaten.

## Tests

- `reachability.component.spec.ts`:
  - Zeile anklicken setzt `selected()`.
  - queryParam `findingId` -> vorbefuelltes Signal `neueFindingId`.

## Nicht-Umfang

- Finding-ID-Autocomplete, Feature-Flag-Banner bleiben offene
  Punkte (eigener Endpoint fehlt bzw. Autocomplete-Service).

## Abnahme

- `ng lint`/`ng build`/Karma gruen.
