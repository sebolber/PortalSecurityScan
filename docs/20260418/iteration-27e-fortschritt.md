# Iteration 27e - Folge-Arbeit (Reachability + Fix-Verification)

**Jira**: CVM-65
**Branch**: `claude/ui-theming-updates-ruCID`
**Stand**: 2026-04-18

Folge-Arbeitsblock auf derselben Branch nach 27d. Ziel: die letzten
beiden Platzhalter (Reachability, Fix-Verifikation) durch echte
Basis-Sichten ersetzen und dafuer die noetigen Read-Endpunkte
erganzen.

## Umgesetzt

### Backend: Fix-Verifikations-Uebersicht

- `MitigationPlanRepository` um
  `findAllByOrderByCreatedAtDesc(Pageable)` und
  `findByVerificationGradeOrderByCreatedAtDesc(...)` ergaenzt.
- Neues Paket `application.fixverification`:
  - `FixVerificationSummaryView` (Record): Mitigation + Grade-
    Felder ohne JPA-Entity-Durchleiten.
  - `FixVerificationQueryService`: `recent(limit)` und
    `byGrade(grade, limit)`, Default 50, Max 500.
- Neuer Controller `FixVerificationQueryController` unter
  `/api/v1/fix-verification`. Optionaler Query-Param `grade`
  (A/B/C/UNKNOWN). Der bestehende FixVerificationController
  `/api/v1/mitigations/...` bleibt unveraendert.
- Tests: `FixVerificationQueryServiceTest` (3 Tests),
  `FixVerificationQueryControllerWebTest` (2 Tests).

### Backend: Reachability-Uebersicht

- `AiSuggestionRepository` um
  `findByUseCaseOrderByCreatedAtDesc(useCase, Pageable)` ergaenzt.
- Neues Paket `application.reachability`:
  - `ReachabilitySummaryView` (Record).
  - `ReachabilityQueryService` mit Use-Case `"REACHABILITY"`,
    `recent(limit)`, Default 50, Max 500.
- Neuer Controller `ReachabilityQueryController` unter
  `/api/v1/reachability`. Der bestehende ReachabilityController
  `/api/v1/findings/{id}/reachability` (POST) bleibt
  unveraendert.
- Tests: `ReachabilityQueryServiceTest` (2 Tests),
  `ReachabilityQueryControllerWebTest` (1 Test).

### Frontend: Fix-Verifikation Basis-Ansicht

- `core/fix-verification/fix-verification.service.ts` kapselt
  `GET /api/v1/fix-verification`.
- `features/fix-verification/fix-verification.component.*`
  (Component rewrite gegenueber 27d-Placeholder):
  - MatButtonToggleGroup-Filter `ALL / A / B / C / UNKNOWN`.
  - Material-Tabelle (Erstellt, Strategie, Status,
    Ziel-Version, Grade-Chip, Verifiziert-Datum).
  - Grade-Chip-Paletten: A=LOW, B=MEDIUM, C=HIGH, UNKNOWN=NA,
    alles ueber Severity-Tokens.
  - Tokens fuer Spacing, Radius, Typografie.

### Frontend: Reachability Basis-Ansicht

- `core/reachability/reachability.service.ts` kapselt
  `GET /api/v1/reachability`.
- `features/reachability/reachability.component.*`:
  Material-Tabelle mit Zeit, Severity-Chip (Tokens), Status
  (`PROPOSED`-Standard), Begruendung, Confidence (2 Nachkomma-
  stellen) und Finding-ID.

### Test-Konsequenz

- Die neuen und bestehenden `*ControllerWebTest` im selben
  Paket (`fixverify`, `reachability`) laden dieselbe
  `TestApi`-ComponentScan. Durch das Hinzufuegen eines weiteren
  Controllers entstehen wechselseitige Autowire-Erwartungen.
  Loesung: bestehende Tests bekommen einen @MockBean auf den
  jeweils anderen Service (FixVerificationQueryService bzw.
  ReachabilityQueryService), die neuen Tests einen @MockBean
  auf FixVerificationService bzw. ReachabilityAgent.

## Coverage-Matrix

`frontend-backend-coverage.md` aktualisiert:

- Reachability: `PLATZHALTER` -> `ANGEBUNDEN`.
- Fix-Verifikation: `PLATZHALTER` -> `ANGEBUNDEN`.
- **Keine Platzhalter mehr**: Die Definition-of-Done-Zeile
  "Kein Menuepunkt in der Sidebar fuehrt auf eine komplett
  leere Seite" aus Iteration 27 ist damit erfuellt.

## Build / Verifikation

- `./mvnw -T 1C test` -> BUILD SUCCESS. 325 Tests gesamt
  (+6 neu in dieser Session: 3 FixVerificationQueryService,
  2 Controller, 2 ReachabilityQueryService, 1 Controller).
- `npx ng lint cvm-frontend` -> All files pass linting.
- `npx ng build cvm-frontend` -> Application bundle
  generation complete.

## Offene Punkte (27f+)

- Vollstaendige Migration aller Feature-SCSS auf Tokens
  (Reports, Rules, Profile, Settings, AI-Audit stehen noch
  mit gemischten Werten).
- Stylelint-Guard im CI scharfschalten
  (`.stylelintrc.cvm27.json` existiert als Stub).
- `FullNavigationWalkThroughTest` + axe-core-Lauf in
  Playwright (Sandbox-bedingt noch offen).
- Detail-Seiten pro Reachability/Fix-Verifikation-Eintrag
  (CallSites, Evidence-Type-Details, Rollback-Link zum
  Finding).
