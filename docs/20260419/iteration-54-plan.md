# Iteration 54 - Profil-YAML-Editor (Monaco)

## Ziel

Textarea in der Profiles-Seite durch den Monaco-Editor
(ngx-monaco-editor-v2) ersetzen. Syntax-Highlighting, Auto-Indent,
Minimap aus.

## Vorgehen

1. `angular.json`: Monaco-Assets aus
   `node_modules/monaco-editor/min` nach `/assets/monaco/` kopieren.
2. `shared/editor/monaco-providers.ts`: liefert einen Component-
   scoped `NGX_MONACO_EDITOR_CONFIG` mit `baseUrl=assets/monaco` und
   YAML-geeigneten Defaults.
3. `profiles.component.ts`: `MonacoEditorModule` importieren,
   `providers: [monacoRouteProviders()]` ergaenzen.
4. `profiles.component.html`: `<textarea>` &rarr;
   `<ngx-monaco-editor>` mit Language=yaml, theme=vs,
   automaticLayout, minimap=false.
5. `profiles.component.scss`: Fixed-Height-Container fuer den
   Editor.

## Scope

- **Nicht** in dieser Iteration: der Side-by-Side Diff-Editor
  (Monaco Diff). Der bestehende strukturierte Diff-Button (gegen
  die aktive Version) bleibt; Monaco-Diff kann in einer weiteren
  Iteration ergaenzt werden.

## Jira

`CVM-104` - Profil-YAML-Editor Monaco.
