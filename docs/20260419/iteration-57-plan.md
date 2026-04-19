# Iteration 57 - Monaco Side-by-Side Diff-Editor

## Ziel

Die Profiles-Seite zeigt bisher nur den strukturierten Feld-Diff
(`ProfileDiffEntry`-Liste) nach dem Draft-Save. Iteration 57
ergaenzt einen Monaco Side-by-Side Diff-Editor: aktiv-YAML links,
Draft-/Editor-YAML rechts, direkt vergleichbar.

## Vorgehen

1. `profiles.component.ts`:
   - `ProfileRow.diffOffen` als UI-State.
   - `diffUmschalten(row)`, `diffOriginal(row)`, `diffModified(row)`.
2. `profiles.component.html`:
   - Neuer Button "Diff aktiv vs. Draft" pro Zeile mit aktivem
     Profil.
   - `<ngx-monaco-diff-editor>` im Full-Width-Container mit
     `renderSideBySide=true` und `readOnly=true`.
3. CSS: Diff-Editor-Styles fuer den bestehenden Monaco-Container.

## Jira

`CVM-107` - Monaco Side-by-Side Diff-Editor fuer Profile.
