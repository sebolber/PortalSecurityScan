# Iteration 82 - Plan: Queue-Filter-URL-Persistenz + Status-Chips (U-02a)

**Jira**: CVM-322

## Ziel

Queue-Filter ueberlebt Page-Reload und URL-Sharing. Zusaetzlich
erweiterter Status-Filter als Chip-Gruppe (bisher Select mit nur
zwei Werten).

## Umfang

### Status-Chips in der Filter-Bar

Neue Chip-Gruppe ersetzt das bisherige Select:
- ALLE (Filter entfernt, Backend-Default greift: PROPOSED + NEEDS_REVIEW)
- PROPOSED
- NEEDS_REVIEW
- APPROVED
- REJECTED
- EXPIRED

### URL-Persistenz

`queue.component.ts`:
- Effect: `this.store.filter()` → `Router.navigate` mit queryParams
  fuer `productVersionId`, `environmentId`, `status`, `source`.
  `queryParamsHandling: 'merge'`, `replaceUrl: true`. Nur wenn
  der URL-Stand vom Filter abweicht.
- Bestehender queryParams-Effect (Iteration 80) bleibt; der
  neue Effect darf keinen Loop triggern, daher Vergleich vor
  `navigate`.

### Filter-Reset

"Filter zuruecksetzen" ruft `store.resetFilter()`. Der
URL-Effect navigiert dann zu `/queue` ohne queryParams.

## TDD

- `queue.component.spec.ts`: neuer Case "Filter-Aenderung
  navigiert zu URL mit queryParams".
- `queue-filter-bar.component.spec.ts` (neu): die Status-Chips
  rendern und reagieren auf Klicks (mit einem lokalen
  QueueStore).

## Nicht-Umfang

- `severityIn` bleibt Client-only und wandert nicht in die URL
  (kommt in einer Folge-Iteration). In der URL-Persistenz nur
  Server-Filter abbilden, damit der Einstieg ueber einen
  Deep-Link wirklich identisch laedt.

## Abnahme

- `ng lint`/`ng build`/Karma gruen.
- Backend unveraendert.
