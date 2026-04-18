# Iteration 16 - Fix-Verifikation - Test-Summary

**Jira**: CVM-41
**Datum**: 2026-04-18

## Testlauf

```
./mvnw -T 1C test  BUILD SUCCESS  (~97 s)
```

## Grade-Matrix (Test-getriebene Nachweise)

| Testfall | LLM-Antwort | Service-Final | Nachweis |
|---|---|---|---|
| CVE-ID in Release-Notes + Commit | A (EXPLICIT) | **A** | `gradeA` |
| "security improvements" + XXE-Commit (kein CVE-ID) | A (EXPLICIT) | **B** (Override) | `downgradeAaufBwennKeineCveId` |
| Release-Notes "general improvements" + `bump version` | C (NONE) | **C** | `gradeC` |
| Keine Notes, Commit nennt CVE-ID | C (NONE) | **A** (Upgrade) | `upgradeCaufAwennCveIdBelegt` |
| Flag aus | - | UNKNOWN (available=false) | `deaktiviert` |

## Neue Tests

### `cvm-integration` (+5 = jetzt 13)
| Testklasse | # | Kurz |
|---|---:|---|
| `GitHubApiProviderTest` | 5 | release-notes parse, compare, 404 (Notes), 404 (Compare), slug-Extraktion. |

### `cvm-ai-services` (+17 = jetzt 65)
| Testklasse | # | Kurz |
|---|---:|---|
| `SuspiciousCommitHeuristicTest` | 6 | CVE-ID, GHSA, Keyword, Datei-Match, Refactor (negativ), fileBasis. |
| `FixVerificationServiceTest` | 6 | Grade A/B/C, beide Overrides, deaktiviert, load-unverified. |
| `OpenFixWatchdogTest` | 5 | Upgrade erkannt, Finding mit Fix uebersprungen, kein Release, Dedup, deaktiviert. |

### `cvm-api` (+4)
| Testklasse | # | Kurz |
|---|---:|---|
| `FixVerificationControllerWebTest` | 4 | POST 200, GET, 404, 400. |

## Gesamt-Testlage

| Modul | Gruen | Skipped | Rot |
|---|---:|---:|---:|
| cvm-domain | 4 | 0 | 0 |
| cvm-persistence | 0 | 6 | 0 |
| cvm-application | 126 | 0 | 0 |
| cvm-integration | **13** | 0 | 0 |
| cvm-llm-gateway | 61 | 0 | 0 |
| cvm-ai-services | **65** | 0 | 0 |
| cvm-api | **~45** | 0 | 0 |
| cvm-app | 0 | 5 | 0 |
| cvm-architecture-tests | 8 | 0 | 0 |
| **Gesamt** | **322** | 11 | 0 |

## Sicherheits-Invarianten (durch Tests gehaertet)

- Grade-A nur mit CVE-ID-Beleg.
- Kein Auto-APPROVED-Pfad - Watchdog setzt nur NEEDS_REVIEW +
  AiSuggestion.
- Commit-Cap verhindert unbegrenzten LLM-Kontext.
- Flag-Aus -&gt; kein Netz-/LLM-Zugriff.

## Architektur

- `ModulgrenzenTest` + `SpringBeanKonstruktorTest` gruen.
- `GitProviderPort` sitzt in `cvm-integration` - `cvm-ai-services`
  ist erlaubt, darauf zuzugreifen (ai-services -&gt; integration).
