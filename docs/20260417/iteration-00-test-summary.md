# Iteration 00 – Test-Summary

| Testklasse | Tests | Erfolgreich | Geskippt | Fehler |
|---|---:|---:|---:|---:|
| `com.ahs.cvm.architecture.ModulgrenzenTest` | 7 | 7 | 0 | 0 |
| `com.ahs.cvm.app.SmokeIntegrationTest` | 1 | 0 | 1 | 0 |
| `com.ahs.cvm.app.FlywayBaselineTest` | 2 | 0 | 2 | 0 |

**Gesamt**: 10 Testfaelle, 7 gruen, 3 geskippt (Docker in der Sandbox nicht
verfuegbar), 0 rot.

## Laufender Befehl

```
./mvnw -T 1C test
```

Dauer des Reaktor-Laufs: ~10 s (Wall-Clock), saubere Caches.

## Coverage

In Iteration 00 ist noch kein Fachcode vorhanden. JaCoCo-Aggregation ist
konfiguriert (`<jacoco.minimum.line.coverage>0.80</jacoco.minimum.line.coverage>`),
das Gate wird erst ab Iteration 01 scharfgeschaltet.

## Pitest

Konfiguriert (Plugin-Version 1.17.0), aber in Iteration 00 nicht ausgefuehrt,
weil es keine produktiven Klassen gibt.

## Offene Punkte fuer naechste Iterationen

- Sobald Produktionscode in `cvm-domain` existiert:
  `cvm-architecture-tests/src/test/resources/archunit.properties` pruefen
  (ggf. `archRule.failOnEmptyShould=true` wieder aktivieren).
- CI: Docker-in-Docker-Testrun manuell einmal in GitLab laufen lassen, um
  Testcontainers-Reuse zu bestaetigen.
