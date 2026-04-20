# Iteration 08 – Bewertungs-Queue-UI

**Jira**: CVM-17
**Abhängigkeit**: 06, 07
**Ziel**: Der Bewerter arbeitet die Queue komfortabel ab – mit Shortcut-
Bedienung, sichtbarer Vorschlagsquelle und klarer Vier-Augen-Logik.

---

## Kontext
Konzept v0.2 Abschnitt 8 (UI), Punkt 3 (Bewertungs-Queue) und 4 (CVE-Detail).
Die Queue ist der Kern-Arbeitsplatz.

## Scope IN
1. Route `/queue` mit Filter-Sidebar:
   - Produkt × Produktversion × Umgebung
   - Status (Default: `PROPOSED`, `NEEDS_REVIEW`)
   - Severity-Filter
   - Vorschlagsquelle (`REUSE`, `RULE`, `AI`, `MANUAL`)
2. Tabellenansicht mit Spalten:
   CVE | Komponente | Original-Severity | Vorschlags-Severity |
   Quelle (Badge) | Alter | Aktionen
3. Detail-Panel (Slide-In rechts):
   - CVE-Beschreibung, CWE, KEV-Flag, EPSS-Score
   - alle Fundstellen (Component-Occurrences mit FilePath)
   - vorgeschlagene Severity, vorgeschlagene Begründung (editierbar)
   - vorgeschlagener Fix-Plan (Strategy, Ziel-Release, Datum – editierbar)
   - Buttons: *Approve*, *Approve with edits*, *Override*, *Reject*
4. Tastatur-Shortcuts (dokumentiert in Help-Overlay):
   - `j`/`k`: nächster/vorheriger Eintrag
   - `a`: Approve
   - `o`: Override (öffnet Severity-Dropdown)
   - `r`: Reject (öffnet Kommentar-Pflichtfeld)
   - `?`: Shortcut-Hilfe
5. Vier-Augen-Indikator:
   - Bei Downgrades: Badge „Zweitfreigabe erforderlich", Approve-Button
     zeigt „Zur Zweitfreigabe einreichen" statt „Freigeben".
   - Separate Queue-Ansicht „Zweitfreigaben" für `CVE_APPROVER`.
6. Optimistic UI: sofortige Update-Anzeige, Rollback bei API-Fehler.
7. Batch-Aktionen (Multi-Select): Approve-with-edits auf Gruppe,
   sinnvoll für semantische Cluster (Clustering erst ab Iteration 13
   automatisch, UI hier vorbereitet).

## Scope NICHT IN
- Copilot (Iteration 14).
- Clustering-Erzeugung (Iteration 13; Gruppierung kommt erst mit KI).
- CVE-Detail-Seite (eigene Iteration später).

## Aufgaben
1. Feature-Modul `features/queue/` mit Standalone-Components.
2. State-Management: Lightweight Signal-Store (`@angular/core` Signals,
   kein NgRx für diese Iteration).
3. `QueueService.reload()`-Polling alle 60 s, manuell triggerbar per „Refresh".
4. Error-Handling: Bei Konflikt (parallele Bearbeitung) Banner mit
   Konfliktbeschreibung, Option „überschreiben".
5. A11y: Tastaturbedienbar, ARIA-Labels, Fokus-Management.

## Test-Schwerpunkte
- Jasmine: `QueueStoreTest`, `ShortcutDirectiveTest`.
- Playwright-E2E: Happy-Path (Approve), Override, Reject, Vier-Augen-
  Zweitfreigabe, Konfliktszenario.
- Screenshot-Tests für die Severity-Badges.

## Definition of Done
- [ ] Queue sichtbar mit echten Daten aus Iteration 06.
- [ ] Alle Aktionen funktional gegen Backend.
- [ ] Vier-Augen-Flow UI-seitig umgesetzt.
- [ ] Shortcuts funktionieren, Help-Overlay vorhanden.
- [ ] Coverage Frontend-Queue ≥ 80 %.
- [ ] Fortschrittsbericht.
- [ ] Commit: `feat(queue-ui): Bewertungs-Queue mit Shortcuts und Vier-Augen-Flow\n\nCVM-17`

## TDD-Hinweis
E2E-Test zuerst für Happy-Path, danach Unit-Tests für State-Store und
Shortcut-Dispatcher. **Ändere NICHT die Tests** bei Rot.

## Abschlussbericht
Standard, plus kurzes GIF/Video der Queue-Bedienung (optional).
