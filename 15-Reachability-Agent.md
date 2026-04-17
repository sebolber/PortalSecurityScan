# Iteration 15 – Reachability-Agent (Claude Code gegen Git)

**Jira**: CVM-40
**Abhängigkeit**: 13
**Ziel**: Für hochwertige Vorschläge den eigenen Code in den Bewertungsprozess
ziehen: Wird die vulnerable Funktion überhaupt erreicht?

---

## Kontext
Konzept v0.2 Abschnitt 6.7. Besondere Zutat: der Agent läuft nicht als HTTP-
Call, sondern als **Claude-Code-CLI-Subprozess** mit Zugriff auf einen
Git-Checkout der Produktversion. Dadurch entfällt der Umweg über das LLM-
Gateway für das Lesen des Codes; die Audit-Pflicht bleibt.

## Scope IN
1. Neuer Use-Case `REACHABILITY` im LLM-Gateway mit Spezialbehandlung:
   `ai_call_audit` protokolliert Subprozess-Aufruf statt HTTP-Request
   (Befehlszeile, cwd, Exit-Code, stdout-Länge, Kosten geschätzt).
2. `GitCheckoutService`:
   - Klont/fetched Repo aus `ProductVersion.repoUrl + branch + commitSha`
     in temporäres Arbeitsverzeichnis (SSH-Keys aus Vault).
   - Cache-basiert, Working-Copies pro Commit wiederverwendet.
3. `ReachabilityAgent`:
   - Eingaben: `Finding` (mit CVE, Komponente, PURL), `ProductVersion`,
     `CveAdvisoryDetails` (verletzliche Klasse/Methode).
   - Schreibt einen Prompt-File und startet Claude Code via Subprozess:
     ```
     claude code --prompt-file /tmp/reach-<id>.md --output json --read-only
     ```
   - Empfängt strukturierte JSON-Antwort (Call-Sites, Trust-Level,
     Befund-Text, Empfehlung).
4. Prompt-Template `reachability.v1` mit:
   - Aufgabenbeschreibung („Finde alle Aufrufe von `X.y(…)` in diesem Commit.
     Klassifiziere je Aufrufer: Nutzereingabe trust/untrust, Abschirmung
     durch vorangehende Validierung, statische Konfiguration.").
   - Verbot aller schreibenden Aktionen (`--read-only`).
5. Ergebnis wird als `ai_source_ref` an den zugehörigen `ai_suggestion`
   geheftet (bei neuen Findings) oder an bestehende Assessments ergänzt
   (`POST /api/v1/findings/{id}/reachability` → Ergebnis landet in
   vorhandenem Vorschlag).
6. Sandboxing: Subprozess läuft mit unprivilegiertem User, begrenzte
   Laufzeit (5 min default, konfigurierbar), keine Netzwerkverbindung aus
   dem Arbeitsverzeichnis heraus (Network-Namespace oder Seccomp).
7. Feature-Flag `cvm.ai.reachability.enabled=false` default.

## Scope NICHT IN
- UI-Trigger (Button in Queue-Detail ergänzen, aber Detail-View für
  Reachability-Report kommt später).
- Sprachspezifische Analyse für Ruby/Go (MVP: Java, TypeScript, Python).

## Aufgaben
1. Git-Operationen via JGit (nicht Shell).
2. Subprozess-Runner `ClaudeCodeSubprocess` mit strikten
   Ressourcenlimits (`CompletableFuture` + `Process.onExit()`,
   Timeout via `destroyForcibly`).
3. Output-Parser akzeptiert nur strukturiertes JSON gegen Schema;
   Freitext wird verworfen und als Fehler protokolliert.
4. Retry-Strategie: ein Retry bei Timeout, danach Abbruch mit
   Vermerk im Vorschlag („Reachability-Analyse nicht verfügbar").
5. UI: in Queue-Detail ein Button „Reachability prüfen" (sichtbar
   für `CVE_ASSESSOR` aufwärts, rate-limited auf Benutzer).

## Test-Schwerpunkte
- Unit-Test mit `FakeClaudeCodeSubprocess`, der definierte JSON-
  Antworten liefert.
- Integrationstest mit einem Mini-Repo in Testressourcen (Java-Projekt
  mit bekannter SnakeYAML-Nutzung), Agent findet die Call-Sites.
- Security-Test: Subprozess-Timeout greift.
- Audit-Test: `ai_call_audit` enthält Subprozess-Metadaten.
- `@DisplayName`: `@DisplayName("Reachability: Analyse findet drei Call-Sites und klassifiziert alle als statische Konfiguration")`

## Definition of Done
- [ ] Reachability für Java/TS/Python funktional.
- [ ] Sandbox- und Timeout-Verhalten getestet.
- [ ] Ergebnis erscheint als Source-Ref am Vorschlag.
- [ ] Coverage `cvm-ai-services/reachability` ≥ 85 %.
- [ ] Fortschrittsbericht.
- [ ] Commit: `feat(ai): Reachability-Agent via Claude Code gegen Git-Checkout\n\nCVM-40`

## TDD-Hinweis
Der Subprozess-Runner ist die größte Fehlerquelle. Teste Timeout,
Resource-Cleanup, Crash-Recovery zuerst. **Ändere NICHT die Tests**
bei Rot.

## Abschlussbericht
Standard, plus Beispielausgabe an echtem PortalCore-Commit (falls verfügbar,
sonst synthetisch).
