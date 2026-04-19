# Iteration 88 - Fortschritt: Reachability-Detail + Deep-Link (U-05)

**Jira**: CVM-328

## Was wurde gebaut

- `ReachabilityComponent`:
  - Neues `selected`-Signal + `auswaehlen(row)` / `schliessen()`.
  - `ngOnInit` liest `findingId` aus queryParamMap und belegt
    damit `neueFindingId` vor, sodass ein Deep-Link
    `/reachability?findingId=<uuid>` den Start-Dialog sofort mit
    der richtigen Finding-ID oeffnen lassen kann.
- `reachability.component.html`:
  - Tabellen-Zeilen erhalten `cursor-pointer`, hover und
    Auswahl-Markierung; Klick ruft `auswaehlen`.
  - Neues festes `<aside>`-Slide-In rechts mit allen Details
    (Zeit, Status, Severity, Confidence, Finding-ID,
    Begruendung).
- Karma-Cases:
  - findingId aus queryParams setzt `neueFindingId`.
  - Zeilen-Klick setzt `selected` und rendert das Detail-Panel.

## Ergebnisse

- `ng lint` / `ng build` OK.
- Karma 129 Tests SUCCESS (+ 2 neu).
- `./mvnw -T 1C test` BUILD SUCCESS in 01:49 min.
