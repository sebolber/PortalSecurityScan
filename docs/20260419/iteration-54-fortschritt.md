# Iteration 54 - Fortschritt

**Thema**: Profil-YAML-Editor mit Monaco (CVM-104).

## Was gebaut wurde

- `angular.json`: Monaco-Assets aus
  `node_modules/monaco-editor/min` werden nach `/assets/monaco/`
  ausgerollt.
- `shared/editor/monaco-providers.ts`: Component-scoped
  `NGX_MONACO_EDITOR_CONFIG` mit `baseUrl='assets/monaco'`,
  `minimap=false`, `automaticLayout=true`, `tabSize=2`.
- `profiles.component.ts`: `MonacoEditorModule` importiert und
  `providers: [monacoRouteProviders()]` eingetragen. Dadurch wird
  Monaco nur beim Aktivieren der Profiles-Route geladen; der
  Initial-Bundle bleibt unveraendert.
- `profiles.component.html`: `<textarea>` &rarr;
  `<ngx-monaco-editor>` mit `language: 'yaml'` und den ueblichen
  Defaults.
- `profiles.component.scss`: Fixed-Height-Container (420px) fuer
  den Editor.

## Build

- `npx ng build` &rarr; ok, Initial-Bundle weiter unter Budget.
- `npx ng lint` &rarr; All files pass linting.

## Scope-Hinweis

Der Side-by-Side Monaco-Diff-Editor ist bewusst nicht Teil dieser
Iteration. Der strukturierte Feld-Diff (API +
`ProfileDiffEntry`-Liste) bleibt unveraendert und reicht fuer die
Approve-Review. Monaco-Diff kann in einer Folge-Iteration ergaenzt
werden, falls gewuenscht.

## Vier Leitfragen (Oberflaeche)

1. *Weiss ein Admin, was zu tun ist?* Ja - Editor mit YAML-
   Highlight und Hilfetext zu den Schema-Pfaden.
2. *Ist erkennbar, ob eine Aktion erfolgreich war?* Ja - "Draft
   speichern" meldet die neue Version via SnackBar.
3. *Sind Daten sichtbar, die im Backend existieren?* Ja - der
   Editor laedt die aktive YAML-Quelle vor.
4. *Gibt es einen Weg zurueck/weiter?* "Auf aktive Version
   zuruecksetzen" stellt den Original-Inhalt wieder her.
