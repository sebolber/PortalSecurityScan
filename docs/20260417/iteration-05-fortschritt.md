# Iteration 05 – Fortschrittsbericht

**Jira**: CVM-14
**Datum**: 2026-04-17
**Ziel**: Deterministische Regel-Engine (Cascade-Stufe 2) inkl. Dry-Run.

## 1 Was wurde gebaut

### Domain (`cvm-domain`)
- Neue Enums `RuleOrigin` (MANUAL, AI_EXTRACTED) und
  `RuleStatus` (DRAFT, ACTIVE, RETIRED).

### Persistenz (`cvm-persistence`)
- Entity `Rule` mit Condition-JSON (TEXT), Rationale-Template,
  `rationale_source_fields` (jsonb) und Audit-Spalten fuer
  Vier-Augen-Freigabe.
- Entity `RuleDryRunResult` mit `conflicts` als JSONB-Liste.
- Repositories mit ein paar Finder-Queries plus `FindingRepository`
  erhielt `findByDetectedAtBetween` fuer den Dry-Run.
- Flyway `V0009__regel_engine.sql` (V0008 war durch Iteration 04 belegt).

### Anwendungsschicht (`cvm-application/rules`)
Komponenten:
| Klasse | Zweck |
|---|---|
| `ConditionNode` (sealed) | AST mit `LogicAll`, `LogicAny`, `LogicNot`, `Comparison`. |
| `ConditionParser` | JSON-DSL &rarr; AST; strenge Pfadpraefix-Pruefung. |
| `PathResolver` | Aufloesung `cve.*`, `profile.*`, `component.*`, `finding.*` auf `JsonNode`. |
| `RuleEvaluator` | Wertet AST gegen `RuleEvaluationContext` aus. |
| `RationaleTemplateInterpolator` | Ersetzt `{path}`-Tokens; unbekannte Tokens bleiben markiert. |
| `RuleEngine` | Iteriert alle ACTIVE-Regeln, liefert ersten Treffer als `ProposedResult`. |
| `RuleService` | CRUD, Vier-Augen-Freigabe (`FourEyesViolationException`). |
| `DryRunService` | Laeuft Regel gegen Findings im Zeitraum, schreibt `RuleDryRunResult`. |
| `RuleView` | Read-Model fuer Controller. |

### Cascade (`cvm-application/cascade`)
- `CascadeInput`, `CascadeOutcome`, `CascadeService`.
- Reihenfolge exakt wie in Konzept v0.2 Abschnitt 4.3: REUSE &rarr; RULE
  &rarr; (AI-Platzhalter, Iteration 13) &rarr; HUMAN.
- Service schreibt nichts &mdash; Iteration 06 bindet das an die
  Persistenz an.

### REST (`cvm-api/rules`)
- `RulesController`:
  - `GET  /api/v1/rules`
  - `POST /api/v1/rules` (`@PreAuthorize("hasRole('CVM_ADMIN')")`)
  - `POST /api/v1/rules/{id}/activate` (Vier-Augen-Prinzip)
  - `POST /api/v1/rules/{id}/dry-run?days=180`
- DTOs: `RuleCreateRequest`, `RuleActivateRequest`, `RuleResponse`,
  `DryRunResponse` (inkl. `ConflictEntry`).
- `RulesExceptionHandler` liefert 400/404/409 mit deutschsprachigen
  Meldungen.
- `RulesTestApi` als Slice-Config fuer den WebMvcTest.

### Operator-Tabelle

| Operator | Werttyp | Beispiel |
|---|---|---|
| `eq` / `ne` | beliebig (skalar) | `{"eq": {"path": "cve.kev", "value": true}}` |
| `gt` / `lt` | numerisch | `{"gt": {"path": "cve.epss", "value": 0.5}}` |
| `between` | numerisch `[low, high]` inklusiv | `{"between": {"path": "cve.epss", "value": [0.1, 0.9]}}` |
| `in` | value ist Array | `{"in": {"path": "component.pkgType", "value": ["maven", "npm"]}}` |
| `containsAny` | actual ist Array | `{"containsAny": {"path": "cve.cwes", "value": ["CWE-79", "CWE-89"]}}` |
| `matches` | Regex auf Text | `{"matches": {"path": "cve.description", "value": "(?i)billion.*laughs"}}` |
| `all` / `any` | Logik-Kombinatoren | s. Beispiel-Regel |
| `not` | Negation | `{"not": {"eq": {"path": "...", "value": true}}}` |

## 2 Was laenger dauerte
- `condition_json` als JSONB mit String-Inhalt geht in Hibernate 6 nicht
  reibungslos. Entscheidung: **TEXT**-Spalte, freies JSON. Damit kein
  Serialisierungs-Voodoo und das Feld bleibt fuer kuenftige JSONB-Queries
  umkonvertierbar (bewusst Backlog).
- Der `PathResolver` muss zwischen `profile.*` (JsonNode-Baum) und den
  getypten CVE-/Component-Snapshots unterscheiden. Loesung: komplette
  Normalisierung auf `JsonNode`, damit der Evaluator immer die gleiche
  Quelle sieht.
- `MissingNode` vs. `NullNode`: Beide gelten im Evaluator als abwesend,
  um deutsche Fehlermeldungen zu sparen; `eq:null` ist nie true.
- `@WebMvcTest` + bestehendes `TestApiApplication`: wie in Iteration 04
  ein Slice-Config (`RulesTestApi`) ins Test-Paket gelegt.

## 3 Abweichungen vom Prompt
1. **Flyway-Nummer**: Prompt nennt `V0008`; die Profil-Iteration belegt
   V0008. Diese Iteration nutzt **V0009**.
2. **Cascade-Outcome "MANUAL"**: Prompt beschreibt die Stufe als "Mensch
   entscheidet"; ich mappe das auf `ProposalSource.HUMAN`, damit die
   bestehende Enum-Stufe passt.
3. **Condition-JSON als TEXT statt JSONB**, s.o. Dokumentiert in
   `offene-punkte.md` fuer spaeteren JSONB-Switch.

## 4 Entscheidungen fuer Sebastian
- Sollen Regeln **immutable-versioniert** sein (analog Assessment/Profil)
  oder soll `RETIRED` reichen? Aktuell reicht das fuer den Stand.
- Soll der Dry-Run die Profil-Historie (Profil zum Zeitpunkt des
  Findings) beruecksichtigen? Aktuell nimmt der Service das aktuell
  aktive Profil je Umgebung; Verfeinerung ist Backlog, sobald Profil-
  Zeitreihen relevant werden.
- Welche Rolle soll Dry-Run starten duerfen? Aktuell `CVM_ADMIN`.

## 5 Naechster Schritt
**Iteration 06 &mdash; Bewertungs-Workflow** (CVM-15). Verdrahtet
CascadeService mit Persistenz (PROPOSED-Assessment erzeugen), Queue,
Vier-Augen-Approve, SMTP-Alerts-Event.

## 6 Build-Status

```
./mvnw -T 1C test  BUILD SUCCESS
```

- cvm-domain `EnumTest`: 4/4
- cvm-persistence ITs (Docker): 6 geskippt
- cvm-application (Iteration 05 neu): **31/31** in `rules`, plus
  `CascadeServiceTest` 3/3. Gesamt `cvm-application`: 62/62.
- cvm-integration WireMock-Tests: 8/8
- cvm-api (Iteration 05 neu): `RulesControllerWebTest` 5/5. Gesamt: 15/15.
- cvm-app ITs (Docker): 5 geskippt
- cvm-architecture `ModulgrenzenTest`: 7/7

**Gesamt: 107 Tests, 96 gruen, 11 geskippt ohne Docker, 0 rot.**

---

*Autor: Claude Code.*
