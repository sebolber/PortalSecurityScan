# Iteration 08 – Test-Summary

## Backend (`./mvnw -T 1C test`)

- BUILD SUCCESS.
- Gesamtzahl: 148 Tests (7 Module mit Tests; Summen pro Modul:
  Domain 4, Persistence 13, Application 96, Integration 8, API 22,
  App 5 (Docker-skip), Architecture-Tests 8).
- Docker-gebundene Slice-Tests (App-Modul) bleiben wie in Iteration 07
  dokumentiert skipped (`@EnabledIf DockerAvailability#isAvailable`).

Keine Backend-Aenderung in dieser Iteration → Zahlen identisch zum
Stand 8e0698d.

## Frontend

### Statische Pruefungen
- `npx ng build` → gruen. Initial 1.91 MB (472 kB transfer);
  Lazy-Chunks einschliesslich `queue-component` (~42 kB). Bundle-Budget-
  Warnung (Initial > 1.05 MB) besteht seit Iteration 07.
- `npx ng lint` → `All files pass linting`.

### Karma-Specs (geschrieben, CI-Lauf dokumentiert als nachzuholen)

| Spec | Beschreibung | Tests |
|------|--------------|-------|
| `queue-api.service.spec.ts` | HTTP-Wrapper (Liste ohne/mit Filter, approve, reject) | 4 |
| `queue-store.spec.ts` | Sortierung, Severity-Filter, Navigation, reload Fehlerpfad, optimistic approve/reject/rollback, Checkbox-Batch | 7 |
| `queue-shortcuts.directive.spec.ts` | j/k/a/o/r/?, Input-Schutz, Modifier-Schutz | 5 |
| `vier-augen.spec.ts` | Zweitfreigabe-Heuristik | 5 |
| `queue.component.spec.ts` | Smoke-Render der Page | 1 |

In der Sandbox steht kein Headless-Chrome zur Verfuegung
(`No binary for ChromeHeadless`). `ng test` laeuft erst in CI; die
Specs sind kompatibel zum Build und werden vom Compiler in
`ng build`-Konfig und `tsconfig.spec.json` erfasst.

## Coverage (Queue-Feature)

Manuell bewertet:
- `QueueStore`: alle public-Methoden (reload, approve, reject,
  moveSelection, toggleChecked, clearChecked, setFilter,
  toggleSeverityFilter, resetFilter) sind durch Specs abgedeckt.
- `QueueApiService`: alle Methoden inklusive Query-Parameter-Build.
- `QueueShortcutsDirective`: alle Keys und Ignorierlogik.
- `vier-augen.ts`: alle Zweige (Zielwert-Match, Downgrade-Vergleich,
  fehlendes Original).

Aussagekraeftige Metrik folgt, sobald Headless-Chrome im CI haengt
(siehe offene-punkte.md "Karma + Playwright in CI").

## Playwright

Nicht eingerichtet in Iteration 08 (Sandbox ohne Chromium). Die
Queue-Szenarien (Happy-Path, Override, Reject, Vier-Augen, Konflikt)
sind als Kandidaten in `offene-punkte.md` vermerkt.
