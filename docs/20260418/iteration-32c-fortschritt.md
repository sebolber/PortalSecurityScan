# Iteration 32c – Fortschritt

**Thema**: UI-Review 2 nach Iteration 32 (6 Neu-Findings)
**Jira**: CVM-76
**Datum**: 2026-04-18

## Hintergrund

Nach Iteration 32 wurde `scripts/run-ui-exploration.sh` erneut
ausgefuehrt. Verdict 17 INHALT / 2 FEHLER (vorher 0 / 19). Die
Screenshots zeigten die behobenen Findings, brachten aber 6 neue
UX-Luecken zutage.

## Umgesetzt

### HIGH-neu-1 – Produkt-/Umgebung-Switcher-Label ragt aus dem Header

**Symptom**: `mat-form-field appearance="outline"` rendert einen
Floating-Label ueber der Toolbar-Oberkante. Auf jedem Screenshot
sichtbar als "Produkt / Umgebung waehlen"-Text, der aus der
Titelleiste herausragt.

**Fix**: Der Switcher ist jetzt ein
`mat-button`-Menue-Trigger (`cvm-product-switcher`) mit
`layers`-Icon, Label und `arrow_drop_down`-Chevron. Kein
Floating-Label mehr, keine Ueberlappungen. Die Auswahl laeuft
ueber `mat-menu` mit aktivem Zustand (Haken-Icon).

Dateien: `shell.component.{html,ts,scss}`.

### MEDIUM-neu-1 – Queue-Severity-Filter waren grau

**Symptom**: `/queue` hatte Severity-Filter-Chips im gleichen grauen
Stil, egal welche Stufe. Kein visueller Anker.

**Fix**: Chips bekommen `data-sev`-Attribut mit CSS-Regeln, die
Severity-Token-Farben als Top-Border zeigen. Aktive Chips fuellen
ihre gesamte Flaeche mit BG/FG-Farbe aus den Tokens
(`--color-severity-<level>-bg` + `-fg`).

Dateien: `queue-filter-sidebar.component.ts` (Inline-Styles).

### MEDIUM-neu-2 – Admin-Products-Titel war klein

**Symptom**: "Produkte & Versionen" renderte als Mini-Titel (kein
h1-Stil wie auf anderen Seiten).

**Fix**: `text-title-lg`-Klasse auf h1 ergaenzt.

### MEDIUM-neu-3 – `/reports` ohne Katalog zeigt UUID-Textfeld

**Symptom**: Wenn `environments.list()` eine leere Liste liefert
(noch keine Umgebungen angelegt), fiel der Fallback auf das
"Umgebung (UUID)"-Textfeld zurueck. Fuer Admins ohne vorhandene
Umgebungen ein unsinniger Workflow.

**Fix**: Das Textfeld kommt jetzt nur noch bei echtem
Katalog-Fehler (HTTP 403, Netz). Bei leerem Katalog
erscheint ein gerahmter Hinweis-Block mit CTA-Link nach
`/admin/environments` bzw. `/admin/products`.

Dateien: `reports.component.{html,ts,scss}`.

### MEDIUM-neu-4 – Admin-Theme-Eingabefelder zu breit

**Symptom**: Die `mat-form-field`-Felder im Branding-Formular
streckten sich auf die volle Card-Breite. Material 3 rendert bei
leerem/kurzem Input die Outline breit ueber den Wert hinaus, was
auf Screenshots wie zwei nebeneinanderliegende Felder wirkte.

**Fix**: `max-width: 420px` im Form-SCSS, damit das Label den
Wert eng umschliesst.

Dateien: `admin-theme.component.scss`.

### LOW-neu – Components-Card ohne Lead

**Symptom**: Card "Produkte" im Components-Screen hatte keine
Erklaerung, was sie tut.

**Fix**: Subtitle ergaenzt: "Produkt auswaehlen, um rechts die
Versionen mit Git-Commits und Release-Datum zu sehen."

## Verifikation

- `./mvnw -T 1C test` &rarr; **BUILD SUCCESS** (9 Module).
- `npx ng build --configuration=development` &rarr; erfolgreich.
- `npx ng test --watch=false` &rarr; **TOTAL: 68 SUCCESS**.

## Offene Punkte

Die zwei Routen, die im aktuellen Auto-Report als FEHLER markiert
sind (`/cves`, `/tenant-kpi`), liefern inhaltlich INHALT - die
`FEHLER`-Kategorisierung kommt von verbleibenden Backend-500ern
auf unrelevanten API-Calls. Naechster Schritt: Backend-Logs
pruefen und ggf. bearbeiten.
