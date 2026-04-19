# Iteration 44 - Test-Summary

## Neue Unit-Tests

- `FixVerificationConfigEffectiveTest` (3 Tests)
- `RuleExtractionConfigEffectiveTest` (3 Tests)
- `AlertConfigEffectiveTest` (4 Tests)
- `AssessmentConfigEffectiveTest` (3 Tests)
- `AnomalyConfigEffectiveTest` (3 Tests)
- `SystemParameterViewTest` (4 Tests)
- `SystemParameterCatalogTest` um 1 Test erweitert
  (restart_required_markiert_richtige_keys)

## Lauf

- `./mvnw -T 1C -pl cvm-application -am test` &rarr; 311 Tests,
  BUILD SUCCESS.
- `./mvnw -T 1C -pl cvm-ai-services -am test` &rarr; 130 Tests,
  BUILD SUCCESS.
- `./mvnw -T 1C -pl cvm-app -am test` &rarr; 147 Web-Tests,
  6 Testcontainers-Tests geskippt (Sandbox-Docker),
  BUILD SUCCESS.
- `npx ng build` &rarr; Bundle generated, Budget-Warnung bleibt
  bestehen (Iteration 52 zur Reduktion geplant).
- `npx ng lint` &rarr; All files pass linting.

## Coverage / Pitest

Nicht separat erhoben.
