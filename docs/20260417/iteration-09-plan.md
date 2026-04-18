# Iteration 09 – SMTP-Alerts – Plan

**Jira**: CVM-18
**Abhaengigkeit**: 06 (Bewertungs-Workflow + Events).
**Ziel**: Deterministische Mail-Alerts mit Cooldown und
Eskalationsstufen.

## Pragmatische Entscheidungen

1. **Migration**: V0011 (laut CLAUDE.md naechste freie Nummer; der
   Iterations-Prompt nennt V0009, was im Konflikt zur Realitaet steht
   - dokumentiert).
2. **Konfigurations-Schema**: AlertRule wird wie Profile per YAML
   einlesbar, aber initial nur via REST-POST gepflegt. YAML-Datei-
   Bootstrapping ist nicht im Scope (Iteration 21).
3. **Mail-Versand**: Spring `JavaMailSender`. In Tests via
   `cvm.alerts.mode=dry-run` (Default), echte Sends im Profil `dev`
   gegen MailHog (Port 1025).
4. **Templates**: Drei Thymeleaf-Templates unter
   `cvm-application/src/main/resources/cvm/alerts/`. Plain-Text-
   Fallback wird per `Jsoup`-style Strip oder einfacher String-Reduktion
   nicht generiert; HTML reicht (MailHog bekommt Multipart mit text +
   html). Pragmatisch: Text-Body = HTML stripped via simpler Regex.
5. **Cooldown**: Persistiert in `alert_event` (rule_id, trigger_key,
   last_fired_at). Vor Mail-Versand wird gegen `Clock` geprueft; Tests
   nutzen `Clock.fixed(...)`.
6. **Eskalation T1/T2**: Per Cron-Job (alle 5 min) wird die Queue auf
   ueber-faellige Vorschlaege geprueft. T1 = 2 h, T2 = 6 h, jeweils
   konfigurierbar in `application.yaml`. T2 setzt zusaetzlich ein
   Banner-Flag, das das Frontend per `GET /api/v1/alerts/banner` lesen
   kann.
7. **Frontend-Banner**: Shell-Komponente fragt den Banner-Endpoint
   beim Start (und alle 60 s) und zeigt einen roten Streifen, wenn
   T2-Eskalationen offen sind.
8. **Audit-Trail**: Jeder Mail-Versand erzeugt einen
   `AlertDispatch`-Eintrag. Eine separate `AuditTrail`-Tabelle gibt
   es noch nicht (siehe offene-punkte.md, Iteration 21);
   `alert_dispatch` dient als fachlicher Audit-Log fuer Alerts.
9. **REST**:
   - `GET /api/v1/alerts/rules` (alle Regeln)
   - `POST /api/v1/alerts/rules` (Admin)
   - `POST /api/v1/alerts/test` (Dry-Run)
   - `GET /api/v1/alerts/banner` (Banner-Status)
10. **ArchUnit**: Alert-Domain in `cvm-application/alert`,
    Persistenz in `cvm-persistence/alert`. Neue Entity-Eintrag in
    `PersistenceConfig` falls noetig.

## Komponenten und Dateien

### Backend
- `cvm-domain/.../enums/AlertSeverity.java` (INFO/WARNING/CRITICAL).
- `cvm-domain/.../enums/AlertTriggerArt.java` (FINDING_NEU,
  ASSESSMENT_PROPOSED, ASSESSMENT_APPROVED, ESKALATION_T1,
  ESKALATION_T2, KEV_HIT).
- `cvm-persistence/alert/`:
  - `AlertRule.java` (Entity).
  - `AlertEvent.java` (Cooldown-Tracking).
  - `AlertDispatch.java` (Versand-Audit).
  - `AlertRuleRepository.java`, `AlertEventRepository.java`,
    `AlertDispatchRepository.java`.
- `cvm-application/alert/`:
  - `AlertContext.java` (DTO).
  - `AlertConfig.java` (Spring `@Configuration` mit `Clock`-Bean,
    Eskalations-Properties).
  - `AlertEvaluator.java` (Schwellen + Cooldown).
  - `AlertDispatcher.java` (rendert Template, ruft Mail-Sender).
  - `AlertRuleService.java` (CRUD).
  - `AlertEventListeners.java` (haengt an `AssessmentApprovedEvent`,
    `FindingsCreatedEvent`).
  - `AlertEscalationJob.java` (5-min-Cron).
  - `AlertBannerService.java` (T2-Banner-Status).
  - `MailSenderPort.java` (Schnittstelle, damit Tests ohne SMTP).
- `cvm-integration/alert/SpringMailSenderAdapter.java` implementiert
  den Port via `JavaMailSender`.
- `cvm-api/alert/AlertsController.java` + DTOs + Slice-Test-Config.
- Templates unter `cvm-application/src/main/resources/cvm/alerts/`:
  `alert-kritisch-unbewertet.html`, `alert-shutdown-empfehlung.html`,
  `alert-kev-hit.html`.
- Flyway `V0011__alerts.sql`.

### Frontend
- `cvm-frontend/src/app/core/alerts/alert-banner.service.ts` (Polling).
- `cvm-frontend/src/app/shell/alert-banner.component.ts` (rotes Banner).
- Shell rendert die Banner-Komponente oben.

## Tests

- `AlertEvaluatorTest`: Schwelle, Cooldown, Mehrfach-Trigger. Clock
  fix gesetzt, Repos im Speicher (Mockito).
- `AlertDispatcherTest`: Render-Pfad mit Spring's `TemplateEngine`
  (programmatisch konfiguriert), MailSenderPort als Mock.
- `AlertEscalationJobTest`: alterndes Assessment loest T1/T2.
- `AlertsControllerWebTest`: Slice-Test fuer REST.
- `AlertBannerServiceTest`: T2-Status.
- ArchUnit/Spring-Bean-Tests werden unveraendert grun bleiben.

## Nicht im Scope

- AlertRule-YAML-Bootstrap (Iteration 21).
- Teams/Slack/Jira-Tickets.
- KI-personalisierte Texte (Iteration 14).
- Vier-Augen-Pending-spezifischer Alert (folgt mit Iteration 13/14).

## Definition of Done (Mapping)

- [x] Plan geschrieben.
- [ ] Migration V0011 + Domain + Persistenz.
- [ ] Evaluator + Cooldown.
- [ ] Dispatcher + Templates.
- [ ] REST + Slice-Test.
- [ ] Frontend-Banner.
- [ ] mvnw test + ng build/lint gruen.
- [ ] Reports.
- [ ] Commit.
