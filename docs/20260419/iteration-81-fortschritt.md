# Iteration 81 - Fortschritt: Workflow-CTAs Runde 2 (U-01b)

**Jira**: CVM-321

## Was wurde gebaut

Vier Workflow-Kanten aus dem Role-Menu-Workflow sichtbar gemacht:

| Von | Nach | Button | Seite |
|---|---|---|---|
| Reachability | Fix-Verifikation | "Zur Fix-Verifikation" | `reachability.component.html` |
| Fix-Verifikation | Waiver | "Zur Waiver-Verwaltung" | `fix-verification.component.html` |
| Anomalie | Waiver | "Zur Waiver-Verwaltung" | `anomaly.component.html` |
| Waiver | Berichte | "Zum Hardening-Bericht" | `waivers.component.html` |

Jede Komponente importiert `RouterLink` (standalone imports).
Buttons sitzen rechts im Page-Header, konsistent zum Refresh-
Button der jeweiligen Seite.

## Tests (TDD)

Vier neue Karma-Specs, je mit einem Case pro Seite:

- `reachability.component.spec.ts` - Link nach `/fix-verification`.
- `fix-verification.component.spec.ts` - Link nach `/waivers`.
- `anomaly.component.spec.ts` - Link nach `/waivers`.
- `waivers.component.spec.ts` - Link nach `/reports`.

## Ergebnisse

- `npx ng lint` -> "All files pass linting."
- `npx ng build` -> Bundle OK.
- Karma: 105 Tests SUCCESS (4 neu).
- `./mvnw -T 1C test` -> BUILD SUCCESS (Backend unveraendert).

## Migrations / Deployment

- Keine Flyway, keine Dependency, keine Backend-Aenderung.
