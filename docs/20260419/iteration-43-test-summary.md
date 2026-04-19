# Iteration 43 - Test-Summary

## Unit-Tests (neu)

- `SystemParameterResolverTest` (6)
- `ReachabilityConfigEffectiveTest` (3)
- `AutoAssessmentConfigEffectiveTest` (3)
- `OsvEffectivePropertiesTest` (3)
- `FeedEffectivePropertiesTest` (5)

## Lauf

- `./mvnw -T 1C -pl cvm-application -am test -Dtest=SystemParameterResolverTest`
  &rarr; 6 Tests, BUILD SUCCESS.
- `./mvnw -T 1C -pl cvm-integration,cvm-ai-services -am test`
  &rarr; 121 Tests, BUILD SUCCESS (inkl. der neuen Effective-Tests).
- `./mvnw -T 1C -pl cvm-app -am test` &rarr; 147 Tests gruen,
  6 Testcontainers-Tests geskippt.

## Coverage / Pitest

Nicht separat erhoben.

## Hinweis

Spring-Kontext-Load bleibt stabil: `ReachabilityConfig` und
`AutoAssessmentConfig` behalten ihren `@Value`-Konstruktor und
haengen den `SystemParameterResolver` via optionalem Setter ein.
Dadurch bleiben alle bestehenden Slice-Tests ohne
Parameter-Store-Setup lauffaehig.
