# Iteration 28 – UI-Harmonisierung: Reines Tailwind-Redesign nach PortalCore-Vorbild

**Jira**: CVM-62
**Abhängigkeit**: Iteration 27 wird durch diese Iteration **überschrieben** in Bezug auf die Technologiebasis. Fachliche Anforderungen aus 27 (Token-Layer, Komponenten-Kit, Theming-API, Coverage-Audit) bleiben gültig und werden hier fortgeführt.
**Status**: OFFEN – mehrstufige Umsetzung über die Aufgabenliste in Abschnitt 6.
**Ziel**: Komplettumbau der Angular-Oberfläche auf ein **reines Tailwind-Design-System ohne Angular Material**, angelehnt an die harmonische, moderne Anmutung des PortalCore-Frontends. Beseitigung der aktuell sichtbaren Probleme (überlappende Eingabefelder, unruhige Filter-Zeilen, inkonsistente Abstände, schmale Detailbereiche mit ungenutztem Whitespace).

---

## 0 Ausgangslage

Der aktuelle Zustand von `cvm-frontend/` kombiniert **Angular Material 18** (Material-Theme über `mat.m2-define-light-theme`) mit **Tailwind 3.4**. Diese Mischung ist die Hauptursache für die vom Fachbereich beanstandeten UI-Probleme:

- Material-Formfields bringen eigene Paddings, Höhen und Wrapper-DOMs mit. Tailwind-Utilities (`px-4 py-2`, `gap-*`) auf einem `<mat-form-field>` brechen dessen internes Layout – **Eingabefelder überlappen**.
- Zwei Dark-Mode-Selektoren (`data-theme='dark'` für Material, `class='dark'` für Tailwind-Overrides) – einige Bereiche schalten nicht korrekt um.
- Token-Kollisionen: `--cvm-*` Legacy-Aliase, `--color-*` neue semantische Tokens und Material-eigene Theme-Variablen überschreiben sich gegenseitig.
- Keine einheitlichen Komponenten-Klassen für Inputs, Cards, Buttons – jedes Feature stylt selbst.
- Detailbereiche (CVE-Detail, Profil-Editor, KI-Audit-Drawer) nutzen feste Breiten statt der vollen Content-Breite.
- Filter-Zeilen fluchten nicht: Material-Select, native `<input>` und `<button>` haben unterschiedliche Höhen, Label-Positionen und Radii.

Das PortalCore-Frontend zeigt, wie es sauber geht: **keine Material-Komponenten**, reines Tailwind mit `@layer components` (`.btn-primary`, `.card`, `.input-field`, `.badge`), Lucide-Icons, Fira Sans + Fira Sans Condensed, klare `--portal-*`-Tokens. Das ist die Zielarchitektur für diese Iteration.
