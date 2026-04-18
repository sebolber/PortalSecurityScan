# Prompt-Baustein: UI-Exploration (CVM-60)

*Wiederverwendbarer Baustein, der in jeden Iterations-Prompt mit
Frontend-Relevanz eingebunden wird. Schritt 7 in
[CLAUDE.md Abschnitt 10](../../CLAUDE.md#10-arbeitsweise-in-jeder-claude-code-session).*

Grund dafuer, dass dieser Prompt existiert: mehrere Navigationspunkte
(CVEs, Komponenten, Profile, u. a.) fuehrten zeitweise auf leere
Seiten, obwohl das Backend Daten geliefert hat. Tests waren gruen,
das UI war stumm. Das darf nicht wieder passieren.

---

## Teil A - Pflicht-Ablauf

Bei jeder Frontend-relevanten Iteration gehst du durch diesen Ablauf.
Keine Abkuerzung.

### 1. App hochfahren

```bash
docker compose up -d
./mvnw spring-boot:run -pl cvm-app       # Terminal 1
cd cvm-frontend && npm start             # Terminal 2
```

Warte bis `GET http://localhost:8081/actuator/health` `UP` liefert
und das Frontend auf `http://localhost:4200` antwortet.

### 2. Exploration laufen lassen

```bash
cd scripts/explore-ui
npm ci
npx playwright install chromium           # nur beim ersten Mal
export CVM_TEST_ADMIN_PASS=admin
npm run explore -- --target=local
```

Das Skript:

- Loggt sich als `a.admin@ahs.test` in Keycloak ein.
- Iteriert die 19 Sidebar-Routen aus `scripts/explore-ui/routes.ts`.
- Iteriert die Settings-Rubriken innerhalb `/settings` per Click.
- Schreibt `docs/YYYYMMDD/ui-exploration-report.md` und
  `ui-exploration.json`, plus Screenshots unter
  `docs/YYYYMMDD/ui-exploration/screenshots/`.

Exit-Code 0: keine `FEHLER`/`NICHT_ERREICHBAR`. Exit-Code 1: gibt es.

### 3. Screenshots selbst ansehen

**Das ist der entscheidende Punkt.** Oeffne **mindestens fuenf**
der erzeugten Screenshots mit dem `view`-Tool. Nicht nur die, die
das Skript rot gemeldet hat - auch die, die es als `INHALT`
eingestuft hat. Die Heuristik ist blind fuer:

- leere, aber strukturell-vorhandene Tabellen (`<table>` ohne Zeilen)
- schwebende Validierungsmeldungen ohne Feldbezug
- Layout-Brueche, inkonsistente Hierarchien
- schlechte Leerzustaende ("Keine Daten" ohne Hinweis, was zu tun ist)
- 401/403-Weiterleitungen, die der User als "leere Seite" wahrnimmt

### 4. Findings-Liste schreiben

Deine Findings-Liste hat **vier Pflicht-Kategorien**, jede mit
mindestens einer ehrlichen Zeile. Auch wenn nichts auffaellt:
schreib das hin. Das macht sichtbar, dass du hingesehen hast.

- **Tote Routen ohne API-Calls**: Routen, die in der Liste der
  Skript-Ergebnisse keinen einzigen `/api/`-Request zeigen und
  auch keinen `<cvm-page-placeholder>` enthalten.
- **Routen mit 4xx/5xx-Fehlern**: aus `apiCalls` in der JSON-Datei.
- **UI-Auffaelligkeiten beim Durchsehen der Screenshots**: mit
  eigenen Worten, nicht Copy-Paste des Skripts.
- **Was ich nicht geprueft habe, aber vermute**: ehrlich auflisten.

Insgesamt mindestens **fuenf Eintraege**, auch wenn sich manche auf
"unauffaellig" reduzieren.

---

## Teil B - Abschlussreport

Lege den Report unter `docs/YYYYMMDD/iteration-NN-ui-exploration.md`
ab (NN = Iterations-Nummer).

Struktur:

1. **Zusammenfassung** (2-4 Saetze): was lief, was nicht, Anzahl
   Routen je Verdict.
2. **Auffaelligkeiten pro Route**: je Route mit Verdict != `INHALT`
   ein Abschnitt mit Screenshot-Verweis, API-Call-Liste, deiner
   Einschaetzung.
3. **Positiv-Beobachtungen**: zwei bis drei Dinge, die gut
   funktionieren (gibt ein realistisches Bild, nicht nur Negatives).
4. **Offene Punkte / Folge-Iteration**: was in der aktuellen
   Iteration nicht behoben wird.

**Keine Copy-Paste des Skript-Outputs**. Der Skript-Output liegt
in `ui-exploration-report.md` daneben. Der Abschlussreport ist
**deine** Bewertung.

---

## Teil C - Infrastruktur-Probleme

Wenn das Skript nicht laeuft:

- Postgres/Keycloak nicht oben: `docker compose ps` checken, ggf.
  `docker compose logs keycloak` lesen. Realm-Import kommt ueber
  `docker-compose.yml:23` (`command: ["start-dev", "--import-realm"]`);
  wenn die Realm-Datei fehlt, stoppen und fragen.
- Backend-Start scheitert: `logs/YYYYMMDD-*/backend.out` lesen.
  Typische Ursachen: Flyway-Migrations-Konflikt, fehlende
  Testcontainers-DB.
- Frontend startet nicht: `npm install` im `cvm-frontend/`-Ordner,
  `npm start`, Port 4200 frei?
- Chromium fehlt: `npx playwright install chromium` im
  `scripts/explore-ui/`-Verzeichnis.

**Stopp-Verhalten**: Wenn nach 10 Minuten Troubleshooting kein
gruener Lauf moeglich ist, meldest du den konkreten Fehler und
fragst Sebastian. Du **umgehst das Skript nicht** und simulierst
nicht.

---

## Teil D - Wann die Exploration skippen darf

Reine Backend-Iterationen duerfen das Skript skippen. "Reine
Backend-Iteration" bedeutet:

- Kein Angular-Code geaendert (kein Commit unter `cvm-frontend/`).
- Kein neuer `@RestController` oder geaenderter Response-DTO, der
  das UI beeinflussen koennte.
- Keine Routing-Aenderung, keine neue Rolle, keine neue
  Keycloak-Konfig.

Wer skippt, schreibt im Fortschrittsreport eine Zeile:

> "UI-Exploration uebersprungen, weil reine
> Backend-/Infrastruktur-Iteration (keine Aenderungen unter
> `cvm-frontend/` oder an Controller-Payloads)."

Alles andere: Exploration laufen lassen.

---

## Anhang - wie die Verdicts entstehen

Das Skript ist diagnostisch, nicht behauptend. Die Heuristik:

| Bedingung | Verdict |
|---|---|
| Konsolen-Error oder 5xx-API-Response | `FEHLER` |
| 4xx-API-Response ohne 5xx | `FEHLER` |
| `<cvm-page-placeholder>` im DOM | `PLATZHALTER` |
| `<table>` / `<form>` / `<canvas>` vorhanden | `INHALT` |
| `<ahs-empty-state>` + mindestens 1 API-Call | `INHALT` |
| Kein API-Call und fast leerer Main-Content | `LEER` |
| Navigation-Exception (Timeout, 500) | `NICHT_ERREICHBAR` |

Wichtig: die Heuristik irrt sich. Sie markiert zum Beispiel eine
Seite, die eine Tabelle **zeigt, aber die Tabelle ist leer und der
Grund dafuer ist ein 401 auf die API**, als `INHALT`. Genau deshalb
ist das Screenshot-Ansehen in Teil A Schritt 3 **Pflicht**.
