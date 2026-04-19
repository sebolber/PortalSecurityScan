# Iteration 90 - Fortschritt

**Jira**: CVM-330 (U-06b CvmConfirmService)

## Umgesetzt

- Neuer Baustein `CvmConfirmService` inklusive
  `CvmConfirmHostComponent`. Promise-basiertes API
  (`confirm({title, message, confirmLabel, variant})`), Rueckgabe
  `false` bei Abbruch/ESC/Overlay-Klick.
- `admin-products.component.ts`: `loescheProdukt` und
  `loescheVersion` nutzen den neuen Service (danger-Variante,
  Labels "Entfernen").
- `admin-environments.component.ts`: `loesche` nutzt den neuen
  Service.
- `rules.component.ts`: `loesche` (Soft-Delete) nutzt den neuen
  Service.
- Specs:
  - `cvm-confirm.service.spec.ts`: 5 Specs (open/bestaetigen/
    abbrechen, Promise-Resolution, Host-Wiederverwendung).
  - `admin-products.component.spec.ts`: +3 Specs (Produkt loeschen
    ok + Abbruch, Version loeschen).
  - `admin-environments.component.spec.ts` (neu): 2 Specs.
  - `rules.component.spec.ts` (neu): 2 Specs.

## Nicht umgesetzt

- `profiles.draftLoeschen`, `admin-llm-configurations`,
  `admin-parameters`, `admin-theme` nutzen weiter
  `window.confirm`. Das wird in einer Folge-Iteration konsolidiert,
  damit der Scope hier auf die drei im UX-Plan genannten Admin-
  Flows beschraenkt bleibt.

## Technische Hinweise

- Der Host wird beim ersten `confirm`-Aufruf lazy erzeugt und an
  `document.body` gehangen - gleiche Strategie wie
  `CvmToastService`.
- Tests greifen auf die interne `host`-Referenz zu
  (`(svc as unknown as { host }).host`), um den Promise-Flow ohne
  DOM-Flush testen zu koennen. Reale Nutzer-Tests (Klick auf
  Button im DOM) wuerden `fixture.detectChanges()` plus
  `queueMicrotask` brauchen und wurden bewusst vermieden.
