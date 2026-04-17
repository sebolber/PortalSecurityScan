# Iteration 05 – Deterministische Regel-Engine

**Jira**: CVM-14
**Abhängigkeit**: 03, 04
**Ziel**: Cascade-Stufe 2 implementieren: Regeln matchen Findings gegen Kontextprofile
und erzeugen `PROPOSED`-Assessments mit nachvollziehbarer Begründung.

---

## Kontext
Konzept v0.2 Abschnitt 4.3 (Cascade) und Anhang B (Beispielregel). Regeln sind
JSON-DSL, Origin kann `MANUAL` oder `AI_EXTRACTED` sein (letzteres erst ab
Iteration 17 relevant, Feld jetzt schon anlegen).

## Scope IN
1. Flyway `V0008__regel_engine.sql`: `rule`, `rule_condition` (optional als
   jsonb in `rule`), `rule_dry_run_result`.
2. Regel-Modell (domain): `Rule`, `RuleCondition`, `ProposedResult`,
   `RuleEvaluationContext`.
3. Condition-DSL:
   - Operatoren: `eq`, `ne`, `containsAny`, `matches` (regex),
     `in`, `gt`, `lt`, `between`.
   - Logik: `all`, `any`, `not`.
   - Pfade: `cve.description`, `cve.cwes`, `cve.kev`, `cve.epss`,
     `profile.<feld>`, `component.pkgType`, `component.name`.
4. `RuleEngine`-Service mit Methoden `evaluate(finding, profile)` →
   `Optional<ProposedResult>` und `dryRun(rule, timeRange)` →
   `DryRunResult`.
5. Cascade-Service (Grundgerüst, wird in Iteration 06 in den Workflow
   eingehängt): `CascadeService.bewerte(finding) → CascadeOutcome` mit
   Reihenfolge REUSE → RULE → (Platzhalter AI) → MANUAL.
6. REST-Endpunkte:
   - `GET /api/v1/rules`
   - `POST /api/v1/rules` (nur `CVE_ADMIN`, Regel entsteht inaktiv)
   - `POST /api/v1/rules/{id}/activate` (Vier-Augen-Prinzip)
   - `POST /api/v1/rules/{id}/dry-run?days=180`

## Scope NICHT IN
- Regel-Extraktion per KI (Iteration 17).
- UI (Iteration 08).
- Schreib-Wirkung aus Cascade (Iteration 06 verbindet alles).

## Aufgaben
1. Condition-Parser: JSON → interne AST; klare Fehlertexte bei ungültiger
   Pfadangabe.
2. Evaluator läuft ohne Reflection auf fertig gebauten Mini-Datamodellen
   (Pfad-Resolver kapselt den Zugriff auf `cve`, `profile`, `component`).
3. Rationale-Template: `"keine Windows-Plattform im Einsatz (Profil: {profile.version})"` –
   Templatierung via kleinen eigenen Interpolator (keine full-blown Template-Engine).
4. Dry-Run-Query:
   - iteriert über historische `Finding`s im Zeitraum,
   - prüft, wie viele der später `APPROVED`-ten Assessments die Regel
     vorausgesagt hätte, Konflikte erkennen (Regel widerspricht bereits
     aktivierter).

## Test-Schwerpunkte
- `ConditionParserTest`: alle Operatoren, verschachtelte `all`/`any`/`not`.
- `RuleEvaluatorTest`: Happy-Path, Path-Not-Found, Type-Mismatch.
- `DryRunTest`: synthetische Historie mit 20 Assessments, Regel matcht 14,
  Coverage-Rate korrekt.
- Cascade-Test: REUSE-Treffer bricht sofort ab, Regel-Treffer wird nur
  ausgeführt, wenn REUSE negativ.
- `@DisplayName`: `@DisplayName("Cascade: bei REUSE-Treffer wird Regel-Engine nicht mehr angefragt")`.

## Definition of Done
- [ ] Regel-CRUD funktioniert.
- [ ] Aktivierung nur mit Vier-Augen-Prinzip.
- [ ] Dry-Run liefert Coverage-Rate und Konfliktliste.
- [ ] Cascade-Service REUSE → RULE fertig (AI-Stufe ist Platzhalter, der
      `Optional.empty()` zurückgibt).
- [ ] Coverage `cvm-application/rules` ≥ 90 % (Kernlogik).
- [ ] Fortschrittsbericht.
- [ ] Commit: `feat(rules): Deterministische Regel-Engine mit Cascade-Integration\n\nCVM-14`

## TDD-Hinweis
Die Regel-Engine ist der Kern der Revisionsfestigkeit. Schreibe umfangreiche
Parser- und Evaluator-Tests zuerst. **Ändere NICHT die Tests** bei Rot.

## Abschlussbericht
Standard, plus Liste der implementierten Operatoren als Tabelle im Bericht.
