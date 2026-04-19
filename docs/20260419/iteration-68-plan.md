# Iteration 68 - Plan: GitHubApiProvider auf Parameter-Resolver

**Jira**: CVM-305

## Ziel

`GitHubApiProvider` baut heute beim Boot einen `RestClient` mit
`defaultHeader("Authorization", "Bearer " + token)`. Der Token
wird aus `@Value("${cvm.ai.fix-verification.github.token}")`
gelesen und ist damit nicht live wechselbar.

## Umsetzung

- `GitHubApiProvider` bekommt einen optionalen
  `SystemParameterResolver`. base-url bleibt via `@Value` (steht
  auch nicht auf der Migrations-Liste).
- RestClient wird ohne `Authorization`-Default-Header gebaut.
- Pro API-Call wird `resolveToken()` aufgerufen:
  1. `SystemParameterResolver.resolve("cvm.ai.fix-verification.github.token")`
  2. Fallback auf `@Value`-Default.
  Der Wert wird per Call als `Authorization: Bearer ...` Header
  gesetzt (falls nicht leer).
- Tests:
  - Neue Test-Konstruktor-Signatur, damit WireMock + Resolver
    injiziert werden koennen.
  - Neuer Testfall `tokenOverrideGreiftOhneRestart`.
- Katalog: `cvm.ai.fix-verification.github.token` auf
  hotReload=true, restartRequired=false. Damit ist die Secret-
  Liste vollstaendig migriert; `secrets_korrekt_konfiguriert`
  kann die Differenzierung auf "alle vier live-reloadable"
  vereinfachen.

## TDD

- Neuer Test: zwei Aufrufe von `releaseNotes(...)` mit einer
  veraenderlichen Token-Map; der erste Call sendet `token-1`, der
  zweite `token-2`.
- Bestehende Tests weiter gruen (fixed baseUrl, 404-Faelle).

## Abnahme

- `./mvnw -T 1C test` -> BUILD SUCCESS.
- ArchUnit-Regeln unveraendert.
