# Iteration 90 - Plan: CvmConfirmService ersetzt window.confirm (U-06b)

**Jira**: CVM-330

## Ziel

Einheitlicher Bestaetigungs-Dialog fuer die Admin-Flows
(Produkte, Versionen, Regeln, Umgebungen). `window.confirm` wird
durch `CvmConfirmService.confirm(...)` ersetzt, das einen
`<cvm-dialog>` programmatisch oeffnet und ein Promise<boolean>
zurueckgibt. Danger-Aktionen (Soft-Delete) rendern den
Bestaetigungs-Button als `btn-danger`.

## Umfang

### Neu
- `cvm-frontend/src/app/shared/components/cvm-confirm.service.ts`
  (`CvmConfirmHostComponent` + `@Injectable CvmConfirmService`).
  Host wird beim ersten Aufruf per `createComponent` gebaut und an
  `document.body` gehangen. API:
  `confirm({ title, message, confirmLabel?, cancelLabel?, variant? })`.
- `cvm-confirm.service.spec.ts`: 5 Specs (Resolve/Reject,
  Host-Wiederverwendung, Reset von `current`).

### Refactor
- `admin-products.component.ts`: `loescheProdukt` und
  `loescheVersion` rufen `CvmConfirmService.confirm(...)` statt
  `window.confirm(...)`. Vier neue Specs im vorhandenen Spec-File.
- `admin-environments.component.ts`: `loesche` nutzt den neuen
  Service. Neuer Spec-File mit 2 Cases (ok/cancel).
- `rules.component.ts`: `loesche` nutzt den neuen Service. Neuer
  Spec-File mit 2 Cases (ok/cancel).

## Nicht-Umfang

- Profile/llm-configurations/parameters/theme nutzen weiter
  `window.confirm` - wandern in einer Folge-Iteration, damit der
  Scope dieser Iteration uebersichtlich bleibt.

## Abnahme

- `ng lint` / `ng build` / Karma gruen.
- Kein neuer Backend-Test noetig (reines Frontend).
