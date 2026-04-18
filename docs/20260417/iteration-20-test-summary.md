# Iteration 20 - Test-Summary

**Stand**: 2026-04-18, `./mvnw -T 1C test` -&gt; BUILD SUCCESS.

## Neue Tests

| Modul | Testklasse | Tests |
|-------|------------|-------|
| cvm-application | `WaiverServiceTest` | 5 |
| cvm-application | `WaiverLifecycleJobTest` | 5 |
| cvm-application | `VexExporterTest` | 5 |
| cvm-application | `VexImporterTest` | 4 |
| cvm-api | `WaiverControllerWebTest` | 7 |
| cvm-api | `VexControllerWebTest` | 8 |

## Coverage-Schwerpunkte

- Vier-Augen fuer `grant` (`grantedBy != decidedBy`)
  und `extend` (`extendedBy != grantedBy`).
- Waiver-faehige Strategien: nur `ACCEPT_RISK`,
  `WORKAROUND` - andere -&gt; `WaiverNotApplicableException`.
- Lifecycle-Transitions: `ACTIVE` -&gt; `EXPIRING_SOON`
  bei &lt;=30 Tage Restlaufzeit, `EXPIRING_SOON` -&gt;
  `EXPIRED` bei Ablauf, Assessment auf
  `NEEDS_REVIEW`.
- VEX-Determinismus: zweimaliger Export liefert
  byte-gleiches JSON.
- VEX-Schema: nicht-CycloneDX -&gt; `errors`,
  unbekannter State -&gt; `warnings`, Statement uebersprungen.

## Gesamt

- cvm-domain: 4/4
- cvm-persistence: 13/13
- cvm-application + cvm-llm-gateway + cvm-ai-services +
  cvm-integration: 108/108
- cvm-api: 77/77 (inkl. 15 neue)
- cvm-architecture-tests: 8/8
- Build: gruen.
