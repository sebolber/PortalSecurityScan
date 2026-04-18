# Iteration 28c - Rule-Editor in der UI

**Jira**: CVM-67
**Branch**: `claude/ui-theming-updates-ruCID`
**Stand**: 2026-04-18

Rule-Author kann neue Regeln (JSON-DSL) direkt in der UI anlegen,
ohne REST-Client. Das Backend hatte bereits ein `POST /api/v1/rules`
mit DRAFT-Status; die UI deckte das nicht ab. Aktivierung und
Dry-Run waren bereits in der UI vorhanden.

## Umgesetzt

### RulesService

- `core/rules/rules.service.ts` bekommt `create(req)` +
  DTO `RuleCreateRequest` (ruleKey, name, description,
  proposedSeverity, conditionJson, rationaleTemplate,
  rationaleSourceFields, origin, createdBy).

### RulesComponent

- Neue Leiste im Header: **"Neue Regel anlegen"** (Toggle).
- Editor-Card mit Formularfeldern:
  - Rule-Key, Name, Severity-Dropdown (CRITICAL ... NOT_APPLICABLE).
  - Beschreibung (optional).
  - Condition-JSON: Textarea mit Mono-Font, Default-Skelett
    (`allOf` mit einem Beispiel-Term).
  - Rationale-Template + Rationale-Quellfelder (komma-
    getrennt, werden in ein Array umgewandelt).
  - Origin (Default `MANUAL`).
- Client-seitige Pruefungen vor dem Call:
  - `ruleKey` + `name` Pflicht.
  - `conditionJson` muss `JSON.parse`-bar sein.
- Rollengate:
  - `CVM_RULE_AUTHOR` oder `ADMIN` aktiviert den
    Editor-Trigger.
- Nach erfolgreichem Anlegen:
  - Snackbar-Meldung, Draft-Form-Reset, Editor schliesst,
    Regel-Liste laedt neu (Status DRAFT).
- Fehler (400 Schema-Verletzung, 409 doppelter Key) landen in
  einem `ahs-banner` im Editor.
- Styles auf Tokens (`--space-*`, `--font-family-mono`,
  `--text-sm`); Grid-Row mit 3 Spalten auf Desktop.

### Nicht in dieser Iteration

- **Edit** einer bestehenden Regel (Backend hat nur create +
  activate; ein `PUT /rules/{id}` gibt es nicht). Fuer
  Aenderungen muss heute weiterhin eine neue Regel angelegt
  und die alte via RETIRED-Status geraeumt werden.
- **Retire** per UI (Backend-Endpunkt fehlt ebenfalls).
- **Schema-Validierung live im Editor** - hier uebernimmt der
  Backend-Call die Validierung (ConditionParser + Output-
  Validator).

## Build / Verifikation

- `npx ng lint cvm-frontend` -> All files pass linting.
- `npx ng build cvm-frontend` -> Application bundle generation
  complete.
- Backend unveraendert, Tests aus den Iterationen 05/17/23
  bleiben gruen.

## Folge-Iterationen

- `PUT /api/v1/rules/{id}` + UI-Edit (neue Iteration).
- `POST /api/v1/rules/{id}/retire` + Retire-Button.
- Schema-Autovervollstaendigung im Condition-Editor (Monaco
  mit JSON-Schema).
