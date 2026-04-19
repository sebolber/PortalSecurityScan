# Iteration 43 - Zugriffs-Wrapper getEffective (Teil 1)

## Ziel

Den in Iteration 41/42 gepflegten System-Parameter-Store als
**lesende Quelle** an die bestehenden `*Config`-Beans anbinden.
Lese-Reihenfolge: **Parameter-Store gewinnt** gegenueber
`application.yaml`. Nicht gesetzte Schluessel fallen auf den
bisherigen `@Value`-Default zurueck.

Iteration 43 betrifft:

- `ReachabilityConfig` (cvm-ai-services/reachability)
- `AutoAssessmentConfig` (cvm-ai-services/autoassessment)
- `OsvProperties` / neuer `OsvEffectiveProperties` (cvm-integration)
- `FeedProperties` / neuer `FeedEffectiveProperties` (cvm-integration)

Iteration 44 uebernimmt: FixVerificationConfig, RuleExtractionConfig,
AlertConfig, AssessmentConfig.

## Vorgehen

1. **Neuer Service** `SystemParameterResolver` in
   `cvm-application/parameter`: liest fuer den aktuellen Tenant-
   Kontext den Wert eines Parameters aus der DB. Liefert
   `Optional<String>` (leer, wenn kein Tenant gesetzt oder Parameter
   nicht vorhanden). Bequemlichkeits-Methoden
   `resolveBoolean/resolveInt/resolveDouble/resolveString` mit
   Fallback-Default.
2. **`ReachabilityConfig`** erhaelt `enabledEffective()`,
   `timeoutEffective()`, `binaryEffective()`, die ueber den Resolver
   den DB-Wert lesen und auf die im Konstruktor uebergebenen
   `@Value`-Fallbacks zurueckfallen.
3. **`AutoAssessmentConfig`** analog: `enabledEffective()`,
   `topKEffective()`, `minRagScoreEffective()`.
4. **`OsvEffectiveProperties`** als neuer Bean in
   `cvm-integration/osv`: kapselt `OsvProperties` und den Resolver;
   liefert `isEnabled()`, `getBatchSize()`, `getTimeoutMs()`,
   `isRetryOn429()`, `getMaxRetryAfterSeconds()` als effektive
   Werte. `OsvComponentLookup` wechselt auf diesen Bean.
5. **`FeedEffectiveProperties`** analog: liefert pro Feed
   (`nvd`,`ghsa`,`kev`,`epss`) die effektive Konfiguration. Wird
   dort verdrahtet, wo bisher `FeedProperties.FeedConfig.isEnabled()`
   etc. genutzt wurde.
6. **Tests** (Mockito + Slice-Tests ohne Docker):
   - `SystemParameterResolverTest`: kein Tenant, kein Eintrag,
     vorhandener Eintrag, sensitive-Wert wird nicht durchgereicht.
   - `ReachabilityConfigEffectiveTest`: Override der DB aendert
     `enabledEffective()` ohne Neubau des Beans.
   - `AutoAssessmentConfigEffectiveTest`: analog.
   - `OsvEffectivePropertiesTest`,
     `FeedEffectivePropertiesTest`.

## Nicht-Ziele

- `restartRequired=true`-Markierung fuer Beans, die einen
  `RestClient.Builder` im Konstruktor zementieren: Iteration 44
  (zusammen mit FixVerificationConfig etc.).
- Callsite-Migration von `OsvProperties` &rarr;
  `OsvEffectiveProperties` in allen Tests. Die OsvComponentLookup-
  Umstellung ist hier bereits inkludiert; weitere Callsites folgen
  bei Bedarf.

## Testerwartung

- `./mvnw -T 1C -pl cvm-application,cvm-integration,cvm-ai-services
  -am test` &rarr; BUILD SUCCESS.

## Jira

`CVM-93` - Parameter-Store-Lesepfad Teil 1.
