# Iteration 09 – SMTP-Alerts (Trigger, Templates, Cooldown)

**Jira**: CVM-18
**Abhängigkeit**: 06
**Ziel**: Mail-Alarme bei kritischen, unbewerteten CVEs gemäß Schwellen aus
Konzept v0.2 Abschnitt 9.

---

## Kontext
Konzept v0.2 Abschnitt 9. Alerts sind deterministisch ausgelöst (nicht
KI-gesteuert). Textformulierung wird später (Iteration 14) KI-unterstützt;
hier statische Thymeleaf-Templates.

## Scope IN
1. `AlertRule`-Entity + Flyway `V0009__alerts.sql`:
   `alert_rule`, `alert_event`, `alert_dispatch`.
2. Konfigurations-Schema (YAML, ähnlich Profile) mit Feldern aus Konzept 9.2:
   `name`, `when` (Condition-DSL wie Regel-Engine), `cooldownMinutes`,
   `subjectPrefix`, `severity` (`INFO`, `WARNING`, `CRITICAL`),
   `recipients` (Liste oder Gruppenverweis).
3. `AlertEvaluator`: läuft bei jedem `AssessmentProposedEvent`,
   `AssessmentApprovedEvent`, `FindingCreatedEvent` und auf Scheduler-Tick
   (alle 5 min).
4. Cooldown: gleiche Regel + gleicher Trigger-Key (CVE, PV, Env) nicht
   öfter als `cooldownMinutes` feuern. Persistiert in `alert_event`.
5. Dispatch via Spring Mail (Jakarta Mail). Templates in
   `cvm-application/alert/templates/`:
   - `alert-kritisch-unbewertet.html` (HTML + Text-Fallback)
   - `alert-shutdown-empfehlung.html`
   - `alert-kev-hit.html`
6. REST:
   - `GET /api/v1/alerts/rules`
   - `POST /api/v1/alerts/rules` (Admin)
   - `POST /api/v1/alerts/test` (Dry-Run, kein echter Mailversand)
7. Eskalationsstufen (Konzept 9.3):
   - T1-Eskalation (Default 2 h) → Plattform-Lead
   - T2-Eskalation (Default 6 h) → Geschäftsführung + UI-Banner-Flag

## Scope NICHT IN
- Teams/Slack-Webhook (Nice-to-have, später).
- Jira-Auto-Ticket (Nice-to-have, später).
- KI-personalisierte Mail-Texte (Iteration 14).

## Aufgaben
1. SMTP-Konfiguration in `application.yaml` mit Placeholdern; MailHog
   als Testsender (Docker-Compose aus Iteration 00).
2. Test-Mode: `cvm.alerts.mode=dry-run|real`. Default `dry-run` in
   Test-Profile.
3. Audit: jeder versendete Alert erzeugt `AuditTrail`-Eintrag.
4. UI-Banner (leichtgewichtig, in Shell aus Iteration 07 nachrüsten):
   Banner erscheint, wenn mindestens ein T2-Eskalations-Event offen ist.

## Test-Schwerpunkte
- `AlertEvaluatorTest`: Schwellen, Cooldown, mehrfaches Feuern.
- Integrationstest mit GreenMail: Mail wird an MailHog zugestellt, Inhalt
  geprüft.
- Cooldown-Test: zweiter Trigger innerhalb Cooldown wird unterdrückt,
  aber als „gedrosselt" protokolliert.
- Test-Endpunkt `POST /alerts/test` funktioniert ohne echten Versand.
- `@DisplayName`: `@DisplayName("Alert: KEV-Hit loest Mail aus, Cooldown unterdrueckt Wiederholung innerhalb 60 Minuten")`

## Definition of Done
- [ ] Alerts konfigurierbar, dispatchbar, auditiert.
- [ ] Cooldown und Eskalationsstufen getestet.
- [ ] UI-Banner bei T2.
- [ ] Coverage `cvm-application/alert` ≥ 85 %.
- [ ] Fortschrittsbericht.
- [ ] Commit: `feat(alert): SMTP-Alerts mit Schwellen, Cooldown und Eskalation\n\nCVM-18`

## TDD-Hinweis
Cooldown-Logik ist fehleranfällig (Zeit!). Tests mit `Clock`-Abstraktion,
`Clock.fixed(...)` pro Testfall. Keine echten `Thread.sleep`-Konstrukte.

## Abschlussbericht
Standard.
