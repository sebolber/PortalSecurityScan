# Iteration 45 - Test-Summary

## Neue/erweiterte Unit-Tests

- `SystemParameterSecretCipherTest` (7 Tests)
- `SystemParameterServiceSecretEncryptionTest` (4 Tests)
- `SystemParameterResolverTest` (+1 Test, jetzt 7)
- `SystemParameterCatalogTest` (+2 Tests, jetzt 13)
- `SystemParameterCatalogBootstrapTest` (+1 Test, jetzt 6)

## Lauf

- `./mvnw -T 1C -pl cvm-application -am test` &rarr; 325 Tests,
  BUILD SUCCESS.
- `./mvnw -T 1C -pl cvm-app -am test` &rarr; 147 Tests, BUILD SUCCESS.
