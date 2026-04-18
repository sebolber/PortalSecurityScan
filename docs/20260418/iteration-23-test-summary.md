# Iteration 23 - Test-Summary

`./mvnw -T 1C test` -> **BUILD SUCCESS**.

## Modul-Summary

| Modul                   | Tests | Failures | Errors | Skipped          |
|-------------------------|-------|----------|--------|------------------|
| cvm-domain              |     4 |        0 |      0 |                0 |
| cvm-persistence         |     0 |        0 |      0 |   6 (Docker)     |
| cvm-application         |   174 |        0 |      0 |                0 |
| cvm-integration         |    18 |        0 |      0 |                0 |
| cvm-llm-gateway         |    62 |        0 |      0 |                0 |
| cvm-ai-services         |   111 |        0 |      0 |                0 |
| cvm-api                 |    98 |        0 |      0 |                0 |
| cvm-app                 |     0 |        0 |      0 |   5 (Docker)     |
| cvm-architecture-tests  |     8 |        0 |      0 |                0 |
| **Gesamt**              | **475** | **0** | **0** | **11 (Docker)**  |

## Neu in Iteration 23

- `cvm-api/config/KeycloakJwtAuthoritiesConverterTest` (4 Tests)
  - setzt fuer jede Realm-Rolle Authority und `ROLE_`-Variante,
  - traegt Resource-Client-Rollen ebenfalls als Authority,
  - ignoriert leere oder fehlende Claims,
  - mapped Rollen case-insensitive nach UPPERCASE.

## Unveraendert gruene Teststufen

- `ModulgrenzenTest` (7) - `api -> persistence`-Grenze bleibt
  intakt. `KeycloakJwtAuthoritiesConverter` importiert nur
  aus `org.springframework.security.*` und
  `java.util.*` - kein Persistence-Durchgriff.
- `SpringBeanKonstruktorTest` (1) - neue Config-Bean
  `jwtAuthenticationConverter` liegt in
  `@Configuration WebSecurityConfig`, Single-Constructor.

## Nicht ausgefuehrt

- End-to-End-Check via echten Keycloak-Flow (sandbox).
- Rollen-gefilterte Controller-Integrationstests - waere mit
  `@SpringBootTest + JwtAuthenticationToken` moeglich,
  aber ohne Testcontainers vs. dem Fach-Benefit aufwendig.
  Steht in den offenen Punkten.
