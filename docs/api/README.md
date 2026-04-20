# CVM REST-Schnittstellenbeschreibung

Stand: 2026-04-20. Dokumentiert alle unter `/api/v1/**` freigegebenen
Endpunkte des Backends. Basis ist die aktuelle Quelltext-Lage auf
`main` nach Iteration 100 (CVM-342).

> **Runtime-Quelle**: `http://localhost:8081/v3/api-docs` (SpringDoc)
> liefert die maschinenlesbare OpenAPI-Definition. Swagger-UI unter
> `http://localhost:8081/swagger-ui.html`. Diese Datei fasst den
> aktuellen Stand menschenlesbar zusammen und dokumentiert die
> Autorisierung, die in den `@PreAuthorize`-Annotationen liegt.

## Rollen

Alle Endpunkte, die nicht explizit "public" markiert sind, erfordern
ein gueltiges Keycloak-OIDC-Token. Die folgenden Spring-Authorities
werden verwendet:

| Rolle | Beschreibung |
|---|---|
| `CVM_VIEWER` | Lesezugriff auf Queue, CVEs, Assessments, Reports |
| `CVM_ASSESSOR` | Erstanalyse, Scan-Upload, Reachability, Fix-Verif. |
| `CVM_REVIEWER` | Zwei-Augen-Review |
| `CVM_APPROVER` | Freigabe (Vier-Augen) |
| `CVM_REPORTER` | PDF-/Executive-Reports erzeugen und lesen |
| `CVM_RULE_AUTHOR` | Regeln anlegen (Status DRAFT) |
| `CVM_RULE_APPROVER` | Regeln aktivieren / retiren |
| `CVM_PROFILE_AUTHOR` | Kontext-Profile anlegen (Draft) |
| `CVM_PROFILE_APPROVER` | Profil-Freigabe |
| `CVM_AI_AUDITOR` | KI-Audit-Log, Anomalie-Board |
| `CVM_ADMIN` | Alles inkl. Admin-Flaechen (Produkte, Umgebungen, Mandanten, Parameter, LLM-Konfiguration) |

Jeder Mandant wird vom `TenantContextFilter` aus dem Keycloak-Token
ermittelt. Alle Schreibvorgaenge wirken nur auf den aktuellen
Mandanten.

---

## Workflow-Endpunkte

### Scans (SBOM-Ingestion)

| Methode | Pfad | Rollen | Zweck |
|---|---|---|---|
| POST | `/api/v1/scans` (multipart) | `CVM_ADMIN`, `CVM_ASSESSOR` | CycloneDX-SBOM hochladen |
| POST | `/api/v1/scans` (JSON) | `CVM_ADMIN`, `CVM_ASSESSOR` | SBOM inline als JSON einreichen |
| GET | `/api/v1/scans/{scanId}` | `CVM_VIEWER`+ | Metadaten eines Scans |
| GET | `/api/v1/scans/{id}/delta-summary` | `CVM_REVIEWER`+ | NDJSON-Stream der LLM-Zusammenfassung |

### Bewertungs-Queue

| Methode | Pfad | Rollen | Zweck |
|---|---|---|---|
| GET | `/api/v1/findings` | `CVM_ASSESSOR`+ | Queue-Liste. Query: `status`, `productVersionId`, `environmentId`, `source`. Bei `status=null` kommen **alle** Statusse (APPROVED/REJECTED/EXPIRED inkl.) zurueck (Iteration 99). |
| GET | `/api/v1/findings/{findingId}/assessments/history` | `CVM_ASSESSOR`+ | Assessment-Audit-Trail pro Finding |

### Assessments (Bewertung + Freigabe)

| Methode | Pfad | Rollen | Zweck |
|---|---|---|---|
| POST | `/api/v1/assessments` | `CVM_ASSESSOR`+ | Neuen Vorschlag (PROPOSED) anlegen |
| POST | `/api/v1/assessments/{id}/approve` | `CVM_APPROVER` | Freigeben. Severity-Downgrade auf NOT_APPLICABLE/INFORMATIONAL erfordert `zweitfreigabe=true`. |
| POST | `/api/v1/assessments/{id}/reject` | `CVM_APPROVER` | Verwerfen mit Pflicht-Begruendung |
| POST | `/api/v1/assessments/{id}/copilot` | `CVM_ASSESSOR`+ | NDJSON-Stream eines LLM-Copilot-Dialogs |

### Reachability-Agent

| Methode | Pfad | Rollen | Zweck |
|---|---|---|---|
| POST | `/api/v1/findings/{id}/reachability` | `CVM_ASSESSOR`+ | Reachability-Analyse starten. `commitSha` ist Pflicht (JGit). |
| GET | `/api/v1/findings/{id}/reachability/suggestion` | `CVM_ASSESSOR`+ | Symbol-Vorschlag aus PURL |
| GET | `/api/v1/findings/{id}/reachability/context` | `CVM_ASSESSOR`+ | **Iteration 97**: Vorbelegung `repoUrl`/`commitSha` aus Product/Version |
| GET | `/api/v1/reachability?limit=N` | `CVM_ASSESSOR`+ | Letzte N Analysen |

### Fix-Verifikation

| Methode | Pfad | Rollen | Zweck |
|---|---|---|---|
| POST | `/api/v1/mitigations/{id}/verify-fix` | `CVM_ASSESSOR`+ | AI-Agent prueft den Fix im Git-Diff |
| GET | `/api/v1/mitigations/{id}/verification` | `CVM_ASSESSOR`+ | Ergebnis-View |
| GET | `/api/v1/fix-verification?grade=...` | `CVM_ASSESSOR`+ | Liste aller Fix-Verifikationen |

### Anomalie-Board

| Methode | Pfad | Rollen | Zweck |
|---|---|---|---|
| GET | `/api/v1/anomalies` | `CVM_AI_AUDITOR`, `CVM_ADMIN` | Anomalien |
| GET | `/api/v1/anomalies/count` | `CVM_AI_AUDITOR`, `CVM_ADMIN` | Zaehler (Banner) |

### Waiver

| Methode | Pfad | Rollen | Zweck |
|---|---|---|---|
| GET | `/api/v1/waivers` | `CVM_VIEWER`+ | Liste (Status-Filter per Query) |
| POST | `/api/v1/waivers` | `CVM_APPROVER`, `CVM_ADMIN` | Waiver anlegen |
| POST | `/api/v1/waivers/{id}/extend` | `CVM_APPROVER`, `CVM_ADMIN` | Gueltigkeit verlaengern (Vier-Augen) |
| POST | `/api/v1/waivers/{id}/revoke` | `CVM_APPROVER`, `CVM_ADMIN` | Zurueckziehen (Pflicht-Begruendung) |

### Reports (PDF)

| Methode | Pfad | Rollen | Zweck |
|---|---|---|---|
| POST | `/api/v1/reports/hardening` | `CVM_REPORTER`, `CVM_ADMIN` | Hardening-Report erzeugen |
| GET | `/api/v1/reports` | `CVM_VIEWER`+ | Paginierte Historie. Query: `productVersionId`, `environmentId`, `page`, `size` (Iteration 93) |
| GET | `/api/v1/reports/{reportId}` | `CVM_VIEWER`+ | PDF-Stream |
| GET | `/api/v1/reports/{reportId}/meta` | `CVM_VIEWER`+ | Metadaten ohne PDF-Bytes |
| GET | `/api/v1/reports/executive` | `CVM_REPORTER`, `CVM_ADMIN` | Executive-Report (NL-Query-basiert) |

### Pipeline-Gate (CI/CD)

| Methode | Pfad | Rollen | Zweck |
|---|---|---|---|
| POST | `/api/v1/pipeline/gate` | `CVM_ASSESSOR`+ | Build-Gate-Entscheidung (PASS/FAIL+Gruende) |

---

## Read-Only-Uebersichten

### Dashboard

| Methode | Pfad | Rollen | Zweck |
|---|---|---|---|
| GET | `/api/v1/dashboard/kpi` | `CVM_VIEWER`+ | **Iteration 100**: Offene CVEs, Severity-Verteilung, aeltestes CRITICAL, Weiterbetriebs-Ampel |
| POST | `/api/v1/dashboard/query` | `CVM_VIEWER`+ | Natural-Language-Query ueber Metriken |

### CVEs

| Methode | Pfad | Rollen | Zweck |
|---|---|---|---|
| GET | `/api/v1/cves` | `CVM_VIEWER`+ | Suche. Query: `q`, `severity`, `kev`, `page` |
| GET | `/api/v1/cves/{cveId}` | `CVM_VIEWER`+ | Detail inkl. EPSS/KEV/Components |

### KPIs

| Methode | Pfad | Rollen | Zweck |
|---|---|---|---|
| GET | `/api/v1/kpis` | `CVM_VIEWER`+ | Aggregierte Mandanten-KPIs |
| GET | `/api/v1/kpis/export` | `CVM_REPORTER`, `CVM_ADMIN` | CSV-Export |

### Alerts

| Methode | Pfad | Rollen | Zweck |
|---|---|---|---|
| GET | `/api/v1/alerts/banner` | `CVM_VIEWER`+ | T2-Eskalations-Zaehler fuer die Shell |
| GET | `/api/v1/alerts/history` | `CVM_VIEWER`+ | Alert-Historie |
| GET | `/api/v1/alerts/rules` | `CVM_ADMIN` | Alert-Regeln |
| POST | `/api/v1/alerts/rules` | `CVM_ADMIN` | Alert-Regel anlegen/aktualisieren |
| POST | `/api/v1/alerts/test` | `CVM_ADMIN` | Test-SMTP ausloesen |

### KI-Audit

| Methode | Pfad | Rollen | Zweck |
|---|---|---|---|
| GET | `/api/v1/ai/audits` | `CVM_AI_AUDITOR`, `CVM_ADMIN` | LLM-Call-Audit |

### Mandanten-Kontext

| Methode | Pfad | Rollen | Zweck |
|---|---|---|---|
| GET | `/api/v1/tenant/current` | authenticated | Aktueller Mandant aus dem JWT |

---

## Konfiguration / Admin

### Regeln

| Methode | Pfad | Rollen | Zweck |
|---|---|---|---|
| GET | `/api/v1/rules` | `CVM_RULE_AUTHOR`+ | Liste |
| POST | `/api/v1/rules` | `CVM_RULE_AUTHOR`+ | DRAFT anlegen |
| PUT | `/api/v1/rules/{ruleId}` | `CVM_RULE_AUTHOR`+ | DRAFT aktualisieren |
| POST | `/api/v1/rules/{ruleId}/activate` | `CVM_RULE_APPROVER`, `CVM_ADMIN` | Aktivieren (Vier-Augen) |
| DELETE | `/api/v1/rules/{ruleId}` | `CVM_ADMIN` | Soft-Delete |
| POST | `/api/v1/rules/{ruleId}/dry-run` | `CVM_RULE_AUTHOR`+ | Simulation gegen N Tage Historie |
| GET | `/api/v1/rules/suggestions` | `CVM_RULE_APPROVER`, `CVM_ADMIN` | AI-Regelvorschlaege |
| POST | `/api/v1/rules/suggestions/{id}/approve` | `CVM_RULE_APPROVER`, `CVM_ADMIN` | Vorschlag uebernehmen |
| POST | `/api/v1/rules/suggestions/{id}/reject` | `CVM_RULE_APPROVER`, `CVM_ADMIN` | Vorschlag verwerfen |

### Kontext-Profile

| Methode | Pfad | Rollen | Zweck |
|---|---|---|---|
| GET | `/api/v1/environments/{environmentId}/profile` | `CVM_PROFILE_AUTHOR`+ | Aktives Profil |
| GET | `/api/v1/environments/{environmentId}/profile/draft` | `CVM_PROFILE_AUTHOR`+ | Aktueller Draft |
| PUT | `/api/v1/environments/{environmentId}/profile` | `CVM_PROFILE_AUTHOR`+ | Draft anlegen |
| PUT | `/api/v1/profiles/{profileVersionId}` | `CVM_PROFILE_AUTHOR`+ | Draft aktualisieren |
| DELETE | `/api/v1/profiles/{profileVersionId}` | `CVM_PROFILE_AUTHOR`+ | Draft loeschen |
| POST | `/api/v1/profiles/{profileVersionId}/approve` | `CVM_PROFILE_APPROVER`, `CVM_ADMIN` | Profil freigeben (Vier-Augen) |
| GET | `/api/v1/profiles/{profileVersionId}/diff?against=...` | `CVM_PROFILE_AUTHOR`+ | Diff gegen aktive/vorherige Version |
| POST | `/api/v1/environments/{environmentId}/profile/assist` | `CVM_PROFILE_AUTHOR`+ | KI-Assistent startet Session |
| POST | `/api/v1/environments/{environmentId}/profile/assist/{sessionId}/reply` | `CVM_PROFILE_AUTHOR`+ | Folge-Frage |
| POST | `/api/v1/environments/{environmentId}/profile/assist/{sessionId}/finalize` | `CVM_PROFILE_AUTHOR`+ | YAML-Draft ableiten |

### Produkte + Versionen

| Methode | Pfad | Rollen | Zweck |
|---|---|---|---|
| GET | `/api/v1/products` | `CVM_ADMIN` | Liste |
| POST | `/api/v1/products` | `CVM_ADMIN` | Anlegen |
| PUT | `/api/v1/products/{productId}` | `CVM_ADMIN` | Aktualisieren (Name, Beschreibung, Repo-URL) |
| DELETE | `/api/v1/products/{productId}` | `CVM_ADMIN` | Soft-Delete |
| GET | `/api/v1/products/{productId}/versions` | `CVM_ADMIN` | Versionen |
| POST | `/api/v1/products/{productId}/versions` | `CVM_ADMIN` | Neue Version |
| DELETE | `/api/v1/products/{productId}/versions/{versionId}` | `CVM_ADMIN` | Soft-Delete Version |

### Umgebungen

| Methode | Pfad | Rollen | Zweck |
|---|---|---|---|
| GET | `/api/v1/environments` | authenticated | Liste |
| POST | `/api/v1/environments` | `CVM_ADMIN` | Anlegen |
| DELETE | `/api/v1/environments/{environmentId}` | `CVM_ADMIN` | Soft-Delete |

### Umgebungs-Model-Profile

| Methode | Pfad | Rollen | Zweck |
|---|---|---|---|
| POST | `/api/v1/environments/{environmentId}/model-profile/switch` | `CVM_ADMIN` | Profil wechseln (Vier-Augen) |
| GET | `/api/v1/environments/{environmentId}/model-profile/history` | `CVM_AI_AUDITOR`, `CVM_ADMIN` | Wechselhistorie |

### LLM-Model-Profile (global)

| Methode | Pfad | Rollen | Zweck |
|---|---|---|---|
| GET | `/api/v1/llm-model-profiles` | `CVM_ADMIN` | Liste |
| POST | `/api/v1/llm-model-profiles` | `CVM_ADMIN` | Anlegen |

### LLM-Konfigurationen (pro Mandant)

| Methode | Pfad | Rollen | Zweck |
|---|---|---|---|
| GET | `/api/v1/admin/llm-configurations/providers` | `CVM_ADMIN` | Unterstuetzte Provider |
| GET | `/api/v1/admin/llm-configurations` | `CVM_ADMIN` | Liste |
| GET | `/api/v1/admin/llm-configurations/active` | `CVM_ADMIN` | Aktive Konfiguration |
| GET | `/api/v1/admin/llm-configurations/{id}` | `CVM_ADMIN` | Detail |
| POST | `/api/v1/admin/llm-configurations` | `CVM_ADMIN` | Anlegen |
| PUT | `/api/v1/admin/llm-configurations/{id}` | `CVM_ADMIN` | Aktualisieren |
| DELETE | `/api/v1/admin/llm-configurations/{id}` | `CVM_ADMIN` | Loeschen |
| POST | `/api/v1/admin/llm-configurations/test` | `CVM_ADMIN` | Test-Connection auf Entwurf |
| POST | `/api/v1/admin/llm-configurations/{id}/test` | `CVM_ADMIN` | Test-Connection auf existierende Konfig |

### System-Parameter

| Methode | Pfad | Rollen | Zweck |
|---|---|---|---|
| GET | `/api/v1/admin/parameters` | `CVM_ADMIN` | Liste |
| GET | `/api/v1/admin/parameters/{id}` | `CVM_ADMIN` | Detail |
| POST | `/api/v1/admin/parameters` | `CVM_ADMIN` | Anlegen |
| PUT | `/api/v1/admin/parameters/{id}` | `CVM_ADMIN` | Definition aktualisieren |
| PATCH | `/api/v1/admin/parameters/{id}/value` | `CVM_ADMIN` | Wert aendern (Audit). Bei `sensitive=true`: AES-GCM-verschluesselt. |
| PATCH | `/api/v1/admin/parameters/{id}/reset` | `CVM_ADMIN` | Auf Default zuruecksetzen |
| DELETE | `/api/v1/admin/parameters/{id}` | `CVM_ADMIN` | Loeschen |
| GET | `/api/v1/admin/parameters/audit-log` | `CVM_ADMIN` | Audit-Log der Parameter-Aenderungen |

### Mandanten

| Methode | Pfad | Rollen | Zweck |
|---|---|---|---|
| GET | `/api/v1/admin/tenants` | `CVM_ADMIN` | Liste |
| POST | `/api/v1/admin/tenants` | `CVM_ADMIN` | Mandant anlegen |
| PATCH | `/api/v1/admin/tenants/{tenantId}/active` | `CVM_ADMIN` | Aktiv/Inaktiv |
| POST | `/api/v1/admin/tenants/{tenantId}/default` | `CVM_ADMIN` | Default setzen |

### Theme & Branding

| Methode | Pfad | Rollen | Zweck |
|---|---|---|---|
| GET | `/api/v1/theme` | public | Mandanten-Branding fuer die Shell |
| GET | `/api/v1/theme/assets/{assetId}` | public | Logo/Asset-Stream |
| PUT | `/api/v1/admin/theme` | `CVM_ADMIN` | Branding aktualisieren |
| POST | `/api/v1/admin/theme/assets` (multipart) | `CVM_ADMIN` | Logo hochladen |
| GET | `/api/v1/admin/theme/history` | `CVM_ADMIN` | Versionshistorie |
| POST | `/api/v1/admin/theme/rollback/{version}` | `CVM_ADMIN` | Rollback |

### Feeds / OSV / RAG

| Methode | Pfad | Rollen | Zweck |
|---|---|---|---|
| POST | `/api/v1/admin/enrichment/refresh` | `CVM_ADMIN` | NVD/GHSA/KEV/EPSS sofort nachladen |
| POST | `/api/v1/admin/osv-mirror/reload` | `CVM_ADMIN` | OSV-JSONL-Mirror neu einlesen |
| POST | `/api/v1/admin/rag/reindex` | `CVM_ADMIN` | pgvector-Rebuild |
| POST | `/api/v1/admin/cves/import` (multipart) | `CVM_ADMIN` | CVE-JSON-Datei importieren (air-gapped) |

### VEX

| Methode | Pfad | Rollen | Zweck |
|---|---|---|---|
| GET | `/api/v1/vex/{productVersionId}` | `CVM_ADMIN`, `CVM_REPORTER` | VEX-Export (OpenVEX / CycloneDX) |
| POST | `/api/v1/vex/import` | `CVM_ADMIN` | VEX-Import (JSON/XML) |

---

## Allgemeine Konventionen

- **Pagination**: Endpunkte mit `page`/`size` liefern
  `{ items, page, size, totalElements, totalPages }`.
- **Fehler**: 4xx-Fehler liefern ein `application/json`-Objekt
  `{ error: "<token>", message: "<de>" }`. Typische Tokens:
  `finding_not_found`, `product_key_conflict`,
  `assessment_invalid_state`, `validation_error`.
- **Zeitstempel**: Alle `Instant`-Felder sind ISO-8601 in UTC
  (`2026-04-20T08:15:00Z`).
- **IDs**: UUIDv4 als String.
- **Severities**: `CRITICAL | HIGH | MEDIUM | LOW | INFORMATIONAL | NOT_APPLICABLE`.
- **Assessment-Status**: `PROPOSED | NEEDS_REVIEW | NEEDS_VERIFICATION | APPROVED | REJECTED | EXPIRED | SUPERSEDED`.
- **Proposal-Source**: `REUSE | RULE | AI | MANUAL`.

## Audit & Sicherheit

- Jeder `LLM`-Call erzeugt einen `ai_call_audit`-Eintrag vor und nach
  dem Call. Ohne Audit kein Call.
- Vier-Augen-Prinzip wird serverseitig erzwungen fuer: Profile,
  Regeln, Waiver-Verlaengerung, LLM-Konfig-Wechsel,
  Severity-Downgrade auf `NOT_APPLICABLE`/`INFORMATIONAL`.
- Sensible System-Parameter (API-Keys) werden AES-GCM-verschluesselt
  gespeichert (Master-Key `cvm.encryption.parameter-secret`).
