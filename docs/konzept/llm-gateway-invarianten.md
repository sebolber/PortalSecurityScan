# LLM-Gateway - Invarianten (Iteration 11, CVM-30)

Diese Datei listet die sicherheitskritischen Invarianten, die das
`cvm-llm-gateway` erzwingt. Jede Invariante ist durch mindestens
einen Test abgesichert. Aenderungen an dieser Datei erfordern
Vier-Augen-Review.

---

## I1 - Kein LLM-Call ohne PENDING-Audit-Eintrag

**Durchsetzung**: `AiCallAuditService.execute(...)` ruft
`AiCallAuditPort.persistPending(...)` bevor der {@link LlmClient}
aufgerufen wird. Der Port-Vertrag garantiert einen Datenbankeintrag
mit Status `PENDING`.

**Nachweis**: `AiCallAuditServiceTest.happyPath` + Mockito-Verify
auf `persistPending`. Der Eintrag existiert, bevor `client.complete`
aufgerufen werden kann.

---

## I2 - Feature-Flag wirksam

**Durchsetzung**: Erster Pruefpunkt in
`AiCallAuditService.execute(...)`. Wenn
`LlmGatewayConfig.enabled()==false` wird `LlmDisabledException`
geworfen, **bevor** `persistPending` aufgerufen wird.

**Nachweis**: `AiCallAuditServiceTest.flagAusWirftUndKeinCall`. Mockito
verifiziert `never()` fuer `persistPending` und `client.complete`.

---

## I3 - Injection-Block erzwingt Abbruch

**Durchsetzung**: Wenn `InjectionDetector` Marker findet und
`LlmGatewayConfig.injectionMode() == BLOCK`, finalisiert der Service
den Audit mit `INJECTION_RISK` und wirft
`InjectionRiskException`. Kein `LlmClient`-Call.

**Nachweis**: `AiCallAuditServiceTest.injectionBlocked`. Warn-Modus
ist separat abgedeckt in `injectionWarn`.

---

## I4 - Output-Schema wird validiert

**Durchsetzung**: Nach jedem Call wird die strukturierte Antwort an
`OutputValidator.validate(...)` uebergeben. Fehler fuehren zu
`INVALID_OUTPUT` im Audit und `InvalidLlmOutputException`. Der Aufrufer
erhaelt den Output nicht.

**Nachweis**: `AiCallAuditServiceTest.outputInvalid`,
`OutputValidatorTest` (6 Tests).

---

## I5 - Rate-Limit wird durchgesetzt

**Durchsetzung**: `LlmRateLimiter.tryAcquire(tenant)` wird vor dem
PENDING-Eintrag geprueft - allerdings wird der Audit trotzdem mit
`RATE_LIMITED` geschrieben, damit auch abgelehnte Requests
sichtbar sind (Konzept 12.2 "jeder Call wird auditiert").
Zwei-Ebenen-Bucket (global + tenant).

**Nachweis**: `AiCallAuditServiceTest.rateLimit` +
`LlmRateLimiterTest` (3 Tests).

---

## I6 - Audit-Datensatz ist immutable (ausser Finalisierung)

**Durchsetzung**: `AiCallAuditImmutabilityListener` wirft bei jedem
Update, das nicht explizit via `finalizingAllowed=true` markiert ist.
JPA-Spalten sind bereits mit `updatable=false` abgesichert; der
Listener ist die Runtime-Garantie. Der JPA-Adapter
(`JpaAiCallAuditAdapter`) setzt das Flag nur beim Uebergang
PENDING -&gt; Final.

**Nachweis**: `JpaAiCallAuditAdapterTest.doppelFinalize` (State-Wechsel
nach Final-Status ist verboten).

---

## I7 - Persistenz nur ueber Port

**Durchsetzung**: Das Modul `cvm-llm-gateway` hat keine
JPA-/Persistenz-Abhaengigkeit. `AiCallAuditPort` ist eine Interface,
das der Adapter in `cvm-ai-services` implementiert. ArchUnit-Regel
`llm_gateway_greift_nur_auf_domain_zu` schuetzt das.

**Nachweis**: `ModulgrenzenTest` (laeuft gruen mit der aktuellen
Codebasis).

---

## I8 - Keine echten LLM-Calls in CI

**Durchsetzung**: Adapter (`ClaudeApiClient`, `OllamaClient`) sind
mit `@ConditionalOnProperty(cvm.llm.enabled=true)` versehen. Ohne das
Flag existieren die Beans nicht. Tests injizieren die Clients mit
einer `RestClient`-Instanz gegen WireMock (Test-Konstruktor). Kein
Test greift gegen api.anthropic.com oder einen echten Ollama-Server.

**Nachweis**: `ClaudeApiClientTest` und `OllamaClientTest` konstruieren
die Clients mit `new ClaudeApiClient(restClient, ...)` via Test-
Konstruktor; das Feature-Flag spielt hier keine Rolle.

---

## I9 - Prompt-Injection-Marker werden protokolliert

**Durchsetzung**: Auch im Warn-Modus (kein Abbruch) wird der Audit
mit `injectionRisk=true` geschrieben. Das erlaubt spaetere
Forensik- und Re-Classification-Runs.

**Nachweis**: `AiCallAuditServiceTest.injectionWarn` pruft das
`pending.injectionRisk()`-Feld.

---

## I10 - Kosten und Latenz werden persistiert

**Durchsetzung**: Nach erfolgreichem Call berechnet
`LlmCostCalculator` den Euro-Preis aus `cvm.llm.pricing.<modelId>`.
Latenz wird in Millisekunden gespeichert. Beides ist Teil des
Finalisierungs-Records.

**Nachweis**: `AiCallAuditServiceTest.happyPath` + `LlmCostCalculatorTest`
(3 Tests).
