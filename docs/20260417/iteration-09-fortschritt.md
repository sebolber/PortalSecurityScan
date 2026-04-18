# Iteration 09 – SMTP-Alerts – Fortschritt

**Jira**: CVM-18
**Status**: umgesetzt im Minimal-Scope.
**Branch**: `claude/continue-next-session-prompt-CSwwq`.

## Zusammenfassung

Das Alert-Subsystem ist deterministisch geliefert: Regeln werden
persistiert, ein Evaluator wertet `AlertContext`-Trigger gegen die
aktiven Regeln aus, ein Cooldown verhindert Mehrfach-Versand,
Templates werden gerendert und ein Mail-Adapter (Spring Mail) bzw.
ein Noop-Bean kommt je nach Profil zum Einsatz. Eskalation
(T1/T2) laeuft als Cron-Job alle 5&nbsp;min und triggert dieselben
Regeln. Fronted-Banner zeigt T2-Eskalationen als roter Streifen
oberhalb der Shell.

## Umgesetzt

### Persistenz
- Flyway `V0011__alerts.sql` (drei Tabellen: `alert_rule`,
  `alert_event`, `alert_dispatch`).
- Entities + Repos: `AlertRule`, `AlertEvent`, `AlertDispatch` und
  drei JPA-Repos.

### Domain
- Enums `AlertSeverity` (INFO/WARNING/CRITICAL) und
  `AlertTriggerArt` (FINDING_NEU, ASSESSMENT_PROPOSED,
  ASSESSMENT_APPROVED, ESKALATION_T1, ESKALATION_T2, KEV_HIT).

### Application
- `AlertConfig` (Properties + Clock + Default-NoopMailSender).
- `AlertContext` (DTO).
- `MailSenderPort` + `MailDispatchException`.
- `AlertTemplateRenderer` (lightweight, ohne Thymeleaf,
  HTML-Escaping) inkl. `toPlainText`-Fallback.
- Templates (`alert-kritisch-unbewertet`, `alert-shutdown-empfehlung`,
  `alert-kev-hit`).
- `AlertEvaluator` (sucht Regeln, prueft Cooldown, ruft Dispatcher,
  fuehrt suppressedCount).
- `AlertDispatcher` (rendert, ruft Mail-Sender, schreibt
  `AlertDispatch`-Audit, beachtet Dry-Run).
- `AlertRuleService` (Listing + Anlegen).
- `AlertRuleView` (Read-Projektion).
- `AlertEventListeners` (`AssessmentApprovedEvent`,
  `ScanIngestedEvent` -> Evaluator).
- `AlertEscalationJob` (Cron 5&nbsp;min, T1/T2-Schwellen).
- `AlertBannerService` (T2-Banner-Status fuer das Frontend).

### Integration
- `SpringMailSenderAdapter` (Spring Mail JavaMailSender), aktiv
  nur wenn `JavaMailSender` Bean da ist. Sonst greift der
  Noop-Sender aus `AlertConfig`.

### API
- `AlertsController`:
  - `GET /api/v1/alerts/rules`
  - `POST /api/v1/alerts/rules` (PreAuthorize CVM_ADMIN)
  - `POST /api/v1/alerts/test` (Dry-Run-Trigger)
  - `GET /api/v1/alerts/banner`
- DTOs als Records inkl. Bean-Validation.
- `AlertsTestApi` Slice-Konfig + `AlertsControllerWebTest`.

### Frontend
- `AlertBannerService` (HTTP-Client fuer Banner-Status).
- `AlertBannerComponent` (rotes Banner oberhalb der Shell).
- `ShellComponent` triggert Polling alle 60&nbsp;s.
- Karma-Spec `alert-banner.service.spec.ts`.

### Konfig
- `application.yaml` erhaelt `cvm.alerts.*`-Defaults
  (`mode`, `from`, `eskalation.t1-minutes`, `eskalation.t2-minutes`).

## Pragmatische Entscheidungen

- **Templates** ohne Thymeleaf, weil Application-Modul Thymeleaf
  bisher nicht kennt und nur statische Vorlagen gebraucht werden.
  Eigene `AlertTemplateRenderer`-Implementation (Regex +
  HTML-Escape, plus `toPlainText`-Fallback). Falls dynamische
  Logik (if/each) noetig wird, kann auf Thymeleaf umgestellt
  werden, ohne dass die Templates-Pfade sich aendern.
- **Migration** ist `V0011`, nicht `V0009` wie im Iterations-Prompt.
  Begruendung: `V0009`/`V0010` sind in Iteration 05/06 vergeben;
  `V0011` ist die naechste freie Nummer (vgl. CLAUDE.md-Hinweis).
- **AssessmentExpiredEvent** noch nicht eingefuehrt. Re-Vorschlag-
  Pfad bei EXPIRED bleibt offen (siehe offene-punkte.md, Iteration
  09-Backlog).
- **Vier-Augen-Pending-Alert** (z.&nbsp;B. ESKALATION_PENDING_REVIEW)
  ist nicht Teil dieser Iteration; T1/T2 fallen pragmatisch auf
  CRITICAL-Vorschlaege im Status PROPOSED/NEEDS_REVIEW.
- **YAML-Bootstrap** der Regeln ist nicht enthalten; Regeln werden
  ueber den REST-Endpoint angelegt. YAML-Loader folgt mit Iteration
  21 (Mandanten-/CICD-Bootstrap).
- **Audit-Trail-Tabelle** existiert noch nicht; `alert_dispatch`
  dient als fachlicher Audit-Log fuer ausgehende Mails.

## Tests

### Backend
- `AlertEvaluatorTest` (5 Tests): Erstes Feuern, Cooldown,
  Cooldown-Ablauf, keine Regeln, Dispatcher-Fehler.
- `AlertDispatcherTest` (3 Tests): Dry-Run, Real-Modus, Sender-Fehler.
- `AlertTemplateRendererTest` (3 Tests): Render mit Escaping,
  unbekanntes Template, `toPlainText`.
- `AlertEscalationJobTest` (3 Tests): T2-Eskalation, T1-Eskalation,
  HIGH wird ignoriert.
- `AlertBannerServiceTest` (2 Tests): T2-aktiv und -inaktiv.
- `AlertsControllerWebTest` (3 Tests): Liste, Test-Trigger, Banner.

`./mvnw -T 1C test` -> **BUILD SUCCESS** (157 Tests, 5 Docker-skip).

### Frontend
- `npx ng build` -> gruen (Initial 1.92 MB,
  `queue-component`-Chunk 27 kB, kein neuer Lazy-Chunk fuer Banner
  da Shell-bound).
- `npx ng lint` -> `All files pass linting`.
- Karma-Spec `alert-banner.service.spec.ts` schreibend
  (Headless-Chrome bleibt CI-Aufgabe).

## Nicht im Scope

- AlertRule-YAML-Bootstrap (Iteration 21).
- Teams/Slack/Jira-Tickets.
- KI-personalisierte Texte (Iteration 14).
- Vier-Augen-Pending-spezifischer Alert.
- Persistenz-Integrationstest fuer V0011 (Docker-Skip).

## Ausblick Iteration 10

PDF-Abschlussbericht (CVM-19): Nutzt openhtmltopdf + Thymeleaf-
Templates (ggf. Thymeleaf jetzt als Dependency aufnehmen, falls die
Reports komplexere Logik brauchen).

## Dateien (wesentlich)

- `cvm-domain/.../enums/AlertSeverity.java`
- `cvm-domain/.../enums/AlertTriggerArt.java`
- `cvm-persistence/.../db/migration/V0011__alerts.sql`
- `cvm-persistence/.../alert/AlertRule.java`
- `cvm-persistence/.../alert/AlertEvent.java`
- `cvm-persistence/.../alert/AlertDispatch.java`
- `cvm-persistence/.../alert/AlertRuleRepository.java`
- `cvm-persistence/.../alert/AlertEventRepository.java`
- `cvm-persistence/.../alert/AlertDispatchRepository.java`
- `cvm-application/.../alert/AlertConfig.java`
- `cvm-application/.../alert/AlertContext.java`
- `cvm-application/.../alert/MailSenderPort.java`
- `cvm-application/.../alert/MailDispatchException.java`
- `cvm-application/.../alert/AlertTemplateRenderer.java`
- `cvm-application/.../alert/AlertEvaluator.java`
- `cvm-application/.../alert/AlertDispatcher.java`
- `cvm-application/.../alert/AlertRuleService.java`
- `cvm-application/.../alert/AlertRuleView.java`
- `cvm-application/.../alert/AlertEventListeners.java`
- `cvm-application/.../alert/AlertEscalationJob.java`
- `cvm-application/.../alert/AlertBannerService.java`
- `cvm-application/src/main/resources/cvm/alerts/alert-*.html`
- `cvm-integration/.../alert/SpringMailSenderAdapter.java`
- `cvm-api/.../alert/AlertsController.java`
- `cvm-api/src/test/.../alert/AlertsTestApi.java`
- `cvm-api/src/test/.../alert/AlertsControllerWebTest.java`
- `cvm-app/src/main/resources/application.yaml` (cvm.alerts.*)
- `cvm-frontend/.../core/alerts/alert-banner.service.ts`
- `cvm-frontend/.../shell/alert-banner.component.ts`
- `cvm-frontend/.../shell/shell.component.ts/.html`
