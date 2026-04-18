# Iteration 15 - Reachability-Agent - Plan

**Jira**: CVM-40
**Branch**: `claude/iteration-10-pdf-report-C9sA4`
**Abhaengigkeit**: Iteration 13 (KI-Vorbewertung), 11 (Audit).

## Architektur

- `cvm-llm-gateway/subprocess`:
  - `SubprocessRunner`-Interface (`SubprocessRequest` -&gt;
    `SubprocessResult`).
  - `ClaudeCodeSubprocessRunner` (real, ProcessBuilder mit
    Timeout, `--read-only`, kein Netz).
  - `FakeSubprocessRunner` (Default; CI-tauglich).
- `cvm-ai-services/reachability`:
  - `GitCheckoutPort` (clone/fetch in tmp-cache).
  - `NoopGitCheckoutAdapter` Default; `JGitCheckoutAdapter` optional
    (wird als Folge-PR aktiviert sobald JGit-Dependency wirklich
    notwendig ist).
  - `ReachabilityAgent` orchestriert: Checkout -&gt; Prompt-File -&gt;
    SubprocessRunner -&gt; JSON-Parser -&gt; Persistenz.
  - `ReachabilityResult`/`ReachabilityCallSite` Read-Models.
  - `ReachabilityConfig` mit Feature-Flag und Timeout.
- `cvm-llm-gateway/prompts`:
  - `reachability.st` (Daten/Anweisungs-Trennung, JSON-Schema).
- `cvm-ai-services/reachability`:
  - Persistiert Result als `AiSourceRef` an passenden
    `AiSuggestion`. Wenn keiner existiert, wird ein neuer
    `AiSuggestion`-Eintrag (use-case `REACHABILITY`) angelegt.
- `cvm-api/reachability`:
  - `POST /api/v1/findings/{id}/reachability` (Body optional:
    repoUrl, branch, commitSha, vulnerableSymbol).

## Sicherheits-Invarianten

1. **Read-Only**: Subprocess startet ausschliesslich mit
   `--read-only`-Flag. Fehlt das Flag, wird der Aufruf abgebrochen.
2. **Timeout** (`destroyForcibly`) - kein hangender Prozess.
3. **Kein Netz**: Adapter kapselt das in einer abstrakten
   "Sandbox-Strategie"; in CI-Tests ist es trivial (Fake-Runner).
   Ein TODO im Realadapter dokumentiert die Anforderung an die
   OpenShift-Deployment-Konfig.
4. **Audit-Pflicht**: jeder Aufruf laeuft via `AiCallAuditService`
   (use-case `REACHABILITY`). Subprocess-Metadaten landen im
   `raw_response`-Feld als JSON-String.
5. **Output-Validator**: nur strukturiertes JSON mit Pflichtfeldern
   `findings.callSites[].file/symbol/trust`.

## Tests

1. `FakeSubprocessRunnerTest` - liefert vorgegebenes JSON.
2. `ClaudeCodeSubprocessRunnerTest` - Timeout + Read-Only-Pruefung
   (gegen Echo-Skript / `cat`).
3. `ReachabilityAgentTest`:
   - Happy-Path mit Fake-Runner und Mock-GitCheckoutPort -&gt;
     Persistenz.
   - Timeout-Pfad -&gt; Note "nicht verfuegbar".
   - Output-Validator-Verstoss -&gt; Note + kein Source-Ref.
4. `ReachabilityControllerWebTest` (POST 200, 404 bei unbekanntem
   Finding).

## Scope NICHT IN

- JGit-Integration: Ein Adapter-Skelett ist da (Noop), die echte
  JGit-Implementierung ist OS-/SSH-/Vault-abhaengig und kommt mit
  einer eigenen Iteration sobald die Vault-Anbindung steht.
- Sprachspezifische Parser (Java/TS/Python): das LLM analysiert den
  Repo-Pfad selbst, der Adapter macht die statische Analyse nicht
  selbst.
- UI-Button (folgt mit Frontend-Iteration).
- ProductVersion-Erweiterung um `repo_url`/`branch`: Request-Body
  liefert diese Daten direkt; Persistenz-Erweiterung folgt.

## Stopp-Kriterien

- Aufruf ohne `--read-only` &rarr; sofortige Exception.
- Audit-Eintrag fehlt &rarr; Aufruf wird abgebrochen.
- Subprocess-Result kein gueltiges JSON &rarr; Fehlerstatus.
