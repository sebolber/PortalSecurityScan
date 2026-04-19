# Iteration 63 - Test-Summary

**Jira**: CVM-300
**Datum**: 2026-04-19

## Backend

Kommando: `./mvnw -T 1C test`

Reactor-Ergebnis: **BUILD SUCCESS** in 01:58 min.

Relevante Module:

| Modul                     | Ergebnis |
| ------------------------- | -------- |
| cvm-domain                | SUCCESS  |
| cvm-persistence           | SUCCESS  |
| cvm-application           | SUCCESS  |
| cvm-integration           | SUCCESS  |
| cvm-llm-gateway           | SUCCESS  |
| cvm-ai-services           | SUCCESS  |
| cvm-api                   | SUCCESS  |
| cvm-app                   | SUCCESS  |
| cvm-architecture-tests    | SUCCESS  |

Fokussierter Lauf:

- `ProfileControllerWebTest`: 8 Tests, 0 Failures, 0 Errors.
  - `holtAktuellesProfil`
  - `keinProfilVorhanden`
  - `draftAnlegen`
  - `draftSchemaFehler`
  - `diffLiefertLeereListeOhneAktiv` *(neu)*
  - `diffLiefert404BeiUnbekannterProfilId` *(neu)*
  - `diffLiefertEintraegeGegenAktiv` *(neu)*
  - `approveVierAugenVerstoss`

ArchUnit:

- `ParameterModulzugriffTest` -> 2 PASS
- `TenantScopeTest` -> 2 PASS
- `ModulgrenzenTest` -> 7 PASS
- `SpringBeanKonstruktorTest` -> 1 PASS

## Frontend

- `npx ng lint` -> "All files pass linting."
- `npx ng build` -> Bundle erfolgreich, initial 1.10 MB.
- Karma (`profiles.service.spec.ts`): 3 Tests PASS
  (ChromeHeadlessNoSandbox).
  - 200 mit Liste -> Liste zurueck, kein Error-Handler-Aufruf.
  - 404 -> leere Liste, kein Error-Handler-Aufruf.
  - 500 -> Promise rejected, Error-Handler wird aufgerufen.

## Coverage / Pitest

- Coverage-Report nicht separat aufgenommen (Bugfix ohne neue
  Domain-Logik). Die bestehende Cascade/Severity-Abdeckung bleibt
  unveraendert.
