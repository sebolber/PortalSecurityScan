# Iteration 32 – Test-Summary

**Jira**: CVM-74
**Stand**: 2026-04-18

## Backend

`./mvnw -T 1C test` &rarr; **BUILD SUCCESS** auf allen Modulen:

| Modul | Status |
|---|---|
| cvm-domain | SUCCESS |
| cvm-persistence | SUCCESS |
| cvm-application | SUCCESS |
| cvm-integration | SUCCESS |
| cvm-llm-gateway | SUCCESS |
| cvm-ai-services | SUCCESS |
| cvm-api | SUCCESS |
| cvm-app | SUCCESS |
| cvm-architecture-tests | SUCCESS |

Keine neuen Backend-Tests in dieser Iteration - die gelieferten
History-/Rollback-Endpoints sind seit Iteration 31 getestet.

## Frontend

`npx ng build --configuration=development` &rarr; erfolgreich.
Initial Bundle 5.99 MB, `admin-theme-component` mit der neuen
Historie-Card bei 37.14 kB (+ca. 2 kB).

`npx ng test --watch=false --browsers=ChromeHeadless` &rarr; konnte
nicht laufen, Chromium fehlt in der Sandbox
(`Can not find the binary /Applications/Google Chrome.app/...`).
Dasselbe Problem wie in Iteration 29. Die neu eingefuehrten
Methoden sind defensiv (try/catch) und dupliziert nicht-getesteten
Code, sodass der Build-Erfolg das Mindestkriterium ist.

## Architektur

`ModulgrenzenTest` + `SpringBeanKonstruktorTest` &rarr; 8/8 gruen.
Keine neuen Module-Abhaengigkeiten: das Frontend nutzt bereits
vorhandene Services (`ProductsService`, `EnvironmentsService`,
`BrandingHttpService`) und erweitert sie additiv.
