# UI-Exploration-Setup - Teil 1: Repo-Analyse

**Ticket**: CVM-60
**Stand**: 2026-04-18
**Analyst**: Claude Code (Sandbox-Lauf)

Analyse der Voraussetzungen, bevor Skripte oder CI-Workflow
geschrieben werden. **Keine Annahme, die nicht aus dem Repo
verifiziert ist.**

---

## 1 Frontend-Start lokal

- **Paketdefinition**: `cvm-frontend/package.json:5-13`.
- **Start-Kommando**: `npm start` → `ng serve --host 0.0.0.0`.
- **Port**: 4200 (bestaetigt ueber `scripts/start.sh:41`
  `FRONTEND_PORT="${CVM_FRONTEND_PORT:-4200}"` und die
  Keycloak-`redirectUris` in `infra/keycloak/dev-realm.json`).
- **Konfiguration**: `cvm-frontend/src/assets/config.json` haelt
  `apiBaseUrl` und Keycloak-Werte:
  ```json
  {
    "apiBaseUrl": "http://localhost:8081",
    "keycloak": {
      "url": "http://localhost:8080",
      "realm": "cvm-local",
      "clientId": "cvm-local"
    }
  }
  ```
- **Ohne Backend + Keycloak**: die Shell laedt durch (Dashboard
  ist `public`, siehe `app.routes.ts:17-21`), alle anderen
  Routen feuern 401 in den API-Interceptor.

## 2 Backend-Start lokal

- **Spring-Boot-Modul**: `cvm-app` (`./mvnw spring-boot:run -pl cvm-app`).
- **Port**: 8081 (`cvm-app/src/main/resources/application.yaml:44`
  `server.port: ${CVM_SERVER_PORT:8081}`).
- **Abhaengigkeiten**:
  - PostgreSQL 16 + pgvector auf 5432 (`docker-compose.yml:2-17`)
  - Keycloak 24.0 auf 8080 (`docker-compose.yml:19-29`)
  - MailHog auf 1025/8025 (`docker-compose.yml:31-38`)
- **Profile**: Default-Profil hat Flyway-Migration V0000-V0026
  plus Seed V0023 (Default-Mandant + LLM-Profile).

## 3 Keycloak + Testuser

- **Realm-Datei**: `infra/keycloak/dev-realm.json` (einzige
  Datei unter `infra/keycloak/`).
- **Realm**: `cvm-local`, Client `cvm-local` public, PKCE-Flow.
- **Rollen** (Zeilen 11-24 der Realm-Datei): CVM_VIEWER,
  CVM_ASSESSOR, CVM_REVIEWER, CVM_APPROVER, CVM_PROFILE_AUTHOR,
  CVM_PROFILE_APPROVER, CVM_RULE_AUTHOR, CVM_RULE_APPROVER,
  CVM_REPORTER, AI_AUDITOR, CVM_ADMIN.
- **Test-User** (Zeilen 40-85):
  - `t.tester@ahs.test` / `test` → VIEWER/ASSESSOR/REVIEWER
  - `a.admin@ahs.test` / `admin` → alle ADMIN-nahen Rollen
  - `j.meyer@ahs.test` / `meyer` → PROFILE_AUTHOR /
    RULE_AUTHOR / REPORTER / REVIEWER
- **Fuer Exploration geeigneter User**: `a.admin@ahs.test` (hat
  Zugriff auf alle Admin-Routen `/admin/theme`,
  `/admin/products`, `/admin/environments`, `/tenant-kpi`).
- **Automation**: Das Realm wird via `--import-realm` beim
  Keycloak-Start automatisch geladen
  (`docker-compose.yml:23` command). Kein separates Seed-Skript
  noetig.

## 4 Aktuelle Routen in der Sidebar

Quelle: `cvm-frontend/src/app/app.routes.ts` und
`cvm-frontend/src/app/core/auth/role-menu.service.ts`.

| Pfad | Komponente | Mindest-Rolle(n) |
|---|---|---|
| `/dashboard` | `DashboardComponent` | public |
| `/queue` | `QueueComponent` | ASSESSOR/REVIEWER/APPROVER/ADMIN |
| `/cves` | `CvesComponent` | authGuard (jeder eingeloggte User) |
| `/components` | `ComponentsComponent` | authGuard |
| `/profiles` | `ProfilesComponent` | PROFILE_AUTHOR/PROFILE_APPROVER/ADMIN |
| `/rules` | `RulesComponent` | RULE_AUTHOR/RULE_APPROVER/ADMIN |
| `/reports` | `ReportsComponent` | VIEWER/REPORTER/ADMIN |
| `/ai-audit` | `AiAuditComponent` | AI_AUDITOR/ADMIN |
| `/settings` | `SettingsComponent` | authGuard |
| `/admin/theme` | `AdminThemeComponent` | ADMIN |
| `/admin/products` | `AdminProductsComponent` | ADMIN |
| `/admin/environments` | `AdminEnvironmentsComponent` | ADMIN |
| `/scans/upload` | `ScanUploadComponent` | ADMIN/ASSESSOR |
| `/waivers` | `WaiversComponent` | authGuard |
| `/alerts/history` | `AlertsHistoryComponent` | authGuard |
| `/reachability` | `ReachabilityComponent` | authGuard |
| `/fix-verification` | `FixVerificationComponent` | authGuard |
| `/anomaly` | `AnomalyComponent` | AI_AUDITOR/ADMIN |
| `/tenant-kpi` | `TenantKpiComponent` | ADMIN |

Das sind 19 konkrete Routen plus der Login-Callback. Die im
Setup-Prompt erwaehnte "37er-Liste" existiert nicht in diesem
Repo und wurde korrekt als "nicht annehmen, sondern aus Sidebar
lesen" gekennzeichnet.

**Settings-Rubriken (Iteration 22)**: Die Settings-Seite ist
eine einzelne Komponente mit internen Rubriken, keine
`/settings/*`-Unterrouten. Der Prompt erwaehnt aber
"`/settings/*`-Rubriken aus Iteration 22" - hier muss die
Explorations-Logik die Komponente oeffnen und die Rubriken-
Tabs/Accordion intern ansteuern, nicht versuchen, eigene
Sub-Routen anzusteuern.

## 5 Playwright im Repo

- **Nicht vorhanden**. Weder `@playwright/test` noch
  `playwright.config.*` existieren. Grep ueber
  `cvm-frontend/package.json` und Root ergibt keinen Treffer.
- **Aufzunehmende Version**: `@playwright/test` ist ab Angular
  18 kompatibel; `@playwright/test@^1.45.0` ist fuer unsere
  Node-22-Toolchain passend.
- **Test-Runner-Konvention im Repo**: Backend nutzt JUnit 5,
  Frontend heute nur Karma/Jasmine (Unit). Ein **separates**
  Playwright-Setup neben den bestehenden Karma-Unit-Tests ist
  der saubere Weg (`cvm-frontend/e2e/`-Ordner oder
  Top-Level-`scripts/`). Der Setup-Prompt legt letzteres nahe
  (`scripts/explore-ui.ts`).

## 6 Docs-Struktur

- CLAUDE.md Abschnitt 8 und Abschnitt 10 fordern
  `docs/YYYYMMDD/` als Ablage.
- Bestehende Ordner: `docs/20260417/` (Iter 00-21) und
  `docs/20260418/` (Iter 22-28e).
- Keine `docs/prompts/`-Struktur bisher vorhanden. Der
  Setup-Prompt verlangt `docs/prompts/ui-exploration.md` neu
  anzulegen.
- Audit-Reports heissen `docs/YYYYMMDD/iteration-NN-*.md`.
  Fortschritts-Report-Konvention: `*-fortschritt.md`,
  Test-Summary `*-test-summary.md`.

## 7 GitHub Actions / CI

- **`.github/workflows/`-Verzeichnis existiert nicht.**
  `ls .github/` ergibt "no .github".
- Der Haupt-CI-Pfad ist **GitLab CI** (`.gitlab-ci.yml`,
  `.gitlab-ci-cvm-gate.yml`).
- Der Setup-Prompt verlangt ausdruecklich
  `.github/workflows/ui-exploration.yml`. Damit legt der
  Prompt die erste GitHub-Actions-Datei an; das ist bewusst,
  weil das Repo laut Branch-Konvention
  (`sebolber/portalsecurityscan`) auf GitHub gespiegelt wird.

## 8 Sandbox-Inventur

Das ist der kritische Punkt fuer die Entscheidung, ob Teile
2-4 heute umsetzbar sind:

| Werkzeug | Status im Sandbox |
|---|---|
| Node 22.22.2 + npm/npx | **da** (`/opt/node22/bin`) |
| Maven Wrapper (`./mvnw`) | **da** |
| Docker-CLI (`/usr/bin/docker`) | **da, aber Daemon nicht erreichbar** (`connect: no such file or directory` auf `/var/run/docker.sock`) |
| docker-compose | Nicht pruefbar (Daemon fehlt) |
| Chromium / Chrome / Firefox | **nicht installiert** (`command -v chromium …` ergibt nichts) |
| Playwright | **nicht installiert**; `npx playwright install chromium` wuerde eine eigene Chromium-Binary mit Shared-Libs brauchen. Vorhanden: `libnss3.so`, `libasound.so.2` - nicht klar, ob alle transitiven Deps (z.B. `libdrm`, `libxkbcommon`) da sind. |
| System-Font-Stack fuer Headless-Screenshots | unbekannt |

## 9 Bewertung der Stopp-Kriterien aus dem Setup-Prompt

Aus dem Prompt Abschnitt "Stopp-Kriterien":

> "Das Frontend lokal nicht startet oder nicht mit dem Backend spricht."

Der Check dafuer ist:

1. `docker compose up -d` → **nicht moeglich** (Daemon fehlt).
2. Damit kein Postgres, kein Keycloak, keine Backend-DB-Connection.
3. Ohne DB kein Spring-Boot-Start.
4. Ohne Backend kein OIDC-Login, keine API-Antworten fuer die
   Exploration.
5. Ohne Chromium auch bei Backend-Start kein Playwright-Lauf.

Zusaetzlich Teil 4 der Anforderung:

> "Führe `scripts/explore-ui.ts --target=local` aus. (…) Wichtig
> ist nur: **das Skript läuft durch und erzeugt Report und
> Screenshots**."

Das laesst sich im aktuellen Sandbox **nicht ehrlich erfuellen**.
Ein Skript zu schreiben, das zwar syntaktisch korrekt ist, aber
niemals lokal verifiziert wurde, verstoesst gegen die explizite
Vorgabe aus dem Prompt:

> "Keine Schummel-Varianten: kein Skript, das gegen Mocks statt
> echte App läuft. Kein Workflow, der den Launch überspringt.
> Kein Platzhalter-`return 0`."

## 10 Empfehlung (zur Entscheidung durch Sebastian)

Es gibt drei saubere Wege:

**A. Setup in einer Umgebung mit Docker + Chromium durchfuehren.**
Der aktuelle Remote-Sandbox hat weder Docker-Daemon noch einen
Browser. Wenn ich das Setup auf deinem Laptop oder einer
Cloud-VM mit Docker ausfuehren darf, kann ich den kompletten
Pfad 1-4 inklusive Verifikations-Lauf absolvieren.

**B. Skripte schreiben + CI-Workflow anlegen, aber den lokalen
Verifikations-Lauf (Teil 4) explizit auslassen und
dokumentieren.** Dafuer muesste der Prompt gelockert werden,
denn er verlangt explizit einen Baseline-Lauf. Ich kann das
anbieten, aber ich wuerde das ohne deine Zustimmung nicht tun -
genau deshalb sagst du am Anfang "Stopp-freundlich formuliert".

**C. Nur die Skript-Stubs + CLAUDE.md-Aenderungen + Prompt-
Baustein anlegen, der Skript-Lauf und Baseline-Report entfaellt
und wird als "offen" markiert.** Das ist C im Grunde eine Variante
von B, aber mit deutlicher Kennzeichnung: "Iteration angefangen,
Baseline fehlt wegen Sandbox-Limitation".

Meine Empfehlung: **A**, sobald du eine Umgebung hast, in der
Docker + Chromium laufen. Alternativ **C** mit ausfuehrlichem
"Baseline folgt, sobald Infrastruktur steht"-Vermerk.

## 11 Was ich nicht gepruft habe, aber vermute

- Der Setup-Prompt erwaehnt "alle `/settings/*`-Rubriken aus
  Iteration 22". Ich habe nicht in den Iteration-22-Spezifikations-
  Prompt hineingeschaut, um zu sehen, ob die Rubriken als echte
  Sub-Routen gedacht waren oder wie heute als Tabs in einer
  Komponente. Wenn Sebastian bestaetigt, dass es Tabs/Accordion
  innerhalb einer einzigen Route sind, muss die Exploration diese
  per Click-Steuerung durchgehen, nicht per URL.
- Die Anforderung `--target=ci` soll andere Host/Port-Kombis
  abdecken. In der CI wuerden Services in GitHub-Actions-
  `services:`-Blocks laufen, mit denselben Ports (5432/8080/
  8081/4200) - aber als `services: postgres`/`keycloak`/… statt
  `localhost`. Fuer `--target=ci` muessten die URLs per
  Environment-Variablen parametrisiert werden.
- Installation `@playwright/test` im bestehenden Frontend-
  `package.json` versus als eigenes Root-`package.json`.
  Saubere Trennung: eigenes `package.json` unter
  `scripts/explore-ui/` oder bestehendes Frontend-Paket
  erweitern. Beide haben Vor-/Nachteile; ich wuerde das separate
  Paket bevorzugen, um die Angular-Build-Chain nicht zu stoeren.

---

## 12 Konkrete Frage an Sebastian

1. **Infrastruktur**: Ist der naechste Lauf des Setup-Prompts
   auf einer Umgebung mit Docker + installierter Chromium-Runtime
   geplant (Laptop, CI-Runner)? Dann ist Pfad **A** der richtige.
2. **Skript-Installation**: Eigenes `package.json` unter
   `scripts/explore-ui/` oder Dev-Dependency im bestehenden
   `cvm-frontend/package.json`?
3. **Settings-Rubriken**: Sind die `/settings/*`-Rubriken aus
   Iteration 22 heute tabs innerhalb einer Komponente
   (meine Lesart) oder als echte Unterrouten vorgesehen?

Ich warte mit Teil 2-4, bis diese drei Punkte geklaert sind.
