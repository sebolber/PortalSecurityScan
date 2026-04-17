# Iteration 18 – KI-Anomalie-Check + Profil-Assistent

**Jira**: CVM-43
**Abhängigkeit**: 13, 04
**Ziel**: Zwei KI-Funktionen, die den Alltag entlasten, ohne in den
Bewertungsfluss zu greifen: Anomalie-Erkennung auf Assessments und dialogischer
Editor für Kontextprofile.

---

## Kontext
Konzept v0.2 Abschnitt 6.5 (Anomalie-Check als Ergänzung zur Eskalation) und
Abschnitt 6.4 (Profil-Assistent). Beide Funktionen sind Hinweisgeber, keine
Entscheider.

## Scope IN

### Teil A – KI-Anomalie-Check
1. `AnomalyDetectionService` in `cvm-ai-services/anomaly/`.
2. Stündlicher Job `AnomalyCheckJob`:
   - Scannt Assessments der letzten 24 h.
   - Prüft Muster (tabellengetrieben, KI erst als Zweitprüfung):
     - CVE als `NOT_APPLICABLE` trotz `kevListed=true` und `epssScore > 0.7`
     - mehr als N `ACCEPT_RISK`-Waiver desselben Bewerters in 24 h
     - Begründung sehr ähnlich (Cosine-Similarity > 0.9) zu bereits
       abgelehnten Vorschlägen
     - Downgrade um ≥ 2 Severity-Stufen ohne vorhandene Regel-Unterstützung
3. LLM-Zweitprüfung nur, wenn Pattern-Heuristik anschlägt:
   `anomaly-review.v1` fragt gezielt „ist die Einstufung angesichts von
   KEV-Status und Profilfakten plausibel?"; Ausgabe:
   `{severity: INFO|WARNING|CRITICAL, reason, pointers[]}`.
4. Ergebnisse landen in `anomaly_event`-Tabelle und triggern den
   bestehenden Alert-Flow aus Iteration 09 (Trigger `ki-anomalie`).
5. **Keine automatische Sperre** eines Assessments, keine Status-Änderung.
   Nur Hinweis an den Lead.
6. UI: Neues Widget „Hinweise der letzten 24 h" im Dashboard, klickbar
   zur Anomalie-Liste.

### Teil B – Profil-Assistent (dialogischer Editor)
1. `ProfileAssistantService` in `cvm-ai-services/profileassistant/`.
2. Chat-artige Interaktion:
   - Startpunkt: aktuelle Profilversion aus Iteration 04 laden.
   - LLM-Call `profile-wizard.v1` stellt gezielt Fragen zu **genau den
     Feldern**, die für die letzten 100 Assessments relevant waren
     (aus `rationaleSourceFields`).
   - Antworten des Benutzers werden als Profil-Diff vorgemerkt.
3. Kein direkter Schreibzugriff auf das Profil – am Ende des Dialogs
   entsteht ein `ContextProfileDraft`, der durch den regulären
   Freigabe-Workflow aus Iteration 04 läuft (Vier-Augen für Aktivierung).
4. REST:
   - `POST /api/v1/environments/{id}/profile/assist` startet Session.
   - `POST /api/v1/environments/{id}/profile/assist/{sessionId}/reply`
     liefert Antwort + Folgefrage.
   - `POST /api/v1/environments/{id}/profile/assist/{sessionId}/finalize`
     erzeugt Draft.
5. UI: Wizard-Dialog im Profil-Bereich der Angular-App (Tab „Assistent"
   neben „YAML"-Editor).

## Scope NICHT IN
- Retrospektive Anomalie-Analyse älter als 30 Tage.
- Mehrbenutzer-Dialoge am selben Profil gleichzeitig.

## Aufgaben
1. Anomalie-Pattern deterministisch zuerst, LLM nur als zweite Stufe –
   hält Kosten klein und vermeidet false-positive-LLM-Calls.
2. Profil-Assistent: Fragen priorisieren nach
   (a) Feld aktuell unbesetzt, (b) Feld oft in `rationaleSourceFields`.
3. Session-State nur im Backend halten (Redis-artig, hier aber
   einfach JPA-Tabelle `profile_assist_session` mit TTL 24 h).
4. Alle LLM-Calls: Audit via Gateway.

## Test-Schwerpunkte
- `AnomalyDetectionServiceTest`: alle vier Muster, true/false-Fälle.
- Integrationstest: Anomalie → Alert-Event (Iteration 09).
- `ProfileAssistantServiceTest`: Dialog-Fluss, Draft-Erzeugung,
  keine Direkt-Schreibung am aktiven Profil.
- Session-Timeout-Test.
- `@DisplayName`: `@DisplayName("Anomalie-Check: NOT_APPLICABLE trotz KEV und hohem EPSS loest WARNING aus")`

## Definition of Done
- [ ] Anomalie-Check läuft stündlich, ohne Falschalarm-Flut.
- [ ] Profil-Assistent funktional; Draft landet im regulären Freigabe-Flow.
- [ ] Beide Funktionen respektieren Feature-Flag.
- [ ] Coverage `cvm-ai-services/anomaly` und `.../profileassistant` ≥ 85 %.
- [ ] Fortschrittsbericht.
- [ ] Commit: `feat(ai): Anomalie-Check und Profil-Assistent ergaenzt\n\nCVM-43`

## TDD-Hinweis
Der Anomalie-Check darf Bewertungen **nie** automatisch ändern. Schreibe
einen harten Invarianten-Test, der das beweist, und halte ihn fest.
**Ändere NICHT die Tests** bei Rot.

## Abschlussbericht
Standard.
