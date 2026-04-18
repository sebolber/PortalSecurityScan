# Iteration 09 – Test-Summary

## Backend (`./mvnw -T 1C test`)

- BUILD SUCCESS.
- 157 Tests gruen, 5 Skipped (Docker-gebundene Slice-Tests aus
  Iteration 06 unveraendert).

### Neue Tests
| Test | Anzahl | Schwerpunkt |
|------|--------|-------------|
| `AlertEvaluatorTest` | 5 | Erstes Feuern, Cooldown < 60min, Cooldown abgelaufen, keine Regeln, Dispatcher-Fehler |
| `AlertDispatcherTest` | 3 | Dry-Run, Real-Modus, SMTP-Fehler im Audit |
| `AlertTemplateRendererTest` | 3 | Render+Escape, unbekanntes Template, Plain-Text-Fallback |
| `AlertEscalationJobTest` | 3 | T2-Eskalation, T1-Eskalation, Severity-Filter |
| `AlertBannerServiceTest` | 2 | T2-aktiv und -inaktiv |
| `AlertsControllerWebTest` | 3 | Liste, POST /test, Banner-Status |

Damit kommen +19 Tests dazu (vorher 148, jetzt 157).

### Architektur und Spring-Bean
- `ModulgrenzenTest` weiterhin gruen.
- `SpringBeanKonstruktorTest` gruen (Konstruktoren mit nur einem
  Public-Konstruktor wie zuvor).

### Coverage
- `cvm-application/alert`: alle public-Methoden (Evaluator,
  Dispatcher, BannerService, EscalationJob, TemplateRenderer)
  durch Specs abgedeckt.
- `AlertRuleService`: Anlegen mit/ohne Recipients und Liste
  durch Controller-Slice-Test indirekt erfasst (Mock).

## Frontend

### Statisch
- `npx ng build` -> gruen, Initial-Bundle 1.92 MB
  (Banner ist Shell-bound, kein eigener Lazy-Chunk).
- `npx ng lint` -> `All files pass linting`.

### Karma-Specs (geschrieben)
- `alert-banner.service.spec.ts` (2 Tests: Refresh ok, Refresh
  Fehler -> null).
- Karma-Lauf bleibt CI-Pflicht (Sandbox ohne Headless-Chrome).

## Persistenz-Integration

V0011-Migration laeuft beim Start (Flyway), aber ein
Persistenz-Integrationstest mit Testcontainers ist Docker-gebunden
und bleibt skipped. Manuelle Pruefung `mvnw verify` mit Docker ist
in CI nachzuholen.

## Echter SMTP-Lauf

Sandbox liefert keinen SMTP. Manueller Smoke gegen MailHog (Port
1025) ist als Aufgabe fuer den naechsten Hands-On-Lauf in
`offene-punkte.md` vermerkt; alle Voraussetzungen
(`SpringMailSenderAdapter`, `cvm.alerts.mode=real`,
`spring.mail.host=localhost`) sind im Code da.
