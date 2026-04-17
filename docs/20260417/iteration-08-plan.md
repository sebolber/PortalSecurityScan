# Iteration 08 – Bewertungs-Queue-UI – Plan

**Jira**: CVM-17
**Abhaengigkeiten**: 06 (Backend-Workflow), 07 (Shell)
**Ziel**: Produktive Queue-UI: Filter, Tabelle, Detail-Panel, Shortcuts,
Vier-Augen-Indikator, optimistische Updates.

## Annahmen und pragmatische Entscheidungen

1. **State-Management**: Angular Signals. Kein NgRx, kein RxJS-Store.
2. **Detail-Panel**: Slide-In rechts als CSS-Transition; kein
   Material-Dialog (bessere A11y und Fokus-Kontrolle mit Tab-Trap).
3. **Shortcuts**: eigene Direktive `QueueShortcutsDirective`, die auf
   `window`-Keyboard-Events lauscht. Input-Fokus (`<input>`/`<textarea>`)
   schluckt Shortcuts, damit Editierfelder nicht gekapert werden.
4. **Polling**: 60 s via `setInterval`, Cleanup via `DestroyRef`.
5. **Vier-Augen-Indikator**: Lokale Heuristik. Downgrade zu
   `NOT_APPLICABLE`/`INFORMATIONAL` triggert Banner + Button-Label
   `Zur Zweitfreigabe einreichen`. Das Backend wirft bei gleichem
   approverId ohnehin `AssessmentFourEyesViolationException`.
6. **Backend bleibt unveraendert**: Endpunkte (`GET /api/v1/findings`,
   `POST /api/v1/assessments/{id}/{approve|reject}`) sind da.
   Optimistische UI kennt die Fehlerkanaele.
7. **Produkt-/Umgebungs-Auswahl**: Shell liefert fuer diese Iteration
   noch keine Daten; Filter-Sidebar benutzt freie Text-/UUID-Felder
   und eine Klammer-Liste "zuletzt benutzt" aus `localStorage`. Eine
   saubere Produktauswahl folgt mit Iteration 19 (Dashboard-Reports).
8. **Batch-Aktionen**: Multi-Select mit Checkbox und
   `Approve-with-edits`-Button aus der Toolbar, der die Einzel-Endpunkte
   sequentiell aufruft. Kein Backend-Bulk-Endpoint (Scope laut Prompt:
   UI vorbereitet).

## Komponenten und Dateien

- `cvm-frontend/src/app/features/queue/`
  - `queue.component.ts` – Shell-Komponente (existiert, wird ersetzt).
  - `queue-filter.ts` – Typen.
  - `queue.types.ts` – DTOs (`QueueEntry`, `ApproveCommand`,
    `RejectCommand`, `QueueFilter`).
  - `queue-api.service.ts` – HTTP-Client (wraps ApiClient).
  - `queue-store.ts` – Signal-Store (Liste, Auswahl, Filter, Loading,
    Pending-Actions, optimistische Updates).
  - `queue-shortcuts.directive.ts` – j/k/a/o/r/?.
  - `queue-shortcut-help.component.ts` – Overlay fuer `?`.
  - `queue-detail.component.ts` – Slide-In-Panel.
  - `queue-filter-sidebar.component.ts` – Filter-Bedienung.
  - `queue-table.component.ts` – Tabellenansicht.
  - `vier-augen-hinweis.component.ts` – Banner.
- Tests:
  - `queue-store.spec.ts`
  - `queue-api.service.spec.ts`
  - `queue-shortcuts.directive.spec.ts`
  - `queue.component.spec.ts` (Smoke + a11y)

## Test-Strategie

1. **TDD**: erst `queue-store.spec.ts` + `queue-api.service.spec.ts`
   + `queue-shortcuts.directive.spec.ts`, dann Produktionscode.
2. **Karma-Specs** kompilieren ueber `ng build` + `ng test`-Konfig.
3. **Playwright** wird nicht in der Sandbox ausgefuehrt; Spec-Datei
   `queue.spec.ts` als Skizze in `cvm-frontend/e2e/` (nur wenn
   existierendes E2E-Setup vorhanden). **Entscheidung**: Da bisher
   kein `e2e/`-Ordner existiert, wird Playwright aufgeschoben und in
   `offene-punkte.md` markiert.

## Nicht in dieser Iteration

- Produktauswahl aus echten Daten (Iteration 19).
- Screenshot-Tests (Playwright nicht aufgesetzt).
- Clustering (Iteration 13).

## Definition of Done (mapping)

- [x] Plan geschrieben.
- [ ] Queue sichtbar (Shell bietet Route schon).
- [ ] Aktionen funktional gegen Backend.
- [ ] Vier-Augen-Flow.
- [ ] Shortcuts + Help.
- [ ] Karma-Specs geschrieben (CI-Lauf dokumentiert als "pending
      Headless-Chrome").
- [ ] Fortschrittsbericht.
- [ ] Commit + Push.
