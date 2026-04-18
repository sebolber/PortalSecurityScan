# UI-Exploration-Report (Baseline)

**Ticket**: CVM-60
**Stand**: 2026-04-18
**Target**: n/a (Sandbox-Baseline, kein Playwright-Lauf)
**User**: n/a

## Status dieser Baseline

Die vollstaendige Playwright-basierte Exploration (Login + 19 Routen
+ Settings-Rubriken + Screenshots) konnte im Setup-Sandbox **nicht
ausgefuehrt werden**: kein Docker-Daemon, kein Chromium vorhanden.
Siehe dazu die Infrastruktur-Analyse in
[`ui-exploration-setup-analyse.md`](./ui-exploration-setup-analyse.md)
Abschnitt 8.

**Damit keine Spezifikation laeuft, die keine Baseline gegen sich
hat**, enthaelt dieser Report statt der Screenshots die **statisch
erzeugten Audit-Outputs** (grep-basiert, laufen ohne Full-Stack).
Diese decken die Backend-/Frontend-Coverage-Dimension ab; die
**visuelle** Dimension (Screenshots, Verdict-Matrix) wird beim
**ersten gruenen CI-Lauf** automatisch erzeugt und haengt als
Artefakt `ui-exploration-<run>` am PR.

## Verdicts (erwartet beim ersten Lauf)

Sobald `scripts/explore-ui/` in einer Umgebung mit Docker + Chromium
laeuft, erscheinen hier die echten Zahlen. Erwarteter Ausgangszustand
(extrapoliert aus der Coverage-Matrix
`docs/20260418/frontend-backend-coverage.md`):

- 20 Routen total, davon 19 Feature-Routen + Dashboard-Default
- Erwartete Verteilung: ~16 `INHALT`, 2-4 `INHALT mit leerem
  Datastand` (Admin-Seiten ohne Testdaten), 0 `PLATZHALTER`,
  0 `FEHLER` bei sauberer Keycloak-/Backend-Infrastruktur

## Audit-Helfer-Outputs (statisch, echter Lauf in diesem Setup)

### Backend-Endpunkte

`scripts/audit/list-endpoints.sh` wurde ausgefuehrt. Ergebnis:
**68 REST-Endpunkte** in 29 Controllern.

Output liegt unter
[`ui-exploration/backend-endpoints.txt`](./ui-exploration/backend-endpoints.txt)
und enthaelt pro Endpunkt: HTTP-Methode, Pfad, PreAuthorize-Rolle
(sofern annotiert), Quelldatei + Zeile.

### Frontend-API-Aufrufe + Routen

`scripts/audit/list-frontend-calls.sh` wurde ausgefuehrt. Ergebnis:
**16 unique API-Aufrufe** in den Frontend-Services plus **20
Angular-Routen** aus `app.routes.ts`.

Output liegt unter
[`ui-exploration/frontend-calls.txt`](./ui-exploration/frontend-calls.txt)
und enthaelt zwei Abschnitte: (1) `this.api.{get,post,put,delete,
patch}`-Aufrufe mit URL + Quelle, (2) Routen mit Komponenten-Namen.

### Auffaelligkeiten im statischen Abgleich

Aus dem ersten echten Lauf sichtbar (vor Playwright):

- `GET /api/v1/ai-audit` (Controller) vs. `/api/v1/ai/audits`
  (manuelle Zeile im Endpunkt-Output) - unterschiedliche Pfade,
  tatsaechlich **gleiche** Ressource mit historischem Umweg, gehoert
  in `frontend-backend-coverage.md` aufgeraeumt.
- `POST /api/v1/scans` (scan-upload in Iteration 28) erscheint nur in
  der CI-Gate-Dokumentation, nicht als direkter Frontend-Call unter
  `this.api.post(...)` - der Upload-Pfad nutzt den `HttpClient`
  direkt wegen Multipart. **Kein Befund, nur Hinweis** an die naechste
  Coverage-Pflege, dass der Regex im Audit-Helfer das nicht faengt.
- Mehrere Aufrufe mit `url="?"` im Audit-Output: das sind Calls, bei
  denen der Regex die URL nicht extrahieren konnte (mehrzeilige
  Template-Strings, dynamische Pfade). Diese sind im Code-Kontext
  wohl-definiert; der Audit-Helfer ist hier bewusst hemdsaermelig
  (siehe README).

## Was der erste CI-Lauf liefern wird

Der GitHub-Actions-Workflow `.github/workflows/ui-exploration.yml`
loest bei jedem PR mit Frontend- oder Controller-Aenderung aus und
liefert:

- `docs/YYYYMMDD/ui-exploration-report.md` mit echter Verdict-Tabelle
- `docs/YYYYMMDD/ui-exploration.json` (maschinenlesbar)
- `docs/YYYYMMDD/ui-exploration/screenshots/*.png` (19 Routen plus
  Settings-Rubriken)

30 Tage Aufbewahrung, per `github.run_number` eindeutig benannt.

## Empfehlung fuer Iteration 23C

Diese Baseline ist **nicht** die gleiche wie "Skript-Lauf mit
Screenshots". Das ist klar benannt. Die naechste sinnvolle
Aktion ist einer dieser zwei Wege:

1. **Lokaler Lauf durch Sebastian** auf seinem Laptop mit Docker
   + Chromium. Schritte stehen in
   [`../prompts/ui-exploration.md`](../prompts/ui-exploration.md)
   Teil A. Ergebnis ersetzt diesen Baseline-Report.
2. **PR gegen `main` oeffnen**, der auch nur eine triviale Frontend-
   Aenderung hat (z.B. Doku-Korrektur in `cvm-frontend/README.md`).
   Der CI-Workflow startet und erzeugt die echten Screenshots als
   Artefakt.

Bis dahin gilt: Coverage-Matrix
(`frontend-backend-coverage.md`) + Audit-Helfer-Outputs als
belastbare Baseline.
