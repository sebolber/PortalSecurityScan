# Iteration 53 - Rules-Editor (Update-Form)

## Ziel

Die Rules-Seite hat bereits eine Draft-Create-Form. Iteration 53
ergaenzt den Editier-Pfad fuer DRAFT-Regeln: klick auf
"Bearbeiten" bei einer DRAFT-Regel laedt deren Werte in das
bestehende Formular und speichert per PUT.

## Vorgehen

1. **Backend**: neuer `RuleService.updateDraft(id, input, editor)`
   mit Parser-Validierung, Ablehnung fuer Nicht-DRAFT oder
   soft-geloeschte Regeln. Controller: `PUT /api/v1/rules/{ruleId}`
   (`CVM_RULE_AUTHOR|CVM_ADMIN`).
2. **Frontend**:
   - `rulesService.update(ruleId, req)`.
   - `rules.component`: neuer `editingRuleId` Signal, Methode
     `bearbeite(rule)` pre-fuellt das Formular, `draftAnlegen()`
     wechselt zwischen create und update.
   - HTML: "Bearbeiten"-Button fuer DRAFT-Regeln, Editor-Titel und
     Submit-Label dynamisch.
3. Tests: drei neue Service-Tests (Happy, ACTIVE-lehnt-ab,
   unbekannt).

## Jira

`CVM-103` - Rules-Editor Update-Form.
