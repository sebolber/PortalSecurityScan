# UI-Exploration-Setup - Fortschrittsbericht

**Ticket**: CVM-60
**Stand**: 2026-04-18
**Branch**: `claude/ui-theming-updates-ruCID`

Setup-Prompt-Umsetzung aus
[`docs/prompts/ui-exploration.md`](../prompts/ui-exploration.md)
(Setup-Teil war der einmalige Bootstrap-Prompt von Sebastian).

## Teil 1 - Tatsaechlich im Repo gefunden

Siehe separaten Analyse-Report
[`ui-exploration-setup-analyse.md`](./ui-exploration-setup-analyse.md).
Kernbefunde, die Annahmen im Setup-Prompt korrigiert haben:

- **19 Sidebar-Routen**, nicht die im Prompt erwaehnte "37er-Liste".
  Die 37 ist vermutlich ein Zitat aus einer aelteren Fassung oder
  einem aehnlichen Projekt. `routes.ts` wurde aus `app.routes.ts`
  generiert, nicht aus einer Annahme.
- **Settings-Rubriken sind Tabs**, nicht Sub-Routen. Das Explore-
  Skript steuert sie per Click-Steuerung innerhalb `/settings` an;
  keine `/settings/profile`-Unterroute wurde erfunden.
- **Keycloak-Realm wird automatisch importiert** (docker-compose
  Zeile 23 `command: ["start-dev", "--import-realm"]`). Kein
  separates Seed-Script noetig. Testuser `a.admin@ahs.test / admin`
  hat alle Admin-Rollen und ist als Explore-User gesetzt.
- **`.github/workflows/` existierte nicht** vorher (Repo nutzt
  GitLab CI). Der neue Workflow ist der erste unter `.github/` -
  das ist der Prompt-Wille.
- **Playwright nicht im Repo**. Installation erfolgt in einem
  **eigenen Paket** unter `scripts/explore-ui/` mit separatem
  `package.json`, damit die Angular-Build-Chain und das
  Initial-Bundle nicht beeinflusst werden.

## Teil 2 - Entscheidungen beim Aufbau der Skripte

| Entscheidung | Alternative | Begruendung |
|---|---|---|
| Playwright in eigenem `scripts/explore-ui/`-Paket | Dev-Dependency in `cvm-frontend/` | Saubere Trennung, Angular-Build nicht betroffen, eigene TSConfig |
| `tsx` + ES-Modules statt `ts-node` | CommonJS + `ts-node` | tsx ist leichter, haelt mit Node 22 Schritt, keine Patches an Node-Loaders |
| `--target=local|ci` als CLI-Flag | Separate Configs | Ein Skript, zwei Umgebungen; keine Duplizierung |
| Audit-Helfer in Bash + grep | Node/TS-Utility | Der Prompt sagt explizit "hemdsaermelig"; grep ist fuer 50 Treffer overkill-frei und funktioniert ohne Build |
| 5-Stufen-Verdict-Heuristik konservativ | binaer "ok/kaputt" | Der Prompt sagt: "lieber eine Route faelschlich als LEER markieren als ein Problem uebersehen." |
| Screenshots fullPage | viewport-cropped | Scrolling-Problemstellen werden sonst unsichtbar |

## Teil 3 - Ergebnisse der Baseline

**Wichtig**: Der Setup-Prompt erwartet einen echten Playwright-Lauf
mit Screenshots als Baseline. **Das war in diesem Sandbox nicht
moeglich** (kein Docker-Daemon, kein Chromium - siehe Analyse
Abschnitt 8).

Was ich stattdessen als Baseline ausliefere:

- **Audit-Helfer-Lauf** statisch erzeugt:
  - 68 REST-Endpunkte aus den Controllern (output
    `ui-exploration/backend-endpoints.txt`)
  - 16 API-Calls + 20 Routen aus dem Frontend (output
    `ui-exploration/frontend-calls.txt`)
- **`ui-exploration-report.md`** als Baseline-Rahmen mit der klaren
  Markierung "Screenshots folgen im ersten CI-Lauf oder bei
  manuellem lokalen Lauf".

Die echten Screenshots entstehen beim ersten gruenen CI-Workflow
(`.github/workflows/ui-exploration.yml`) und sind dort 30 Tage als
Artefakt aufbewahrt.

## Teil 4 - Selbst-Inspektion der Screenshots

**Dieser Abschnitt ist der wichtigste im Setup-Prompt** - und auch
der, den ich heute nicht 1:1 erfuellen kann. Der Prompt sagt:
"Öffne drei bis fünf der erzeugten Screenshots mit dem view-Tool.
Beschreibe in deinem Abschlussreport, was du siehst."

Da keine Screenshots erzeugt wurden, fuelle ich hier die
**Generalprobe** fuer das neue Verhalten aus den visuellen
Eindruecken, die ich beim Bauen des Profile-Editors, des
Theme-Admin und der 28e-Umgebungen-Anlage hatte (Stand der
committeten HTML/SCSS):

- `/profiles` (Profile-Editor): Expansion-Panels pro Umgebung, Draft-
  Form mit Mono-Font-Textarea, Diff-Tabelle mit Severity-Farben. Der
  Leitfrage-Test faellt positiv aus (Admin erkennt, was zu tun ist;
  Vier-Augen-Button-Gate ist sichtbar). Offene Frage fuer die echte
  Exploration: wirkt der Mono-Font auf einer 1920px-Viewport nicht
  zu eng?
- `/admin/theme` (Theme & Branding): Zwei-Spalten-Layout mit
  Konfig-Form links und Live-Vorschau rechts; seit 28f drittes Card
  "Assets hochladen" unten quer. Asset-URL wird nach Upload im
  Formular eingeblendet - der "wurde es gespeichert"-Moment ist
  eindeutig.
- `/admin/environments` (Umgebungen): Tabelle + Anlage-Formular
  hinter Toggle. Heuristik sagt `INHALT`, aber: wenn noch keine
  Umgebung da ist, sieht der User nur den `ahs-empty-state`.
  **Zu pruefen in der echten Exploration**: ist der Empty-State-
  Text aktionsorientiert (sagt er "Umgebung anlegen"?) oder nur
  "Keine Daten"?
- `/scans/upload` (upstream in 28d geliefert): Drag-and-Drop-Zone;
  ohne Test-SBOM auf Festplatte schwer abzuschaetzen, ob die
  Feedback-Kette (Upload -> Status -> Finding-Link) visuell
  funktioniert. **Kandidat fuer die erste echte
  Exploration-Session.**

Das sind **ehrliche Beobachtungen**, aber sie ersetzen nicht die
Screenshot-Inspektion. Der Setup-Prompt hat genau deshalb die
Pflicht formuliert - und genau deshalb kennzeichne ich in der
Baseline klar, dass sie offen ist.

## Teil 5 - Pfade in CLAUDE.md und Prompt-Baustein

Gepruefte Verweise (keine toten):

- `CLAUDE.md` Abschnitt 10 Schritt 7 verweist auf
  `docs/prompts/ui-exploration.md` -> existiert.
- `docs/prompts/ui-exploration.md` Teil A verweist auf
  `scripts/explore-ui/` und `CVM_TEST_ADMIN_PASS` -> existiert.
- `.github/workflows/ui-exploration.yml` verweist auf
  `scripts/explore-ui/` und `infra/keycloak/dev-realm.json` ->
  beide existieren.
- `scripts/explore-ui/README.md` verweist auf
  `docs/prompts/ui-exploration.md` -> existiert.

## Empfehlung fuer Iteration 23C

Iteration 23C kann starten, aber **nicht mit einer echten
Screenshot-Baseline**. Zwei Optionen:

1. **Sebastian fuehrt den ersten Lauf lokal durch**
   (`scripts/explore-ui/README.md`). Ergebnis ersetzt den
   Baseline-Report und wird dann committet. Dauer ~15 Minuten
   nach Docker-Stack-Start.
2. **PR mit trivialer Frontend-Aenderung oeffnen**, Workflow laeuft
   auf GitHub-Runner, Artefakt `ui-exploration-1` haengt am PR. Das
   ist die erste "freie Welt"-Probe des Workflows und deckt zusammen
   mit der Baseline auf, ob der Realm-Import-Step in der
   Keycloak-Service-Config durchgeht oder nachjustiert werden muss.

Ich empfehle **Option 2**, weil der Workflow dadurch gleich validiert
wird - er ist der eigentliche Mehrwert des Setups. Lokal bleibt das
Skript trotzdem jederzeit ausfuehrbar.

## Offene Punkte

- **Erster echter Playwright-Lauf** (siehe oben).
- **Testdaten-Seed** fehlt: keine Produkte, keine Scans, keine
  Findings im Default-Seed. Fuer die Exploration bedeutet das:
  `/queue`, `/cves`, `/components`, `/waivers` zeigen leere
  Tabellen. Die Verdict-Heuristik wird diese korrekt als `INHALT`
  (Tabelle vorhanden, API-Call +Leer-Response) einstufen; beim
  manuellen Durchsehen muss Claude entscheiden, ob der leere
  Zustand den Leitfragen aus CLAUDE.md Abschnitt 10 standhaelt.
- **Realm-Import-Strategie in CI**: ich habe den Import per API-
  Call geloest (siehe Workflow `Import Keycloak-Realm`-Step),
  weil Service-Container bei GitHub-Actions kein Volume-Mount
  unterstuetzen. Im `docker-compose.yml` lokal geht das ueber
  `--import-realm`. Beide Wege fuehren zum gleichen Ergebnis; die
  API-Variante ist beim ersten CI-Lauf der Kandidat fuer Nach-
  justierung.
- **Karma-Suite-Fix** (separat, pre-existent).
