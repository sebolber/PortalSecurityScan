# Iteration 45 - Secret-Behandlung im Parameter-Store

## Ziel

Sensitive Parameter (`sensitive=true`) duerfen nicht als Klartext in der
DB liegen. Wir fuehren AES-GCM-Verschluesselung analog zur bestehenden
`SbomEncryption` ein und nehmen die bisher ausgesparten Secret-Keys in
den Katalog auf.

## Vorgehen

1. **`SystemParameterSecretCipher`** (neu, cvm-application/parameter):
   AES-GCM (256 Bit), Key aus
   `cvm.encryption.parameter-secret:cvm-dev-default-parameter-secret`
   via SHA-256 abgeleitet. Format: `12-Byte-IV || Ciphertext+Tag`,
   Base64-encoded in der DB.
2. **`SystemParameterService.create/changeValue/reset`**:
   Wenn der Parameter `sensitive=true` traegt **und** der zu
   speichernde Wert nicht leer ist, verschluesseln wir mit dem Cipher
   bevor er in der DB landet. Der gespeicherte Wert faengt mit dem
   Marker `enc:` an, damit beim Lesen erkannt wird, ob entschluesselt
   werden muss (Abwaertskompatibilitaet: wenn der Marker fehlt, wird
   der Wert als Klartext behandelt).
3. **Lese-Pfad**: Der `SystemParameterResolver` wird um eine Methode
   `resolve(paramKey)` erweitert &rarr; entschluesselt transparent.
   `SystemParameterView` bleibt maskiert.
4. **Katalog-Erweiterung**: neue Block-A-Secret-Eintraege als
   `sensitive=true, restartRequired=true`, `defaultValue=null`:
   - `cvm.llm.claude.api-key`
   - `cvm.feed.nvd.api-key`
   - `cvm.feed.ghsa.api-key`
   - `cvm.ai.fix-verification.github.token`
5. **Bootstrap**: `sensitive=true && defaultValue==null` &rarr; Eintrag
   wird mit `value=null` angelegt. Kein Geheimnis wird versehentlich
   geseedet.
6. **Tests**:
   - `SystemParameterSecretCipherTest`: Round-Trip, verschiedene
     Eingaben.
   - `SystemParameterServiceSecretEncryptionTest`: sensitive Werte
     werden AES-GCM verschluesselt (Praefix `enc:`), lesen gibt
     Klartext zurueck, View bleibt maskiert.
   - Catalog-Test: Secrets sind `sensitive=true`,
     `restartRequired=true`, `defaultValue==null`.
   - Bootstrap-Test: Secrets bleiben ohne Wert.

## Nicht-Ziele

- Callsite-Migration der Adapter (Claude, Feeds, GitHub) vom
  `@Value` auf den Parameter-Store: getrennte Iteration, erfordert
  Lazy-Bean-Bau.
- `cvm.encryption.sbom-secret` selbst wird **nicht** migriert (sie
  ist der Master-Key fuer die SBOM-Verschluesselung und haette einen
  Henne-Ei-Effekt; sie bleibt in `application.yaml`/Vault).

## Testerwartung

- `./mvnw -T 1C -pl cvm-application -am test` &rarr; BUILD SUCCESS.

## Jira

`CVM-95` - Parameter-Store-Secret-Verschluesselung.
