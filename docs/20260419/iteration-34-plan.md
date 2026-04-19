# Iteration 34 – Plan

**Thema**: Eigene LLM-Konfigurationen verwalten (Provider, Modell, Secret)
**Jira**: CVM-78
**Datum**: 2026-04-19

## Ziel

Admins sollen eigene LLM-Zugänge anlegen und aktivieren können. Die
Spezifikation kommt vom Product Owner:

| Feld | Pflicht | Beschreibung |
|---|---|---|
| name | ja | Anzeigename, max 255 |
| provider | ja | `openai`, `anthropic`, `azure`, `ollama`, `adesso-ai-hub` |
| model | ja | z. B. `gpt-4o`, `claude-sonnet-4-6`, `llama3` |
| description | nein | Freitext |
| baseUrl | nein | API-Endpoint. Leer → Provider-Default |
| secretRef | nein | API-Key, AES-verschluesselt gespeichert |
| maxTokens | nein | Integer |
| temperature | nein | Double 0.0 – 1.0 |
| isActive | nein | Nur eine pro Mandant aktiv |

Automatisch: `id` (UUID), `tenantId` (aus JWT/Context), `createdAt`,
`updatedAt`, `updatedBy`.

## Provider-Defaults

| Provider | Base-URL |
|---|---|
| openai | `https://api.openai.com/v1` |
| anthropic | `https://api.anthropic.com/v1` |
| ollama | `http://localhost:11434/v1` |
| adesso-ai-hub | `https://adesso-ai-hub.3asabc.de/v1` |
| azure | (leer - muss explizit gesetzt werden) |

## Nicht-Ziel (in dieser Iteration)

- Anbindung an den bestehenden `LlmGateway`-Code: hier waehlt der Scan-
  Ingest und die KI-Autoassessments aktuell ueber `LlmModelProfile` +
  `Environment`. Das ist fachlich an GKV-Freigabe und Budget geknuepft
  (siehe CLAUDE.md §6). Die neue `LlmConfiguration` steht zunaechst
  eigenstaendig. Schritt 2 (Iteration 35) verdrahtet sie bei Bedarf
  in den LlmGateway.
- Frontend-UI: kommt als Iteration 34b nach.

## Architektur

```
cvm-persistence/
  modelprofile/            (bestehend, unveraendert)
  llmconfig/               (NEU)
    LlmConfiguration.java
    LlmConfigurationRepository.java
    SecretEncryptionConverter.java   // AES-GCM via SbomEncryption

cvm-application/
  llmconfig/               (NEU)
    LlmConfigurationService.java
    LlmConfigurationView.java
    LlmConfigurationCommands.java    // Create/Update records
    ProviderDefaults.java

cvm-api/
  llmconfig/               (NEU)
    LlmConfigurationController.java  // POST / GET / PUT / DELETE
    LlmConfigurationRequest.java
    LlmConfigurationResponse.java    // secretRef MASKIERT
    LlmConfigExceptionHandler.java
```

## Sicherheit

- **Alle Endpunkte** erfordern `CVM_ADMIN` (via `@PreAuthorize`).
- `secretRef` wird **niemals** im Klartext zurueckgegeben. Die
  Response liefert stattdessen
  `secretSet: true|false` und `secretHint` (letzte 4 Zeichen).
- Verschluesselung wiederverwendet `SbomEncryption` (AES-GCM) und
  wird als Base64-String in der Spalte abgelegt.
- `isActive` ist ein Tenant-Singleton: beim Setzen auf `true` wird
  jedes andere Profil desselben Tenants auf `false` zurueckgesetzt
  (Transaktion).
- Mandant kommt aus `TenantContext.current()` (wie bei Branding).

## Flyway V0028

Neue Tabelle `llm_configuration` mit UNIQUE `(tenant_id, name)` und
Teil-Index auf `tenant_id WHERE is_active = TRUE`, damit maximal eine
aktive Konfig pro Mandant auf DB-Ebene durchsetzbar ist.

## Tests

- `LlmConfigurationServiceTest` (Unit, Mockito)
  - Anlegen mit Defaults (baseUrl nachziehen)
  - Anlegen mit Azure ohne baseUrl: Validierungsfehler
  - isActive=true: alte Aktive wird deaktiviert
  - Update erhaelt alte Felder, wenn nicht angegeben
  - `secretRef` wird beim Load wieder entschluesselt
- `LlmConfigurationControllerWebTest` (Slice, MockMvc)
  - POST / GET / PUT / DELETE-Happy-Path
  - Response maskiert `secretRef`
  - 403 ohne CVM_ADMIN
  - 400 bei unbekanntem Provider
- Persistence-Integration (Docker-gated): `AttributeConverter`
  entschluesselt nach Round-Trip korrekt.

## Stopp-Kriterien

- Neue Persistenz-Migration muss mit vorhandener Flyway-Kette
  kompatibel sein.
- Keine bestehenden Tests duerfen rot werden.
- Reine Admin-Funktion: nicht in `authorizeHttpRequests` ausnehmen.

## Ausblick Iteration 34b

Frontend: `/admin/llm-configurations` mit Liste + Formular. Re-Use von
`<cvm-uuid-chip>` und den Farb-Tokens.
