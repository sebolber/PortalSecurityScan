# Prompt fuer die naechste Claude-Code-Session (Iteration 08+, autonom)

Kopiere den Block zwischen den `---` in ein neues Kontextfenster. Die
Session arbeitet die Iterationen 08 bis 21 ohne Rueckfragen ab und
bricht nur bei den unten gelisteten Stopp-Kriterien ab.

---

Du bist Claude Code und arbeitest am CVE-Relevance-Manager (CVM) der
adesso health solutions GmbH. Repo: `sebolber/PortalSecurityScan`.

## Stand
- `main` ist aktuell auf Commit `8e0698d`.
- Iterationen 00-07 abgeschlossen und produktiv lauffaehig
  (`scripts/start.sh main` faehrt postgres+keycloak+mailhog +
  Spring-Boot-Backend + Angular-DevServer hoch).
- Letzte Iteration (07) hat zwei Bug-Klassen aufgedeckt, die jetzt
  abgesichert sind:
  - Spring-Bean-Wiring per ArchUnit-Test (`SpringBeanKonstruktorTest`).
  - Frontend-Bootstrap-Race (Config vs. Keycloak): jetzt EIN
    sequenzieller `APP_INITIALIZER`.

Test-Bilanz: `./mvnw -T 1C test` -> BUILD SUCCESS. 148 Tests, 137 gruen,
11 Docker-skip. `npx ng build` und `npx ng lint` gruen. Karma- und
Playwright-Laufzeit-Tests benoetigen Headless-Chrome (Sandbox: nicht
verfuegbar -> dokumentieren, nicht abbrechen).

## Auftrag
Setze die Iterationen **08 bis 21 sequentiell** um, jeweils mit
zugehoeriger Iterations-Datei `NN-*.md` im Repo-Root. Stelle keine
Rueckfragen, treffe Entscheidungen pragmatisch und dokumentiere sie
im Fortschrittsbericht. Nach Iteration 21 ist das Projekt fertig.

Reihenfolge (verbindlich):
- 08-Bewertungs-Queue-UI.md  (CVM-17, Abh.: 06, 07)
- 09-SMTP-Alerts.md          (CVM-18, Abh.: 06)
- 10-Abschlussbericht-PDF.md (CVM-19, Abh.: 06)
- 11-LLM-Gateway.md          (CVM-30, Abh.: M1)
- 12-RAG-pgvector.md         (CVM-31, Abh.: 11)
- 13-KI-Vorbewertung.md      (CVM-32, Abh.: 12)
- 14-Copilot-und-Delta-Summary.md (CVM-33, Abh.: 13)
- 15-Reachability-Agent.md   (CVM-40, Abh.: 13)
- 16-Fix-Verifikation.md     (CVM-41, Abh.: 13)
- 17-KI-Regel-Extraktion.md  (CVM-42, Abh.: 13)
- 18-KI-Anomalie-Check-und-Profil-Assistent.md (CVM-43, Abh.: 13)
- 19-NL-Query-Dashboard-und-Executive-Reports.md (CVM-50, Abh.: 14)
- 20-VEX-und-Waiver.md       (CVM-51, Abh.: 06)
- 21-Mandanten-CICD-KPIs.md  (CVM-52, Abh.: alle)

Iterations-Roadmap und Abhaengigkeiten stehen in
`README-Iterationsplan.md`.

## Arbeitsregeln (gelten fuer jede Iteration)
1. **Verstehen vor Codieren**: lies `CLAUDE.md`, die jeweilige
   `NN-*.md`, den letzten Fortschrittsbericht, `offene-punkte.md` und
   die referenzierten Konzept-Abschnitte. Falls
   `docs/konzept/CVE-Relevance-Manager-Konzept-v0.2.md` weiterhin ein
   Platzhalter ist, dokumentiere die getroffenen Annahmen.
2. **TDD**: schreibe zuerst Tests (rot), dann minimalen Produktionscode
   (gruen), dann Refaktor. Aendere niemals einen roten Test, um ihn
   gruen zu bekommen.
3. **Modulgrenzen** (ArchUnit) sind hart. Insbesondere:
   - `cvm-api -> cvm-persistence` ist verboten -> Mapping ueber
     `*View`-Records im Application-Modul (siehe ProfileView, RuleView,
     FindingQueueView).
   - Spring-Beans mit mehreren Konstruktoren brauchen genau einen mit
     `@Autowired`. Der `SpringBeanKonstruktorTest` ist hart.
4. **Persistenz**: nur vorwaerts-Migrationen. Naechste freie
   Flyway-Nummer ist `V0011`. Nutze `pgvector` ab Iteration 12.
5. **WebMvcTest-Slices** brauchen jeweils eine eigene
   `*TestApi`-Slice-Config im Test-Paket (Muster: `RulesTestApi`,
   `AssessmentsTestApi`, `FindingsTestApi`).
6. **Fachsprache**: deutsche `@DisplayName`, deutsche Fehlermeldungen,
   deutsche Begruendungen in Domain-/Service-Code; Bezeichner Englisch.
7. **Conventional Commits** auf Deutsch + Jira-Key im Footer:
   ```
   feat(queue): UI fuer Bewertungs-Queue mit Approve-Override

   CVM-17
   ```
8. **Reports** pro Iteration:
   - `docs/YYYYMMDD/iteration-NN-plan.md`
   - `docs/YYYYMMDD/iteration-NN-fortschritt.md`
   - `docs/YYYYMMDD/iteration-NN-test-summary.md`
   - `docs/YYYYMMDD/offene-punkte.md` (kumulativ erweitern)
9. **Abschluss jeder Iteration**:
   - `./mvnw -T 1C test` -> BUILD SUCCESS.
   - Bei Frontend-Aenderungen zusaetzlich `npx ng build` und
     `npx ng lint` gruen.
   - Commit + Push auf den vom System vorgegebenen `claude/...`-Branch.
   - Anschliessend `main` per fast-forward updaten und pushen.
   - Direkt zur naechsten Iteration **ohne Nachfrage**.
10. **Nach Iteration 21**: Abschlussbericht
   `docs/YYYYMMDD/abschluss-bericht.md`, der Meilensteine M1-M4 und
   alle abgearbeiteten Iterationen referenziert.

## Sandbox-Limits (nicht abbrechen, nur dokumentieren)
- **Kein Docker-Daemon**: Testcontainers-Tests bleiben via
  `@EnabledIf("...DockerAvailability#isAvailable")` geskippt. Offene
  Persistenz-Integrationstests fuer V0010+ in `offene-punkte.md`
  vermerken.
- **Kein Headless-Chrome**: Karma-Specs werden geschrieben und mit
  `ng build` mitkompiliert; ein `ng test`-Lauf wird als "in CI nachholen"
  dokumentiert.
- **Kein Anthropic-API-Key in der Sandbox** (Iterationen 11+):
  LLM-Adapter werden gegen WireMock implementiert und mit
  Mock-Responses getestet. Echte Calls sind explizit in
  `offene-punkte.md` als Backlog markiert.
- **Kein pgvector-Container**: RAG-Logik (12) wird trotzdem implementiert
  und Repository-/Service-Tests bleiben Docker-geskippt. ArchUnit-
  Modulgrenzen werden trotzdem hart geprueft.

## Stopp-Kriterien (Iteration abbrechen, Status melden, NICHT pushen)
- Eine geforderte Aenderung wuerde Tests oder Architektur-Regeln
  verletzen, ohne dass eine konforme Alternative existiert.
- Ein Konzept-Widerspruch tritt auf, der weder aus `CLAUDE.md` noch
  aus dem Konzept v0.2 oder den Iterations-Prompts aufloesbar ist.
- Ein Secret muesste im Klartext im Repo liegen.
- Ein KI-Call muesste ohne `ai_call_audit`-Eintrag erfolgen.
- Ein Downgrade-Pfad wuerde das Vier-Augen-Prinzip umgehen.
- `./mvnw -T 1C test` schlaegt nach drei Korrekturversuchen weiterhin
  fehl mit demselben Root-Cause.

In allen anderen Faellen weitermachen, im Zweifel pragmatisch
entscheiden, Entscheidung dokumentieren.

## Hilfreiche Signposts im Repo (Stand main@8e0698d)
- Cascade + Persistenz: `cvm-application/assessment/*` und
  `cvm-application/cascade/CascadeService.java`.
- StateMachine: `AssessmentStateMachine` + `AssessmentWriteService`.
- REST-Patterns: `cvm-api/profile/`, `cvm-api/rules/`, `cvm-api/assessment/`.
- Slice-Test-Configs: `cvm-api/src/test/java/com/ahs/cvm/api/*/`*`TestApi.java`.
- Frontend-Patterns: Auth/Guard/Interceptor in
  `cvm-frontend/src/app/core/auth/`, ApiClient + Snackbar-Errorhandler
  in `cvm-frontend/src/app/core/api/`, Shared-UI in
  `cvm-frontend/src/app/shared/components/` (`<ahs-card>`,
  `<ahs-button>`, `<ahs-empty-state>`, `<ahs-severity-badge>`).
- Start: `scripts/start.sh <branch>` (Backend, Frontend, Docker, Logs,
  Cleanup-Trap).
- Konfig-Defaults: `cvm-app/src/main/resources/application.yaml`,
  `cvm-frontend/src/assets/config.json`,
  `infra/keycloak/dev-realm.json` (Realm `cvm-local`, Users
  `t.tester@ahs.test/test`, `a.admin@ahs.test/admin`).

## Backlog (offen aus offene-punkte.md, ggf. innerhalb der passenden
Iteration miterledigen)
- Rollen-Verdrahtung `CVM_PROFILE_AUTHOR`, `CVM_PROFILE_APPROVER`,
  `CVM_RULE_AUTHOR`, `CVM_RULE_APPROVER`, `CVM_REVIEWER` (aktuell nur
  `CVM_ADMIN` / `authenticated()`).
- `condition_json` als JSONB statt TEXT (Iteration 17).
- Dry-Run-Profilhistorie (Iteration 19).
- Regel-Immutable-Versionierung (Iteration 17).
- Resilience4j (Retry/Circuit-Breaker), parallel zu Iteration 11.
- ShedLock fuer Scheduled-Jobs (sobald Cluster).
- Fachkonzept-Platzhalter `docs/konzept/CVE-Relevance-Manager-Konzept-v0.2.md`
  ersetzen oder Annahmen explizit dokumentieren.
- Coverage-Auswertung (JaCoCo + Pitest) fuer Severity-Mapping +
  Cascade-Logik.
- Karma + Playwright in CI (Headless-Chrome-Setup).
- Persistenz-IT fuer V0010 (`valid_until`, `reviewed_by`, EXPIRED).
- AssessmentExpiredEvent + Re-Vorschlag-Pfad (Iteration 09).
- i18n EN-Texte (Iteration 19 oder 21).

## Arbeitsweise
1. Erstelle/wechsle auf den vom System vorgegebenen `claude/...`-Branch.
2. Fuehre Iteration 08 vollstaendig aus (Plan, TDD, Code, Tests,
   Reports, Commit, Push, fast-forward main, push main).
3. Direkt weiter mit Iteration 09. Keine Rueckfrage.
4. Wiederhole bis Iteration 21 abgeschlossen ist.
5. Schreibe `docs/YYYYMMDD/abschluss-bericht.md` mit Meilenstein-Status
   M1-M4 und Liste aller Iterationen.
6. Aktualisiere `docs/next-session-prompt.md` auf "Projekt abgeschlossen,
   nur Pflege/Backlog offen".

---
