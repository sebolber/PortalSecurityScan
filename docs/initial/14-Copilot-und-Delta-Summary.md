# Iteration 14 – Inline-Copilot + KI-Delta-Summary

**Jira**: CVM-33
**Abhängigkeit**: 13
**Ziel**: Dialogische KI-Hilfe im Queue-Detail und automatische Management-
Zusammenfassung nach jedem Scan.

---

## Kontext
Konzept v0.2 Abschnitt 6.2 (Copilot im Workflow) und 6.3 (Delta-Summary bei
Re-Scan), Abschnitt 8 (UI).

## Scope IN
### Teil A – Copilot (pro Assessment)
1. `CopilotService` in `cvm-ai-services/copilot/`.
2. Anwendungsfälle (fixe Prompt-Templates):
   - **Begründung verfeinern** (`refine-rationale.v1`)
   - **Ähnliche Bewertungen** (`similar-assessments.v1`, nutzt RAG)
   - **Upstream-Commit lesen** (`explain-commit.v1`, bekommt Commit-Text
     als Kontext)
   - **Auditformulierung** (`audit-tone.v1`)
3. REST:
   - `POST /api/v1/assessments/{id}/copilot` Body
     `{useCase, userInstruction, attachments}` → streamt Antwort
     (SSE oder NDJSON) mit Source-Refs.
4. Sessions sind stateless: jede Anfrage trägt die relevanten IDs;
   Historie wird nicht modelliert (keine langen Chats, fokussierte Aktionen).
5. Rate-Limit pro Benutzer (Bucket4j) und Kostencap pro Umgebung.
6. UI in Queue-Detail (Iteration 08 erweitert):
   - Aktionsleiste mit Buttons für die vier Copilot-Use-Cases.
   - Ergebnis erscheint als Vorschlag unter dem editierbaren Begründungs-
     feld mit Button „Übernehmen".

### Teil B – KI-Delta-Summary
1. `ScanDeltaSummaryService`:
   - Eingabe: aktueller Scan + letzter freigegebener Scan derselben
     (ProduktVersion, Umgebung).
   - Berechnet strukturiert: neue CVEs, entfallene CVEs, Severity-Shifts,
     KEV-Änderungen.
   - LLM-Call `delta-summary.v1` mit strukturierter Zusammenfassung als
     Kontext (nicht rohes SBOM).
   - Rückgabe: 2 Varianten (Kurz für Slack-Snippet, Lang für
     Lenkungsausschuss-Status).
2. Generierung automatisch am Ende der Scan-Ingestion (nach Cascade).
3. REST: `GET /api/v1/scans/{id}/delta-summary?audience=short|long`.
4. UI: Summary-Card im Dashboard, ausklappbar für Langfassung.

## Scope NICHT IN
- Reachability-Aufrufe aus Copilot (Iteration 15).
- Board-Report-PDF (Iteration 19).

## Aufgaben
1. Streaming-Antworten via Server-Sent-Events; Fallback auf
   `application/x-ndjson`.
2. Angular-Komponente für Streaming-UI (Tokens erscheinen live).
3. Alle Copilot-Antworten landen als **Vorschlag**, nie als direkter
   Textersatz. Der Bewerter übernimmt bewusst.
4. Delta-Summary-Generator bricht ab, wenn keine Vorlage (erstmaliger
   Scan) – liefert dann statischen Initial-Text.

## Test-Schwerpunkte
- `CopilotServiceTest` je Use-Case.
- Streaming-Controller-Test mit WebTestClient, Prüfung der SSE-Events.
- `ScanDeltaSummaryServiceTest`: Diff-Berechnung deterministisch,
  LLM-Aufruf geschieht nur, wenn Diff ≥ Mindestschwelle (`cvm.ai.summary.minDelta=1`).
- Integrationstest: Scan 1 → Scan 2 → Summary enthält die 3
  neuen CVEs und das 1 geschlossene.
- `@DisplayName`: `@DisplayName("Copilot: Begruendungsverfeinerung uebernimmt Severity nicht")`

## Definition of Done
- [ ] Copilot funktional mit vier Use-Cases.
- [ ] Delta-Summary erscheint im Dashboard und als API-Endpunkt.
- [ ] Streaming funktioniert.
- [ ] Copilot-Outputs ändern Severity nicht automatisch (Test prüft das
      explizit).
- [ ] Coverage `cvm-ai-services/copilot` ≥ 85 %.
- [ ] Fortschrittsbericht.
- [ ] Commit: `feat(ai): Copilot und Delta-Summary ergaenzt\n\nCVM-33`

## TDD-Hinweis
Der Test, dass Copilot niemals Severity direkt setzt, ist Sicherheitskritisch.
**Ändere NICHT die Tests** bei Rot – das ist genau der Punkt, an dem das
Prinzip „Mensch entscheidet" durchgesetzt wird.

## Abschlussbericht
Standard, plus kurze Demo-Aufzeichnung der Copilot-Interaktion.
