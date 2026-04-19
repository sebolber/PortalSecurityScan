# Iteration 94 - Fortschritt

**Jira**: CVM-334 (U-08 Teil 2 Dashboard-Handlungszentrale)

## Umgesetzt

- `DashboardComponent` bezieht jetzt auch `ReportsService` und
  `AlertBannerService`.
- Neue Signale `letzteReports`, `letzteReportsLaedt`,
  `letzteReportsFehler` speichern die Top-5-Reports und ihren
  Ladezustand.
- `ngOnInit()` laedt die Report-Liste rollen-konditional
  (`darfReports` = ADMIN/REPORTER/VIEWER).
- Template: zwei neue Cards unterhalb der KPIs:
  - **T2-Eskalation**: Ampel-Badge + CTA "Zur Queue" oder LOW-
    Zustand.
  - **Zuletzt erzeugte Reports**: UL mit Severity + Titel + Datum,
    CTA "Alle Reports ansehen".
- Bestehende Spec-Struktur erweitert: neuer `describe`-Block mit
  5 Cases, zusaetzliche Fake-Provider fuer AlertBanner und
  Reports (reine In-Memory-Implementierungen, kein HttpClient).

## Nicht umgesetzt

- "Mein Tag"-Card (persoenliche Queue-Items). Braucht einen
  Backend-Endpoint wie `GET /queue/mine` oder
  `?assignee=<user>`.

## Technische Hinweise

- FakeAlertBanner nutzt `signal(...)` statt Mocking der
  `.status`-Property, damit die OnPush-Dashboards automatisch
  neue Werte aufnehmen.
- FakeReports kann via `list.and.callFake(() => { throw ... })`
  Fehlerpfade testen, obwohl `list()` normalerweise ein
  Observable zurueckgibt; `firstValueFrom` fangt den Throw.
