# Iteration 21 - Test-Summary

**Stand**: 2026-04-18, `./mvnw -T 1C test` -&gt; BUILD SUCCESS.

## Neue Tests

| Modul | Testklasse | Tests |
|-------|------------|-------|
| cvm-application | `LlmCostGuardTest` | 6 |
| cvm-application | `ModelProfileServiceTest` | 5 |
| cvm-application | `KpiServiceTest` | 6 |
| cvm-application | `PipelineGateServiceTest` | 6 |
| cvm-api | `ModelProfileControllerWebTest` | 5 |
| cvm-api | `KpiControllerWebTest` | 3 |
| cvm-api | `PipelineGateControllerWebTest` | 3 |

**Summe neu**: 34 Tests.

## Coverage-Schwerpunkte

- **Vier-Augen** beim Modell-Profil-Wechsel:
  `changedBy == fourEyesConfirmer -&gt;
  VierAugenViolationException` (409 im API).
- **Cost-Cap**: Summierung ueber Monatsfenster,
  Budget=0 als "unbegrenzt", unbekanntes Profil faellt offen.
- **Gate-Entscheidung**: PASS/WARN/FAIL deterministisch,
  bekannte CVEs zaehlen nicht als neu, unbekannte CVEs
  liefern Detail ohne FAIL.
- **KPI-SLA**: Burn-Down-Punkte, MTTR-Durchschnitt,
  SLA-Quote mit 50%-Szenario, Automatisierungsquote
  nur ueber `AI_SUGGESTION`-Quellen.
- **Window-Parser**: `90d`, `48h`, reine Zahl; ungueltiges
  Format -&gt; 400.

## Gesamt (alle Module)

- cvm-domain: 4/4
- cvm-persistence: 13/13
- cvm-application + cvm-llm-gateway + cvm-ai-services +
  cvm-integration: 131/131 (+23 neu).
- cvm-api: 88/88 (+11 neu).
- cvm-architecture-tests: 8/8.
- Build: gruen.
