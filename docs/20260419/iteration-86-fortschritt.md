# Iteration 86 - Fortschritt: Vier-Augen-Warnung im Queue-Detail (U-04a)

**Jira**: CVM-326

## Was wurde gebaut

`queue-detail.component.ts`:
- Neuer `get selbstfreigabeKonflikt()` - prueft
  `auth.username() === entry.decidedBy`.
- Rote `banner-critical`-Box oberhalb des Approve-Buttons bei
  Konflikt (Text: "Du hast diesen Vorschlag bereits gestellt.
  Vier-Augen-Prinzip verlangt eine andere Person mit Approver-
  Rolle.").
- Approve-Button ist zusaetzlich zu `pending` disabled bei
  Konflikt; Button-Text wechselt zu "Freigabe durch andere
  Person erforderlich".

## Tests

`queue-detail.component.spec.ts` (neu, 3 Cases):
- kein Konflikt → Banner nicht da, Button enabled.
- Konflikt → Banner da, Button disabled.
- Entry-Wechsel → Konflikt wird korrekt re-evaluiert.

## Ergebnisse

- Karma: 126 Tests SUCCESS.
- `ng lint` / `ng build` / `mvnw test` gruen.
