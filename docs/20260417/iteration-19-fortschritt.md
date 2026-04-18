# Iteration 19 - NL-Query-Dashboard + Executive-Reports - Fortschritt

**Jira**: CVM-50
**Datum**: 2026-04-18

## Zusammenfassung

**NL-Query**: Eine natuerlichsprachliche Frage an das Dashboard
wird per LLM in einen strukturierten Filter uebersetzt - der
Service generiert **nie** SQL aus der Modell-Antwort. Der
`NlFilterValidator` prueft jede einzelne Filter-Eigenschaft gegen
eine Whitelist; unbekannte Felder -&gt; Request wird mit 422
abgewiesen, kein `findAll` am Repository. Invarianten-Test
beweist: bei Prompt-Injection im Input fragt der Service keine
Daten ab.

**Executive-Report**: Board- und Audit-Variante ueber Thymeleaf +
openhtmltopdf (deterministisch). LLM-Summary
(`EXECUTIVE_SUMMARY`) liefert Headline/Ampel/Bullets; die harte
Validator-Regel beschneidet Headline (max 80 Zeichen), Bullets
(max 5 Stueck, je max 140 Zeichen) und akzeptiert nur
`GREEN|YELLOW|RED`.

## Umgesetzt

### LLM-Gateway / Prompts
- `nl-to-filter.st`: Filter-JSON mit kuratierter Feldliste.
- `executive-summary.st`: Headline/Ampel/Bullets mit Laengen-
  Vorgaben.

### AI-Services / NL-Query (`cvm-ai-services/nlquery`)
- `NlFilter` + `NlFilterValidator` (Whitelist fuer 7 Felder +
  4 Sort-Werte).
- `NlQueryResult` Read-Model (ohne rohe Entities).
- `NlQueryService`: LLM-Call ueber `AiCallAuditService`
  (use-case `NL_QUERY`), danach Validator, danach JPA-
  Stream-Filter. Kein rohes SQL aus LLM.

### AI-Services / Executive (`cvm-ai-services/executive`)
- `ExecutiveSummary` + `Validator` (hart auf 5/140/80).
- `ExecutiveReportService.generate(...)`: laed Kennzahlen ueber
  `HardeningReportDataLoader` aus Iteration 10, ruft LLM
  `EXECUTIVE_SUMMARY` ab, rendert via Thymeleaf
  (`cvm/reports/executive/board.html` bzw. `audit.html`) und
  erzeugt PDF ueber `HardeningReportPdfRenderer` (deterministisch).
- `Audience` enum: `BOARD` (1-pager) und `AUDIT` (ausfuehrlich).

### API
- `POST /api/v1/dashboard/query` (NL-Query) -&gt; 200 bei Erfolg,
  422 wenn LLM-Antwort die Whitelist verletzt.
- `GET /api/v1/reports/executive?productVersionId=&environmentId=&audience=board|audit`
  -&gt; `application/pdf` + Header `X-Executive-Ampel`.

## Sicherheits-Invarianten (durch Tests gehaertet)

- **`NlFilterValidatorTest.unbekanntesFeld`**: Nicht-Whitelist-
  Feld fuehrt zur Ablehnung.
- **`NlFilterValidatorTest.unbekannterSort`**: Sort-Werte sind
  gewhitelisted.
- **`NlQueryServiceTest.injectionLiefertKeineQuery`**: Bei
  verletzter Whitelist wird `assessmentRepo.findAll()` **nie**
  aufgerufen (Mockito `never()`).
- **`ExecutiveReportServiceTest.maxFiveBullets` + `truncateLongBullet`
  + `truncateHeadline`**: Validator kuerzt hart.
- **`ExecutiveReportServiceTest.ungueltigeAmpel`**: Fallback
  YELLOW, kein Durchreichen unerlaubter Werte.

## Pragmatische Entscheidungen

- **Stream-Filter** statt JPA Criteria API: die Fachmenge bleibt
  ueberschaubar (ca. 1k Assessments), und der Weg ist
  deterministisch testbar ohne Datenbank. Bei 100k+ Assessments
  braucht es eine Criteria-/QueryDSL-Implementation.
- **Board- und Audit-Template** im `cvm-ai-services`-Modul, nicht
  in `cvm-application`: Der Service muss den LLM-Summary bauen,
  was `cvm-application` nicht darf
  (`application -&gt; domain, persistence`).
- **`&middot;` vermeiden**: openhtmltopdf parst HTML-Named-
  Entities nicht sicher; wir nutzen numerische Entities
  (`&#183;`).
- **Result-Limit** 100 fuer NL-Query (konfigurierbar). Vermeidet
  Riesen-Responses.
- **Audit-Id-Workaround** (wie in Iterationen 13/16): offener
  Punkt.

## Tests (neu)

### `cvm-ai-services` (+16)
| Testklasse | # | Kurz |
|---|---:|---|
| `NlFilterValidatorTest` | 6 | gueltiger Filter, unbekanntes Feld, unbekannter Sort, unbekannte Severity, Status-Whitelist mit NEEDS_VERIFICATION, ungueltiger Output. |
| `NlQueryServiceTest` | 4 | Happy-Path, Prompt-Injection -&gt; keine Query, minAgeDays-Filter, leere Frage. |
| `ExecutiveReportServiceTest` | 6 | Board-PDF, max 5 Bullets, Bullet-Cap, Headline-Cap, Board vs Audit, Ampel-Fallback. |

### `cvm-api` (+5)
| Testklasse | # | Kurz |
|---|---:|---|
| `NlQueryControllerWebTest` | 3 | 200, 422, 400. |
| `ExecutiveReportControllerWebTest` | 2 | Board, Audit + X-Executive-Ampel-Header. |

### Gesamt-Testlauf
```
./mvnw -T 1C test  BUILD SUCCESS  (~70 s)
```

**Gesamt gruen: 382, Skipped 11, Rot 0.**

## Nicht im Scope

- Schreib-Queries aus NL (Invariante - gilt auch fuer Zukunft).
- Auto-Mail-Versand fuer Board-Report.
- UI-Eingabezeile (Backend-Vertrag steht).
- Delta-Berechnung im Executive-Report gegen den letzten
  Executive-Bericht: aktuell 0-Defaults; folgt wenn der Report
  dauerhaft archiviert wird.

## Offene Punkte

- **Delta-Historie** im Executive-Report: aktuell werden
  `neu/entfallen/shifts` auf 0 gesetzt. Sobald vorhergehende
  Executive-Reports archiviert werden, berechnen.
- **UI** fuer NL-Query (Eingabezeile + Filter-Copy) und
  Executive-Download-Button.
- **JPA-Criteria** fuer NL-Query-Filter bei groesseren
  Datenmengen.
- **Board-Report Goldmaster-PDF**: aktuell Text-Marker-Test, kein
  Byte-Goldmaster.
- **Plattform-Lead-Freigabe** vor Board-Report ist noch nicht
  verdrahtet (Konzept-Wunsch).

## Dateien (wesentlich, neu)

### LLM-Gateway
- `cvm-llm-gateway/.../prompts/nl-to-filter.st`
- `cvm-llm-gateway/.../prompts/executive-summary.st`

### AI-Services
- `cvm-ai-services/.../nlquery/NlFilter.java`
- `cvm-ai-services/.../nlquery/NlFilterValidator.java`
- `cvm-ai-services/.../nlquery/NlQueryResult.java`
- `cvm-ai-services/.../nlquery/NlQueryService.java`
- `cvm-ai-services/.../executive/ExecutiveSummary.java`
- `cvm-ai-services/.../executive/ExecutiveReportService.java`
- `cvm-ai-services/src/main/resources/cvm/reports/executive/board.html`
- `cvm-ai-services/src/main/resources/cvm/reports/executive/audit.html`

### API
- `cvm-api/.../nlquery/NlQueryController.java` + TestApi + WebTest
- `cvm-api/.../executive/ExecutiveReportController.java` + TestApi + WebTest

### Docs
- `docs/20260417/iteration-19-plan.md`
- `docs/20260417/iteration-19-fortschritt.md`
