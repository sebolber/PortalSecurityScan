# Iteration 16 – Fix-Verifikation (Upstream-Release-Notes + Commit-Analyse)

**Jira**: CVM-41
**Abhängigkeit**: 13
**Ziel**: Trivy-Empfehlungen `Upgrade to X.Y.Z` werden belastbar verifiziert: adressiert
die Ziel-Version die CVE wirklich, oder steht im Changelog nur generisches
„security improvements"?

---

## Kontext
Konzept v0.2 Abschnitt 6.8. Zusatznutzen: Wenn Trivy „kein Fix verfügbar" meldet,
prüft das System täglich, ob sich das geändert hat.

## Scope IN
1. `FixVerificationService` in `cvm-ai-services/fixverify/`.
2. Eingabe: `MitigationPlan` mit `UPGRADE_DEPENDENCY`-Strategie und Ziel-Version,
   oder ein Finding mit Trivy-Empfehlung.
3. Quellen-Kollektor:
   - Upstream-Release-Notes (GitHub/GitLab Release-API, Changelog-Files
     im Repo, Maven-Central POMs mit `scm.url`).
   - Commit-Range zwischen aktueller und Ziel-Version (JGit oder
     Provider-API).
4. LLM-Call `fix-verification.v1`:
   - Eingabe: CVE-Beschreibung, betroffene Funktion/Klasse,
     Release-Notes-Text, Commit-Liste (mit Commit-Messages und
     optional Diff-Summary für verdächtige Commits).
   - Ausgabe-Schema:
     ```json
     {
       "quality": "A|B|C",
       "evidenceType": "EXPLICIT_CVE_MENTION|FIX_COMMIT_MATCH|INFERRED|NONE",
       "adressedBy": [{"commit": "...", "message": "...", "url": "..."}],
       "confidence": 0.0,
       "caveats": ["..."]
     }
     ```
5. Quality-Grade-Semantik:
   - **A**: Release-Notes oder Commit nennen die CVE-ID explizit.
   - **B**: Commit-Message adressiert die vulnerable Funktion/Klasse
     eindeutig ohne CVE-ID.
   - **C**: keine eindeutige Evidenz („security improvements", Refactoring,
     vermuteter Zusammenhang).
6. Persistenz als `ai_source_ref` am `MitigationPlan`, plus eigenes Feld
   `verification_grade` und `verified_at`.
7. Scheduled-Job `OpenFixWatchdog` (täglich): für Findings mit Status
   „kein Fix verfügbar" werden Upstream-Quellen neu abgefragt; wird ein
   Fix gefunden, erzeugt das System einen `ai_suggestion` vom Typ
   `UPGRADE_RECOMMENDED` und setzt Assessment auf `NEEDS_REVIEW`.
8. REST:
   - `POST /api/v1/mitigations/{id}/verify-fix` – on-demand, synchron oder
     async.
   - `GET /api/v1/mitigations/{id}/verification` – aktuelle Einstufung.

## Scope NICHT IN
- Automatisches Dependency-Upgrade (Dependabot-artig): **explizit nicht** in
  diesem Projekt. Ausschließlich Bewertung und Empfehlung.
- Breaking-Change-Analyse der Ziel-Version (Nice-to-have, später).

## Aufgaben
1. GitHub/GitLab-API-Adapter in `cvm-integration/git/`.
2. Heuristik für „verdächtige Commits":
   - Commit-Message enthält CVE-ID, GHSA-ID oder relevante Keywords
     (`security`, `vulnerability`, `XXE`, `deserialization`, etc.).
   - Commit berührt Datei mit Namen der vulnerable Klasse/Methode.
3. Caching: Release-Notes und Commit-Ranges pro (Produkt, Version-Paar)
   cachen, 24 h TTL.
4. Kostendeckel: maximal 50 Commits werden ans LLM gegeben; darüber
   wird auf Commit-Messages reduziert, Ganztexte entfallen.
5. UI: Mitigation-Plan-Detail (in Queue aus Iteration 08 erweitert)
   zeigt Grade-Badge A/B/C, Klick öffnet Quellen.

## Test-Schwerpunkte
- `FixVerificationServiceTest` mit Fake-LLM: alle drei Grades erzeugbar
  anhand kuratierter Testinputs.
- WireMock für GitHub-API (Release-Notes, Compare-API).
- `OpenFixWatchdogTest`: vor/nach Verfügbarkeit eines Fixes, Übergang
  zu `UPGRADE_RECOMMENDED`.
- Kosten-Test: bei 500 Commits wird nur Messages-Only-Modus genutzt.
- `@DisplayName`: `@DisplayName("Fix-Verifikation: Grade A nur wenn CVE-ID in Release-Notes oder Commit-Message explizit vorkommt")`

## Definition of Done
- [ ] Grade A/B/C reproduzierbar.
- [ ] Watchdog-Job läuft und erzeugt Vorschläge.
- [ ] Grade-Badge sichtbar im UI.
- [ ] Coverage `cvm-ai-services/fixverify` ≥ 85 %.
- [ ] Fortschrittsbericht.
- [ ] Commit: `feat(ai): Fix-Verifikation mit Upstream-Commit-Analyse und Quality-Grading\n\nCVM-41`

## TDD-Hinweis
Die Grade-Zuordnung ist auditrelevant. Testfälle müssen **bevor** der
LLM-Prompt gebaut wird feststehen. Wenn ein Testfall falsch klassifiziert,
ist der Prompt zu fixen – **nicht der Test**.

## Abschlussbericht
Standard, plus Tabelle mit den im Test erzielten Grades je Testfall.
