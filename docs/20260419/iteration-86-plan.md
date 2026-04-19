# Iteration 86 - Plan: Vier-Augen-Warnung im Queue-Detail (U-04a)

**Jira**: CVM-326

## Ziel

Heute blockiert das Backend den Approve mit HTTP 409, wenn der
Approver == Autor. Das UI signalisiert das erst nach dem Klick.
Diese Iteration warnt schon *vor* dem Klick.

## Umfang

`queue-detail.component`:
- Neuer `get selbstfreigabeKonflikt()`: liefert `true`, wenn
  `auth.username()` == `entry.decidedBy`. Nur relevant fuer
  Findings mit einem bereits-gestellten Vorschlag.
- Template: rote Banner-Box oberhalb des Approve-Buttons
  ("Du hast den Vorschlag bereits gestellt. Vier-Augen-Prinzip
  verlangt, dass eine andere Person freigibt.").
- Approve-Button wird zusaetzlich zur bestehenden Pending-Logik
  disabled, wenn `selbstfreigabeKonflikt` true ist. Button-Text
  aendert sich zu "Freigabe durch andere Person erforderlich".

## Tests

- `queue-detail.component.spec.ts` (neu): drei Cases.
  - Ohne Konflikt -> Banner nicht da, Button enabled.
  - Mit Konflikt -> Banner da, Button disabled.
  - Entry-Wechsel: Konflikt wird korrekt neu evaluiert.

## Abnahme

- `ng lint` / `ng build` / Karma gruen.
