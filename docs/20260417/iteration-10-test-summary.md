# Iteration 10 - PDF-Abschlussbericht - Test-Summary

**Jira**: CVM-19
**Datum**: 2026-04-18

## Testlauf

```
./mvnw -T 1C test  BUILD SUCCESS  (Wall-Clock ~54 s)
```

## Neue Tests (Iteration 10)

### `cvm-application`

| Test | Anzahl | Kurz |
|---|---:|---|
| `HardeningReportTemplateRendererTest` | 3 | Kopf/Kennzahlen; CVE-Liste mit Link; offene Punkte + Anhang. |
| `HardeningReportPdfRendererTest` | 3 | `%PDF`-Header; Marker ueber PDFBox `PDFTextStripper`; **Determinismus** (byte-gleich). |
| `ReportGeneratorServiceTest` | 3 | End-to-End mit Fake-Clock/Mock-Loader; Pflichtfelder; `findById`-Fehlerpfad. |
| `ReportDeterminismTest` | 1 | Zwei Generierungen mit gleichem Input -> byte-gleiches PDF + gleicher SHA-256. |

Summe Application: **+10 Tests**, total 122 (vorher 112).

### `cvm-api`

| Test | Anzahl | Kurz |
|---|---:|---|
| `ReportsControllerWebTest` | 4 | POST 201 + Location-Header; POST 400 bei leerem `erzeugtVon`; GET `application/pdf` + `X-Report-Sha256`-Header; GET 404. |

Summe API: **+4 Tests**, total 29 (vorher 25).

## Gesamt-Testlage

| Modul | Gruen | Geskippt (Docker) | Rot |
|---|---:|---:|---:|
| cvm-domain | 4 | 0 | 0 |
| cvm-persistence | 0 | 6 | 0 |
| cvm-application | 122 | 0 | 0 |
| cvm-integration | 8 | 0 | 0 |
| cvm-llm-gateway | 0 | 0 | 0 |
| cvm-ai-services | 0 | 0 | 0 |
| cvm-api | 29 | 0 | 0 |
| cvm-app | 0 | 5 | 0 |
| cvm-architecture-tests | 8 | 0 | 0 |
| **Gesamt** | **171** | **11** | **0** |

## Coverage

- JaCoCo-Report fuer `cvm-application/report` nicht lokal gebaut (`verify` ueberspringt
  Docker-Integration). Per Zeilen-Inspektion: alle Service-Pfade sind durch die
  drei Service-/Renderer-Tests abgedeckt.
- Pitest (Mutation Coverage) bleibt Iteration 11 zugeordnet (siehe offene-punkte.md).

## Determinismus-Nachweis

`ReportDeterminismTest` und `HardeningReportPdfRendererTest.determinismus`
vergleichen zwei unabhaengig erzeugte PDF-Byte-Arrays ueber
`assertThat(a).isEqualTo(b)`. Beide sind byte-gleich; der SHA-256-Hash
stimmt ueberein (Log: `sha256=eb45515d54765d2b8e1fe0ecae50a8c1d9d27d84a09f9ac5a817856cbd75bae4`).

## Architektur-Checks

- `ModulgrenzenTest` -> 7 gruen (keine Verletzung).
- `SpringBeanKonstruktorTest` -> 1 gruen.
- ArchUnit: `cvm-api` -> `cvm-persistence` nicht verletzt, da der
  Controller ausschliesslich gegen `application.report.*` programmiert
  und `GeneratedReportView` das Bytes-Mapping uebernimmt.
