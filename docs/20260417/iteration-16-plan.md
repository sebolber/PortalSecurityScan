# Iteration 16 - Fix-Verifikation - Plan

**Jira**: CVM-41
**Branch**: `claude/iteration-10-pdf-report-C9sA4`
**Abhaengigkeit**: Iteration 13 (KI-Vorbewertung), 11 (Audit).

## Architektur

- `cvm-domain`:
  - `FixVerificationGrade` (`A`, `B`, `C`, `UNKNOWN`).
  - `FixEvidenceType` (`EXPLICIT_CVE_MENTION`, `FIX_COMMIT_MATCH`,
    `INFERRED`, `NONE`).
- `cvm-persistence`:
  - `MitigationPlan` bekommt Spalten
    `verification_grade`, `verification_evidence_type`,
    `verified_at`. Flyway `V0017__mitigation_verification.sql`.
- `cvm-integration/git`:
  - `GitProviderPort` (liest Release-Notes + Commit-Range zwischen
    zwei Versionen/Tags).
  - `GitHubApiProvider` ueber Spring `RestClient` gegen GitHub
    REST (Releases + Compare). WireMock-Tests.
  - `FakeGitProvider` fuer CI/Default.
- `cvm-llm-gateway/prompts`:
  - Neu: `fix-verification.st`. System-Prompt enthaelt die
    Grade-Semantik als Regel; JSON-Schema gemaess Konzept.
- `cvm-ai-services/fixverify`:
  - `SuspiciousCommitHeuristic` (regelbasiert: CVE-ID, GHSA-ID,
    Keywords, Datei-Namen mit vulnerable Klasse).
  - `FixVerificationService`:
    - Input: MitigationPlan-Id.
    - Schritt 1: liest Plan + Finding/CVE.
    - Schritt 2: Git-Range via Provider (von aktueller Version
      bis `targetVersion`).
    - Schritt 3: Heuristik -&gt; verdaechtige Commits markieren.
    - Schritt 4: Commit-Liste eindampfen (50-Commit-Cap;
      darueber nur Messages).
    - Schritt 5: LLM-Call ueber `AiCallAuditService` mit
      use-case `FIX_VERIFICATION`.
    - Schritt 6: Grade aus LLM-Antwort mit Heuristik verifizieren
      (Grade A nur wenn CVE-ID tatsaechlich in Release-Notes
      oder Commit-Message vorkommt).
    - Schritt 7: Persistenz: `MitigationPlan.verificationGrade`,
      `verifiedAt`. `ai_suggestion` (use-case `FIX_VERIFICATION`)
      + `ai_source_ref` pro Evidenz-Commit.
  - `OpenFixWatchdog`: Cron (taeglich) fuer Findings mit Status
    "kein Fix". Wenn Provider einen Fix liefert -&gt; neuer
    `AiSuggestion` vom Typ `UPGRADE_RECOMMENDED`, zugehoeriges
    Assessment auf `NEEDS_REVIEW`.
- `cvm-api/fixverify`:
  - `POST /api/v1/mitigations/{id}/verify-fix` (synchron).
  - `GET /api/v1/mitigations/{id}/verification` (aktuelle
    Einstufung + Quellen).

## Sicherheits-Invarianten (durch Tests gehaertet)

1. **Grade A erfordert CVE-ID-Treffer** in Release-Notes oder
   Commit-Message. Der Service ueberschreibt eine LLM-Antwort
   `A` mit `B`/`C`, wenn die Heuristik keinen Treffer findet.
2. **Kein Upgrade ohne Entscheidung**: Watchdog legt nur einen
   `ai_suggestion` an und setzt Assessment auf `NEEDS_REVIEW`;
   niemals `APPROVED`.
3. **Kostendeckel**: Bei mehr als 50 Commits werden nur
   Commit-Messages gesendet (kein Diff).
4. **Caching**: Release-Notes und Commit-Listen pro
   (repo, fromTag, toTag) mit 24h-TTL; Tests pruefen die
   Cache-Hits.
5. **Feature-Flag** `cvm.ai.fix-verification.enabled=false`
   Default (Datenquelle GitHub ist Netz-abhaengig).

## Tests

1. `SuspiciousCommitHeuristicTest`: CVE-ID-Treffer, GHSA-Treffer,
   Keyword-Treffer, Datei-Treffer, negativer Fall.
2. `FixVerificationServiceTest`:
   - Grade A (CVE-ID in Release-Notes).
   - Grade B (Funktion/Datei-Treffer ohne CVE-ID).
   - Grade C (nur "security improvements").
   - Downgrade A -&gt; B wenn Heuristik widerspricht.
   - 500-Commit-Input -&gt; messages-only-Modus.
3. `OpenFixWatchdogTest`: vorher/nachher, erzeugt Suggestion +
   NEEDS_REVIEW.
4. `GitHubApiProviderTest` (WireMock): Happy, 404, 429.
5. `FixVerificationControllerWebTest`: 200 POST/GET, 404.

## Scope NICHT IN

- Auto-Upgrade (Dependabot-artig) - explizit nicht.
- Breaking-Change-Analyse der Zielversion.
- UI-Badge (Backend-Vertrag definiert; UI-Nachzug spaeter).
- GitLab-Provider: Port ist abstrakt, Interface erlaubt Implementierung;
  konkret liefern wir GitHub + Fake.
- Produktiver Cache-Backing-Store (in-memory `ConcurrentHashMap`
  mit 24h-Eviction reicht fuer Iteration 16; Redis folgt spaeter).
