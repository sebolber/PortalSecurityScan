# Iteration 05 – Arbeitsplan (Regel-Engine)

**Jira**: CVM-14
**Datum**: 2026-04-17
**Branch**: `claude/cve-relevance-manager-57JSm`
**Abhaengigkeit**: Iteration 03 (CVE-Anreicherung), Iteration 04 (Profil).

## Ziel
Deterministische Cascade-Stufe 2: Regeln matchen Findings gegen Kontextprofile
und erzeugen `PROPOSED`-Assessments mit nachvollziehbarer Begruendung. Plus
Dry-Run gegen historische Findings.

## Architektur-Entscheidungen

### Datenmodell
- `Rule` ist **immutable-versioniert** wie Assessment:
  - `DRAFT` &rarr; `ACTIVE` nach Vier-Augen-Freigabe.
  - Neue Regel-Version ersetzt alte per `supersededAt`.
  - Reihenfolge laesst sich spaeter (Iteration 17) aus einem
    `AI_EXTRACTED`-Draft ableiten.
- Status: `DRAFT | ACTIVE | RETIRED`.
- `RuleOrigin`: `MANUAL | AI_EXTRACTED` (letzterer jetzt nur als Enum-Wert).
- Condition als JSONB im Feld `condition_json`.
- `rule_dry_run_result`: pro Lauf ein Eintrag mit
  `total/matched/matched_already_approved/conflicts`.

### Flyway
- Prompt nennt `V0008__regel_engine.sql`; V0008 ist durch Iteration 04
  belegt. Diese Iteration nutzt **V0009**. Inhalt unveraendert.

### Condition-DSL
JSON-Baum, rekursiv:
```json
{ "all": [
  { "eq": { "path": "cve.kev", "value": true } },
  { "not": { "eq": { "path": "profile.architecture.windows_hosts", "value": true } } },
  { "any": [
    { "containsAny": { "path": "cve.cwes", "value": ["CWE-79","CWE-89"] } },
    { "gt": { "path": "cve.epss", "value": 0.5 } }
  ]}
] }
```
Operatoren:
| Op | Semantik |
|---|---|
| `eq` | Gleichheit (Werte-normalisiert: Boolean, Zahl, String). |
| `ne` | Ungleichheit. |
| `containsAny` | Schnittmenge Liste <-> Wert-Liste nicht leer. |
| `matches` | Regex auf String. |
| `in` | Wert in Referenzliste. |
| `gt` / `lt` | Numerischer Vergleich (BigDecimal/Double). |
| `between` | `[low, high]` inklusiv (Zahlen). |
| `all` / `any` / `not` | Logik-Kombinatoren. |

### Pfadaufloesung
`PathResolver` kennt genau drei Praefixe und lehnt andere mit einer
deutschsprachigen `RuleConditionException` ab:
- `cve.{id,description,cwes,kev,epss,cvssScore}`
- `profile.<a>.<b>...` (direkt in den JsonNode des Profils).
- `component.{pkgType,name,version}`

### Rationale-Template
Eigener Interpolator: `{path}`-Tokens werden aus dem
`RuleEvaluationContext` aufgeloest. Unbekannte Tokens bleiben
unveraendert und werden im Audit-Log markiert.

### Cascade
`CascadeService` ruft in Reihenfolge auf:
1. **REUSE** &rarr; `AssessmentLookupService.findeAktiveFreigaben(cve,pv,env)`.
   Treffer &rarr; {@code CascadeOutcome} mit Source REUSE; Abbruch.
2. **RULE** &rarr; `RuleEngine.evaluate(context)`; Treffer &rarr; RULE.
3. **AI** &rarr; Platzhalter, gibt `Optional.empty()`.
4. **MANUAL** &rarr; bleibt Aufgabe des Reviewers &ndash; leerer Outcome.

Der CascadeService **schreibt noch nichts**; Iteration 06 verbindet ihn
mit dem Persistenz-Workflow.

### Dry-Run
- Parameter: `days` (1..3650), default 180.
- Laedt alle `Finding`s im Zeitraum, baut pro Finding einen
  `RuleEvaluationContext` (benoetigt CVE + Component; Profil wird das zum
  Findings-Zeitpunkt aktive Profil sein &ndash; in Iteration 05 vereinfacht:
  das aktuelle ACTIVE-Profil je Umgebung, da wir keine Profil-Historie
  mit `valid_from`-Joins machen wollen, solange es noch keinen Workflow
  gibt).
- Ergebnis: Anzahl Findings, Matches, davon bereits APPROVED als
  Vorhersage-Coverage. Konflikt = Regel schlaegt an, aber vorhandenes
  APPROVED-Assessment hat niedrigere/unpassende Severity.
- Persistiert in `rule_dry_run_result` inkl. `conflicts_json`.

## REST
- `GET    /api/v1/rules`
- `POST   /api/v1/rules` (Rolle `CVM_ADMIN`, legt DRAFT an)
- `POST   /api/v1/rules/{id}/activate` (Vier-Augen, Approver &ne; creator)
- `POST   /api/v1/rules/{id}/dry-run?days=180`

## Reihenfolge (TDD)

1. Tests rot schreiben:
   - `ConditionParserTest` (alle Operatoren + Verschachtelung +
     invalider Pfad).
   - `PathResolverTest` (cve/profile/component, unbekannter Pfad).
   - `RuleEvaluatorTest` (Happy-Path, Path-Not-Found, Type-Mismatch).
   - `RationaleTemplateTest` (mehrere Tokens, fehlender Token).
   - `RuleEngineTest` (evaluate mit komplettem Context, nur ACTIVE-Regeln).
   - `CascadeServiceTest` (REUSE bricht ab, sonst RULE, sonst leer).
   - `RulesControllerWebTest` (GET, POST Draft, POST Activate 4-Augen,
     POST Dry-Run).
2. Persistenz-Entities + Flyway V0009.
3. Domain/Application-Klassen implementieren bis gruen.
4. Controller + DTOs + ExceptionHandler.
5. `./mvnw -T 1C test` &rarr; BUILD SUCCESS.

## Annahmen
- Dry-Run-Zeitraum bezieht sich auf `finding.detected_at`.
- Profil-Lookup im Dry-Run: current ACTIVE profile je Environment.
  Verfeinerung (Profil zum Zeitpunkt des Findings) ist Backlog.
- ArchUnit-Regeln bleiben hart; neuer Code in `com.ahs.cvm.application.rules`
  und `com.ahs.cvm.api.rules`.
