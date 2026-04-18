# Prompt fuer die naechste Claude-Code-Session (Iteration 10+, autonom)

Kopiere den Block zwischen den `---` in ein neues Kontextfenster. Die
Session arbeitet die Iterationen 10 bis 21 ohne Rueckfragen ab und
bricht nur bei den unten gelisteten Stopp-Kriterien ab.

---

Du bist Claude Code und arbeitest am CVE-Relevance-Manager (CVM) der
adesso health solutions GmbH. Repo: `sebolber/PortalSecurityScan`.

## Stand
- `main` steht auf Commit `d3921a0` (nach Iteration 09 + Mail-Adapter-Fix).
- Iterationen 00-09 sind produktiv abgeschlossen.
  - 08 (CVM-17): Bewertungs-Queue-UI mit Shortcuts und Vier-Augen.
  - 09 (CVM-18): SMTP-Alerts (Domain, Evaluator, Cooldown, Dispatcher,
    Templates, Eskalation T1/T2, REST, Frontend-Banner).
- `./mvnw -T 1C test` -> BUILD SUCCESS (157 Tests, 5 Docker-skip).
- `npx ng build` + `npx ng lint` gruen.
- **Achtung fuer lokalen Start**: Nach `git pull` muss einmal
  `./mvnw -T 1C clean install -DskipTests` laufen, damit die
  aktualisierten JARs (insb. `cvm-integration` mit
  `SpringMailSenderAdapter`) im lokalen Maven-Repo landen. Erst dann
  `scripts/start.sh main` starten. Ohne das schlaegt der Start mit
  `UnsatisfiedDependencyException` fuer `MailSenderPort` fehl.
- Erkannter Folgebug nach Neustart ist bereits gefixt: `cvm-integration`
  `SpringMailSenderAdapter` hatte `@ConditionalOnBean(JavaMailSender)`,
  das wurde entfernt (siehe Commit `d3921a0`).

## Auftrag
Setze die Iterationen **10 bis 21 sequentiell** um, jeweils mit
zugehoeriger Iterations-Datei `NN-*.md` im Repo-Root. Stelle keine
Rueckfragen, treffe Entscheidungen pragmatisch und dokumentiere sie
im Fortschrittsbericht. Nach Iteration 21 ist das Projekt fertig.

Reihenfolge (verbindlich):
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

## Arbeitsregeln (gelten fuer jede Iteration)
1. **Verstehen vor Codieren**: lies `CLAUDE.md`, die jeweilige
   `NN-*.md`, den letzten Fortschrittsbericht, `docs/20260417/
   offene-punkte.md` und die referenzierten Konzept-Abschnitte.
2. **TDD**: zuerst Tests (rot), dann minimalen Produktionscode (gruen),
   dann Refaktor. Keine Test-Anpassung, um Rot zu umgehen.
3. **Modulgrenzen** (ArchUnit) sind hart:
   - `cvm-api -> cvm-persistence` ist verboten. Mapping ueber
     `*View`-Records im Application-Modul.
   - `cvm-application -> cvm-api` ist verboten.
   - `cvm-integration -> cvm-api` ist verboten.
   - `SpringBeanKonstruktorTest` verlangt genau einen
     `@Autowired`-Konstruktor bei mehreren Konstruktoren.
4. **Persistenz**: nur vorwaerts-Migrationen. Naechste freie
   Flyway-Nummer ist `V0012`. Nutze `pgvector` ab Iteration 12.
5. **WebMvcTest-Slices** brauchen eine `*TestApi`-Slice-Config im
   Test-Paket (Muster: `RulesTestApi`, `FindingsTestApi`, `AlertsTestApi`).
6. **Conditional-Beans mit Vorsicht**: `@ConditionalOnBean` auf
   `@Component`-Klassen wird zu frueh evaluiert (siehe Alert-Adapter-
   Bug). Stattdessen `@ConditionalOnMissingBean` fuer Fallback-Beans
   oder in dedizierten `@Configuration`-Klassen einsetzen.
7. **Fachsprache**: deutsche `@DisplayName`, deutsche Fehlermeldungen,
   deutsche Begruendungen im Domain-/Service-Code; Bezeichner
   Englisch.
8. **Conventional Commits** auf Deutsch + Jira-Key im Footer:
   ```
   feat(pdf): Abschlussbericht mit openhtmltopdf

   CVM-19
   ```
9. **Reports** pro Iteration unter `docs/YYYYMMDD/`:
   - `iteration-NN-plan.md`
   - `iteration-NN-fortschritt.md`
   - `iteration-NN-test-summary.md`
   - `offene-punkte.md` kumulativ erweitern (oben neuer Abschnitt).
10. **Abschluss jeder Iteration**:
    - `./mvnw -T 1C test` -> BUILD SUCCESS.
    - Bei Frontend-Aenderungen: `npx ng build` und `npx ng lint` gruen.
    - Commit + Push auf den vom System vorgegebenen `claude/...`-Branch.
    - Anschliessend `main` per fast-forward updaten und pushen.
    - **Wenn ein neues JAR entsteht** (neues Modul, geaenderte Module-
      Abhaengigkeit), im Fortschrittsbericht explizit darauf hinweisen,
      dass `mvn install` vor dem naechsten Dev-Start noetig ist.
    - Direkt zur naechsten Iteration, ohne Nachfrage.
11. **Nach Iteration 21**: Abschlussbericht
    `docs/YYYYMMDD/abschluss-bericht.md`, der Meilensteine M1-M4 und
    alle abgearbeiteten Iterationen referenziert. Anschliessend
    `docs/next-session-prompt.md` auf "Projekt abgeschlossen, nur
    Pflege/Backlog offen" umstellen.

## Sandbox-Limits (nicht abbrechen, nur dokumentieren)
- **Kein Docker-Daemon**: Testcontainers-Tests bleiben via
  `@EnabledIf("...DockerAvailability#isAvailable")` geskippt. Offene
  Persistenz-IT fuer V0011/V0012+ in `offene-punkte.md` vermerken.
- **Kein Headless-Chrome**: Karma-Specs werden geschrieben und mit
  `ng build` mitkompiliert; `ng test`-Lauf ist "in CI nachholen".
- **Kein Anthropic-API-Key** (Iterationen 11+): LLM-Adapter werden
  gegen WireMock implementiert und mit Mock-Responses getestet.
  Echte Calls sind in `offene-punkte.md` als Backlog markiert.
- **Kein pgvector-Container**: RAG-Logik (12) wird trotzdem
  implementiert; Repository-/Service-Tests bleiben Docker-geskippt.
  ArchUnit-Modulgrenzen werden trotzdem hart geprueft.

## Stopp-Kriterien (Iteration abbrechen, Status melden, NICHT pushen)
- Eine geforderte Aenderung wuerde Tests oder Architektur-Regeln
  verletzen, ohne dass eine konforme Alternative existiert.
- Ein Konzept-Widerspruch tritt auf, der weder aus `CLAUDE.md` noch
  aus dem Konzept v0.2 oder den Iterations-Prompts aufloesbar ist.
- Ein Secret muesste im Klartext im Repo liegen.
- Ein KI-Call muesste ohne `ai_call_audit`-Eintrag erfolgen.
- Ein Downgrade-Pfad wuerde das Vier-Augen-Prinzip umgehen.
- `./mvnw -T 1C test` schlaegt nach drei Korrekturversuchen weiter
  fehl mit demselben Root-Cause.

## Hilfreiche Signposts im Repo (Stand main@d3921a0)
- Cascade + Persistenz: `cvm-application/assessment/*` und
  `cvm-application/cascade/CascadeService.java`.
- StateMachine: `AssessmentStateMachine` + `AssessmentWriteService`.
- REST-Patterns: `cvm-api/profile/`, `cvm-api/rules/`, `cvm-api/
  assessment/`, `cvm-api/alert/`.
- Slice-Test-Configs: `cvm-api/src/test/java/.../*TestApi.java`.
- Alert-Subsystem (Referenz-Pattern fuer Clock/Cooldown/Audit):
  `cvm-application/alert/*`, `cvm-integration/alert/*`.
- Queue-UI (Referenz-Pattern fuer Signal-Store + HTTP-Adapter):
  `cvm-frontend/src/app/features/queue/*`.
- Frontend-Banner (Shell-bound Polling-Service):
  `cvm-frontend/src/app/core/alerts/*`,
  `cvm-frontend/src/app/shell/alert-banner.component.ts`.
- Start: `scripts/start.sh <branch>` (Backend, Frontend, Docker,
  Logs, Cleanup-Trap). Bei neuen Modulen oder geaenderten
  Abhaengigkeiten vorher `./mvnw -T 1C clean install -DskipTests`.
- Konfig: `cvm-app/src/main/resources/application.yaml` (enthaelt
  seit Iteration 09 `cvm.alerts.*`),
  `cvm-frontend/src/assets/config.json`,
  `infra/keycloak/dev-realm.json` (Realm `cvm-local`, Users
  `t.tester@ahs.test/test`, `a.admin@ahs.test/admin`).

## Offene Punkte (aus docs/20260417/offene-punkte.md, innerhalb der passenden Iteration miterledigen)
- Rollen-Verdrahtung `CVM_PROFILE_AUTHOR`, `CVM_PROFILE_APPROVER`,
  `CVM_RULE_AUTHOR`, `CVM_RULE_APPROVER`, `CVM_REVIEWER` (aktuell nur
  `CVM_ADMIN` / `authenticated()`).
- `condition_json` als JSONB statt TEXT (Iteration 17).
- Dry-Run-Profilhistorie (Iteration 19).
- Regel-Immutable-Versionierung (Iteration 17).
- Resilience4j (Retry/Circuit-Breaker), parallel zu Iteration 11.
- ShedLock fuer Scheduled-Jobs (sobald Cluster).
- Fachkonzept-Platzhalter `docs/konzept/CVE-Relevance-Manager-
  Konzept-v0.2.md` ersetzen oder Annahmen explizit dokumentieren.
- Coverage-Auswertung (JaCoCo + Pitest) fuer Severity-Mapping +
  Cascade-Logik.
- Karma + Playwright in CI (Headless-Chrome-Setup).
- Persistenz-IT fuer V0010/V0011 (Docker).
- AssessmentExpiredEvent + Re-Vorschlag-Pfad.
- i18n EN-Texte (Iteration 19 oder 21).
- AlertRule-YAML-Bootstrap (Iteration 21).
- ESKALATION_PENDING_REVIEW-Trigger (optional, Iteration 13/14).
- Banner i18n (aktuell hart deutsch).
- Bundle-Budget-Warnung im Frontend (Initial > 1.05 MB).
- Produkt-/Umgebungs-Dropdowns in der Queue statt UUID-Freitextfelder
  (Iteration 19 mit echten Produkt-Endpunkten).
- Bulk-Approve-Endpoint fuer Queue-UI Batch-Checkboxen.
- Playwright-E2E fuer Queue-Szenarien.
- Manueller MailHog-Smoke fuer Alerts mit `cvm.alerts.mode=real`.

## Arbeitsweise
1. Erstelle/wechsle auf den vom System vorgegebenen `claude/...`-Branch.
2. Fuehre Iteration 10 vollstaendig aus (Plan, TDD, Code, Tests,
   Reports, Commit, Push, fast-forward main, push main).
3. Direkt weiter mit Iteration 11. Keine Rueckfrage.
4. Wiederhole bis Iteration 21 abgeschlossen ist.
5. Schreibe `docs/YYYYMMDD/abschluss-bericht.md` mit Meilenstein-
   Status M1-M4 und Liste aller Iterationen.
6. Aktualisiere `docs/next-session-prompt.md` auf "Projekt
   abgeschlossen, nur Pflege/Backlog offen".

---
