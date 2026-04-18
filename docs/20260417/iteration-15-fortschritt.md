# Iteration 15 - Reachability-Agent - Fortschritt

**Jira**: CVM-40
**Datum**: 2026-04-18
**Branch**: `claude/iteration-10-pdf-report-C9sA4`

## Zusammenfassung

Reachability-Analyse via Claude-Code-Subprozess ist verdrahtet.
Der Agent laeuft strikt read-only, schreibt einen
PENDING-Audit-Eintrag vor dem Aufruf und finalisiert ihn auf OK,
ERROR oder INVALID_OUTPUT. Ergebnis (Empfehlung + Call-Sites)
landet als `AiSuggestion` mit use-case `REACHABILITY` und einem
`AiSourceRef` pro Call-Site. Ohne Feature-Flag oder bei Subprozess-
Fehler liefert der Agent ein "nicht verfuegbar"-Ergebnis und
blockiert nichts. Default-Implementierungen sind testbar ohne
Claude-Binary (Fake-Runner und Noop-Git-Checkout).

REST: `POST /api/v1/findings/{id}/reachability`.

## Umgesetzt

### LLM-Gateway / Subprocess
- `SubprocessRunner`-Interface mit
  `SubprocessRequest`/`SubprocessResult`-Records.
- `FakeSubprocessRunner` (Default-Bean, deterministisches
  Default-JSON, `setResponseFactory(...)` fuer Tests).
- `ClaudeCodeSubprocessRunner` (real, ProcessBuilder mit Timeout
  und `destroyForcibly`, schliesst stdin, kein Netzwerk-Sandboxing
  innerhalb der JVM - wird auf Deployment-Ebene gesetzt).
- Sicherheits-Check im Compact-Constructor:
  `requireReadOnly=true` ohne `--read-only` -&gt;
  `IllegalArgumentException`.

### Prompt-Template
- `cvm/llm/prompts/reachability.st` mit Daten/Anweisungs-Trennung,
  JSON-Schema-Pflicht und ausdruecklichem
  Read-Only/No-Network-Hinweis.

### AI-Services / Reachability
- `GitCheckoutPort` Interface; `NoopGitCheckoutAdapter` Default
  (legt nur Tmp-Dir an). JGit-Adapter folgt mit Vault-Anbindung.
- `ReachabilityRequest`, `ReachabilityResult` Records (mit
  `CallSite`-Sub-Record).
- `ReachabilityConfig` (Flag, Timeout, Binary).
- `ReachabilityAgent`:
  - Schritt 1: Flag-Check.
  - Schritt 2: Finding laden, Git-Checkout, Prompt-File.
  - Schritt 3: PENDING-Audit via `AiCallAuditPort`.
  - Schritt 4: Subprocess starten.
  - Schritt 5: Output validieren (JSON, callSites-Schema).
  - Schritt 6: Audit finalisieren (OK/ERROR/INVALID_OUTPUT).
  - Schritt 7: `AiSuggestion` (use-case `REACHABILITY`) +
    `AiSourceRef` pro Call-Site persistieren.

### API
- `ReachabilityController` mit
  `POST /api/v1/findings/{id}/reachability`.
- `ReachabilityExceptionHandler` (404 bei "Finding nicht gefunden",
  400 sonst).
- Slice-Konfig + WebTests.

## Pragmatische Entscheidungen

- **Subprocess-Audit ueber bestehenden Port**: Statt eine zweite
  Audit-Tabelle zu bauen, schreiben wir den Subprozess als
  PENDING/OK-Eintrag in `ai_call_audit`. `model_id`=
  `claude-code-cli`, `system_prompt` enthaelt die
  Subprozess-Konfiguration, `user_prompt` die effektive
  Befehlszeile.
- **Noop-Git-Checkout als Default**: JGit-Integration ist
  aufwaendig (SSH-Keys, Vault, Cache). In CI / Fake-Profile
  reicht ein Tmp-Dir, weil der Subprocess sowieso gefakt ist.
  Realer Adapter folgt sobald die Vault-Bindung steht.
- **Read-Only-Erzwingung im Konstruktor**: Sicherheits-Invariante
  wird als Constructor-Vorbedingung im
  `SubprocessRequest`-Record durchgesetzt, nicht erst beim
  Subprocess-Start. Damit ist sie compile-/runtime-test-bar.
- **`unavailable`-Pfad statt Exception**: Reachability-Probleme
  sollen den Caller nicht killen. Wir liefern ein "available=false"
  mit Hinweis-Text. Der Bewerter sieht es im Vorschlag.
- **Sandboxing/Network-Policy**: Lebt auf Deployment-Ebene
  (OpenShift NetworkPolicy + Seccomp). Innerhalb der JVM nicht
  enforcable; Konzept-Hinweis im Plan-Doc dokumentiert das.

## Sicherheits-Invarianten (durch Tests gehaerttet)

- `SubprocessRunnerTest.requireReadOnlyOhneFlag`: keine Subprocess-
  Konstruktion ohne `--read-only`.
- `ReachabilityAgentTest.deaktiviert`: Flag aus -&gt; kein
  Subprozess-Aufruf, kein Audit-Eintrag.
- `ReachabilityAgentTest.timeout`: Timeout fuehrt zu
  Audit-Status `ERROR`.
- `ReachabilityAgentTest.invalidOutput` /
  `ReachabilityAgentTest.schemaVerletzung`:
  ungueltiges JSON / falsches Schema -&gt; Audit
  `INVALID_OUTPUT`, kein `AiSuggestion`.
- `ReachabilityAgentTest.subprocessFlags`: aktueller Aufruf
  enthaelt `--read-only` und `--output json`.

## Tests

### cvm-llm-gateway (+9 = jetzt 61)
- `SubprocessRunnerTest` (6).
- `ClaudeCodeSubprocessRunnerTest` (3, Linux/Mac-only via
  `@EnabledOnOs`).

### cvm-ai-services (+6 = jetzt 48)
- `ReachabilityAgentTest` (6).

### cvm-api (+3 = jetzt 38)
- `ReachabilityControllerWebTest` (3): 200, 404, 400.

### Gesamt-Testlauf
```
./mvnw -T 1C test  BUILD SUCCESS  (~74 s)
```

| Modul | Gruen | Skipped | Rot |
|---|---:|---:|---:|
| cvm-domain | 4 | 0 | 0 |
| cvm-persistence | 0 | 6 | 0 |
| cvm-application | 126 | 0 | 0 |
| cvm-integration | 8 | 0 | 0 |
| cvm-llm-gateway | **61** | 0 | 0 |
| cvm-ai-services | **48** | 0 | 0 |
| cvm-api | **38** | 0 | 0 |
| cvm-app | 0 | 5 | 0 |
| cvm-architecture-tests | 8 | 0 | 0 |
| **Gesamt** | **293** | 11 | 0 |

Iteration 15 bringt **18 neue Tests** ins System.

## Nicht im Scope

- JGit-/SSH-/Vault-Integration: GitCheckoutPort hat einen
  Noop-Adapter; reale Implementierung folgt mit der Vault-
  Anbindung.
- UI-Trigger ("Reachability pruefen"-Button) - Backend-Vertrag
  steht.
- Sprachspezifische Parser fuer Java/TS/Python: das LLM analysiert
  den Repo-Pfad selbst, kein eigener Adapter im CVM.
- Network-Sandbox innerhalb der JVM (Seccomp/NetworkPolicy auf
  Deployment-Ebene).

## Offene Punkte

- **JGit-Adapter** + Cache pro Commit + SSH-Key aus Vault.
- **UI-Trigger** im Queue-Detail.
- **Auto-Trigger nach Auto-Assessment** (wenn AI-Vorschlag
  Confidence &lt; X, automatisch Reachability anwerfen).
- **Network-Sandboxing**: OpenShift `NetworkPolicy` +
  Seccomp-Profil dokumentieren und testen.
- **Mehr Sprachen**: zusaetzliche Prompts pro Sprache (Ruby/Go).
- **`ai_source_ref.kind=CODE_REF` schon im Check-Constraint
  vorhanden** - Reachability nutzt das ohne Schema-Aenderung.

## Ausblick Iteration 16
Fix-Verifikation - automatischer Re-Scan nach einem Update,
Ueberpruefung ob das CVE behoben ist.

## Dateien (wesentlich, neu)

### LLM-Gateway
- `cvm-llm-gateway/.../subprocess/SubprocessRunner.java`
- `.../subprocess/FakeSubprocessRunner.java`
- `.../subprocess/ClaudeCodeSubprocessRunner.java`
- `cvm-llm-gateway/src/main/resources/cvm/llm/prompts/reachability.st`

### AI-Services
- `cvm-ai-services/.../reachability/GitCheckoutPort.java`
- `.../reachability/NoopGitCheckoutAdapter.java`
- `.../reachability/ReachabilityRequest.java`
- `.../reachability/ReachabilityResult.java`
- `.../reachability/ReachabilityConfig.java`
- `.../reachability/ReachabilityAgent.java`

### API
- `cvm-api/.../reachability/ReachabilityController.java`
- `.../reachability/ReachabilityExceptionHandler.java`

### Docs
- `docs/20260417/iteration-15-plan.md`
- `docs/20260417/iteration-15-fortschritt.md`
- `docs/20260417/iteration-15-test-summary.md`
