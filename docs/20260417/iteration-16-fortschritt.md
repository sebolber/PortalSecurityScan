# Iteration 16 - Fix-Verifikation - Fortschritt

**Jira**: CVM-41
**Datum**: 2026-04-18
**Branch**: `claude/iteration-10-pdf-report-C9sA4`

## Zusammenfassung

Fix-Verifikation ist live. Pro `MitigationPlan` kann via
`POST /api/v1/mitigations/{id}/verify-fix` ein LLM-Call
getriggert werden, der die Commit-Range zwischen aktueller und
Ziel-Version gegen die CVE-ID und gegen die vulnerable
Funktion/Klasse prueft. Ergebnis ist ein Grade **A/B/C** plus
Evidenz-Commits. Der Service **ueberschreibt** eine zu optimistische
LLM-Antwort, wenn die Heuristik die CVE-ID in keiner Quelle findet
(A -> B/C) bzw. hebt auf A, wenn die Heuristik die CVE-ID
eindeutig belegt.

Der `OpenFixWatchdog` laeuft taeglich, prueft Findings ohne
`fixedInVersion`, und triggert bei neuem Upstream-Release einen
`AiSuggestion(UPGRADE_RECOMMENDED)` und setzt das zugehoerige
Assessment auf `NEEDS_REVIEW`.

## Umgesetzt

### Domain
- `FixVerificationGrade` (A/B/C/UNKNOWN), `FixEvidenceType`.

### Persistenz
- Flyway `V0017__mitigation_verification.sql`:
  - `mitigation_plan` um `verification_grade`,
    `verification_evidence_type`, `verified_at` erweitert
    (Check-Constraints fuer Enum-Stabilitaet).
  - `ai_source_ref.kind` um `GIT_COMMIT` erweitert, damit
    Evidenz-Commits typisiert referenziert werden koennen.
- `MitigationPlan`-Entity um die drei Felder erweitert.

### Integration (`cvm-integration/git`)
- `GitProviderPort` mit `releaseNotes()` + `compare()`.
- `FakeGitProvider` (Default, Test-stubbar).
- `GitHubApiProvider` via Spring `RestClient` gegen GitHub
  REST API (`/repos/:owner/:repo/releases/tags/:tag`,
  `/repos/:owner/:repo/compare/:from...:to`). Aktiv ueber
  `cvm.ai.fix-verification.github.enabled=true`. WireMock-Tests.

### LLM-Gateway / Prompts
- `fix-verification.st` mit JSON-Schema-Vorgabe und
  Grade-Regeln im System-Prompt.

### AI-Services (`cvm-ai-services/fixverify`)
- `SuspiciousCommitHeuristic`: Marker-basiert (CVE-ID,
  GHSA-ID, Security-Keywords, Datei-/Symbol-Match).
- `FixVerificationConfig`: Flag, Full-Text-Commit-Cap (50),
  Cache-TTL (24h).
- `FixVerificationResult` + `CommitEvidence` Read-Models.
- `FixVerificationService`:
  - Provider-Daten ueber in-memory Cache (TTL 24h).
  - Heuristik markiert verdaechtige Commits; Messages-Only-
    Modus ab 50 Commits.
  - LLM-Call ueber `AiCallAuditService` (use-case
    `FIX_VERIFICATION`).
  - **Grade-Override**: Service setzt finalen Grade auf Basis
    der eigenen CVE-ID-Heuristik (A nur mit Beleg, Upgrade auf
    A, wenn CVE-ID klar im Commit/Notes steht).
  - Persistenz: `MitigationPlan.verificationGrade` +
    `AiSuggestion` mit `ai_source_ref(GIT_COMMIT)`.
- `OpenFixWatchdog`:
  - `@Scheduled` taeglich, nur wenn Flag + scheduler-enabled.
  - `runOnce(Map repoUrls)` als Test-/Admin-Einstieg.
  - Dedup-Check: kein doppelter `UPGRADE_RECOMMENDED` fuer
    selbes Finding+Tag.
  - Setzt aktive Assessments auf `NEEDS_REVIEW` via bestehender
    `markiereAlsReview`-Query.

### API
- `FixVerificationController`:
  - `POST /api/v1/mitigations/{id}/verify-fix`
  - `GET  /api/v1/mitigations/{id}/verification`
- `FixVerificationExceptionHandler` (404/400).
- Slice-Konfig + WebTest.

## Pragmatische Entscheidungen

- **Service-Override der LLM-Grade-Antwort**: Wird vom
  Test `downgradeAaufBwennKeineCveId` hart geprueft. Das
  Modell darf nicht per Prompt-Tuning plotzlich flaechendeckend
  Grade-A-Bewertungen erzeugen.
- **In-Memory-Cache**: `ConcurrentHashMap` mit TTL-Check.
  Reicht fuer Iteration 16; Redis/Cacheable folgt bei
  Produktivmengen (offene-punkte.md).
- **Watchdog mit `repoUrls`-Map aus Admin-Input**: Ein
  permanentes Mapping `cveKey -> repoUrl` lebt noch nicht im
  DB-Modell. Fuer den automatischen Tageslauf braucht das
  Mapping spaeter einen Konfigurationsort. Heute wird der
  Watchdog entweder per Test oder per zukuenftigem Admin-
  Endpunkt mit explicit map aufgerufen; sein `@Scheduled`
  laeuft ohne Map, d.h. er meldet nur `geprueft`-Counts.
- **Audit-Id-Zuordnung**: wie in Iteration 13/16 beschrieben,
  via "letzter OK-Audit des Use-Cases". Sauberer ist ein
  Rueckgabewert aus `AiCallAuditService.execute(...)`; offener
  Punkt.
- **Watchdog-Audit**: Synthetischer `ai_call_audit`-Eintrag
  mit `modelId=watchdog`, damit der `AiSuggestion`-FK erfuellt
  ist, obwohl kein LLM-Call stattgefunden hat. Alternative
  waere `ai_call_audit_id` nullable - die Semantik-Luecke
  dokumentiert `offene-punkte.md`.
- **Kein Auto-Upgrade**: Konzept 6.8 ausdruecklich - der
  Watchdog empfiehlt, `AssessmentStatus` geht auf
  `NEEDS_REVIEW`, nie auf `APPROVED`.

## Sicherheits-Invarianten (durch Tests gehaertet)

- `SuspiciousCommitHeuristicTest` + `FixVerificationServiceTest`
  pruefen die Grade-Regeln explizit (A nur mit CVE-ID-Beleg).
- `OpenFixWatchdogTest.dedup`: kein doppelter Vorschlag fuer
  dieselbe `(Finding, Tag)`-Kombination.
- `OpenFixWatchdogTest.deaktiviert`: `scheduledRun()` ist No-Op
  bei ausgeschaltetem Feature-Flag.
- `FixVerificationControllerWebTest`: 400 bei fehlenden
  Pflichtfeldern; 404 bei unbekannter Mitigation.

## Tests (neu)

### cvm-integration (+5 = jetzt 13)
- `GitHubApiProviderTest` (5): Happy release-notes, Compare,
  404 auf beiden, Slug-Extraktion.

### cvm-ai-services (+17 = jetzt 65)
- `SuspiciousCommitHeuristicTest` (6).
- `FixVerificationServiceTest` (6).
- `OpenFixWatchdogTest` (5).

### cvm-api (+4 = jetzt ~45)
- `FixVerificationControllerWebTest` (4): POST happy, GET,
  404, 400.

### Gesamt-Testlauf
```
./mvnw -T 1C test  BUILD SUCCESS  (~97 s)
```

**Gesamt gruen: 322, Skipped 11 (Docker), Rot 0.**
Iteration 16 bringt **~26 neue Tests** ins System.

## Nicht im Scope

- GitLab-Provider (Port ist abstrakt, konkret liefern wir
  GitHub + Fake).
- Breaking-Change-Analyse der Zielversion.
- UI-Badge fuer den Grade (Backend-Vertrag definiert).
- Produktiver Cache-Backing-Store (Redis, Caffeine).

## Offene Punkte

- **Watchdog-Mapping**: `cveKey -> repoUrl` braucht einen
  persistenten Ort (Tabelle `cve_source_repo` oder erweiterte
  Produkt-Version-Metadaten), damit der automatische Tageslauf
  produktiv Nutzen hat.
- **Audit-Id-Rueckgabe**: `AiCallAuditService.execute(...)`
  sollte `AuditedLlmResponse(response, auditId)` liefern.
- **Watchdog-Audit-Lightweight**: synthetischer `ai_call_audit`-
  Eintrag. Eine eigene `ai_subprocess_audit`-Tabelle oder
  `ai_call_audit_id` nullable waere sauberer.
- **UI-Badge fuer Grade** in der Queue-Detail-Ansicht.
- **Cache-Eviction** sauber absichern (aktuell kein Cleanup -
  Speicher waechst bis JVM-Restart).

## Ausblick Iteration 17
KI-Regel-Extraktion: aus wiederholten manuellen Bewertungen
lernen und regelbasierte Vorschlaege erzeugen, die der Admin
als `Rule` freigibt.

## Dateien (wesentlich, neu)

### Domain/Persistenz
- `cvm-domain/.../enums/FixVerificationGrade.java`
- `cvm-domain/.../enums/FixEvidenceType.java`
- `cvm-persistence/src/main/resources/db/migration/V0017__mitigation_verification.sql`
- `cvm-persistence/.../mitigation/MitigationPlan.java` (Felder)

### Integration
- `cvm-integration/.../git/GitProviderPort.java`
- `.../git/FakeGitProvider.java`
- `.../git/GitHubApiProvider.java` + Test

### LLM-Gateway
- `cvm-llm-gateway/src/main/resources/cvm/llm/prompts/fix-verification.st`

### AI-Services
- `cvm-ai-services/.../fixverify/*` (Config, Heuristic, Result,
  Service, Watchdog, 3 Test-Klassen)

### API
- `cvm-api/.../fixverify/*` (Controller, ExceptionHandler,
  WebTest)

### Docs
- `docs/20260417/iteration-16-plan.md`
- `docs/20260417/iteration-16-fortschritt.md`
- `docs/20260417/iteration-16-test-summary.md`
