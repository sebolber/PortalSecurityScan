# Iteration 01 – Test-Summary

| Modul | Testklasse | Tests | Gruen | Geskippt | Fehler |
|---|---|---:|---:|---:|---:|
| cvm-domain | `EnumTest` | 4 | 4 | 0 | 0 |
| cvm-persistence | `ProductRepositoryIntegrationsTest` | 2 | 0 | 2 | 0 |
| cvm-persistence | `AssessmentImmutableTest` | 2 | 0 | 2 | 0 |
| cvm-persistence | `FlywayMigrationReihenfolgeTest` | 2 | 0 | 2 | 0 |
| cvm-app | `SmokeIntegrationTest` | 1 | 0 | 1 | 0 |
| cvm-app | `FlywayBaselineTest` | 2 | 0 | 2 | 0 |
| cvm-architecture-tests | `ModulgrenzenTest` | 7 | 7 | 0 | 0 |

**Gesamt**: 20 Tests, 11 gruen, 9 geskippt (Docker in Sandbox nicht verfuegbar), 0 rot.

## Befehl
```
./mvnw -T 1C test
```

## Laufzeit
~15 s (Wall-Clock), inklusive aller Module.

## Hinweise
- Sobald Docker verfuegbar ist (lokal / CI), laufen die 9 geskippten
  Tests automatisch mit. Keine Codeanpassung noetig.
- `archunit.properties` mit `failOnEmptyShould=false` bleibt aktiv.
  Iteration 01 hat Packages `com.ahs.cvm.persistence.*` befuellt. Die
  verbliebenen leeren Packages (API-Controller, LLM-Gateway,
  AI-Services) werden in spaeteren Iterationen Klassen tragen. Wenn alle
  Regel-Zielpackages besetzt sind, `failOnEmptyShould=true` setzen.
