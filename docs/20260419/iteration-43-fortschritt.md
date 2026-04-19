# Iteration 43 - Fortschritt

**Thema**: Parameter-Store-Lesepfad Teil 1 (ReachabilityConfig,
AutoAssessmentConfig, OsvEffectiveProperties, FeedEffectiveProperties).

**Jira**: CVM-93.

## Was gebaut wurde

- `cvm-application/parameter/SystemParameterResolver`: liest zur
  Laufzeit den DB-Wert fuer den aktuellen Tenant-Kontext und faellt
  auf einen Aufrufer-Default zurueck. Convenience-Methoden fuer
  Boolean/Integer/Long/Double/String. Ohne Tenant-Kontext wird
  **immer** der Fallback zurueckgegeben (Hintergrund-Jobs).
- `ReachabilityConfig.*Effective()`: `enabledEffective`,
  `timeoutEffective`, `binaryEffective`. Bean bleibt als `@Value`-
  basierter Konstruktor erhalten, damit bestehende Tests
  unveraendert laufen; der Resolver wird via optionalem Setter
  eingehaengt.
- `AutoAssessmentConfig.*Effective()`: analog.
- `OsvEffectiveProperties`: neuer Bean, der `OsvProperties` und den
  Resolver kapselt. `base-url` bleibt bewusst aus der
  Nicht-migrieren-Liste heraus.
- `FeedEffectiveProperties`: liefert pro Feed (`nvd`,`ghsa`,`kev`,
  `epss`) ein `EffectiveFeed`-Record mit den wirklichen Werten,
  `base-url` und `apiKey` kommen weiter aus `FeedProperties`.

## Neue Tests

- `SystemParameterResolverTest` (6) - ohne Tenant, kein Eintrag,
  typisierte Rueckgabe, leerer DB-Wert, ungueltige Zahl, Boolean-
  Parsing.
- `ReachabilityConfigEffectiveTest` (3).
- `AutoAssessmentConfigEffectiveTest` (3).
- `OsvEffectivePropertiesTest` (3).
- `FeedEffectivePropertiesTest` (5).

## Was bewusst NICHT umgestellt wurde

- Die bestehenden `OsvComponentLookup`, `NvdFeedClient`,
  `GhsaFeedClient`, `KevFeedClient`, `EpssFeedClient` lesen weiter
  aus `OsvProperties` / `FeedProperties`. Ein Umbau der Callsites
  wuerde zahlreiche Mock-Tests anfassen; er ist als neuer offener
  Punkt aufgenommen und kann inkrementell erfolgen.
- `restartRequired=true`-Marker fuer Beans mit zementierter
  `RestClient.Builder`-Base-URL: folgt in Iteration 44.

## Vier Leitfragen (Oberflaeche)

Keine UI-Aenderung.

## Hinweise fuer den naechsten Start

- Keine Flyway-/Dependency-Aenderung.
- Die neuen `*Effective`-Methoden sind additiv: alte Aufrufer laufen
  unveraendert weiter.
