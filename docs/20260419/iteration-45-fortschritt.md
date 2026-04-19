# Iteration 45 - Fortschritt

**Thema**: AES-GCM-Verschluesselung fuer sensible System-Parameter.

**Jira**: CVM-95.

## Was gebaut wurde

- `cvm-application/parameter/SystemParameterSecretCipher`:
  AES-GCM (256 Bit, 12-Byte-IV, 128-Bit-Tag) mit einem aus
  `cvm.encryption.parameter-secret` per SHA-256 abgeleiteten Key.
  Format: `enc:<Base64(IV || Ciphertext+Tag)>`. `encrypt(null)=null`,
  `decrypt` ist abwaertskompatibel (Klartext ohne Praefix wird
  durchgereicht).
- `SystemParameterService` verschluesselt sensitive Werte in
  `create/changeValue/reset` transparent und entschluesselt sie vor
  dem Audit-Log-Vergleich. Die View-Ausgabe bleibt unveraendert
  maskiert (`***`).
- `SystemParameterResolver` entschluesselt sensitive Werte vor der
  Rueckgabe. Andere Aufrufer bekommen damit den Klartext-Wert, ohne
  die DB-Verschluesselung zu sehen.
- Vier neue Katalog-Eintraege (alle `sensitive=true`,
  `restartRequired=true`, `defaultValue=null`, `type=PASSWORD`):
  - `cvm.llm.claude.api-key`
  - `cvm.feed.nvd.api-key`
  - `cvm.feed.ghsa.api-key`
  - `cvm.ai.fix-verification.github.token`
- `SystemParameterCatalogBootstrap` seedet bei `sensitive=true`
  **immer** `value=null`, unabhaengig vom `defaultValue`-Feld -
  kein Secret wird versehentlich ausgerollt.
- `cvm.encryption.sbom-secret` bleibt bewusst in
  `application.yaml`/Vault (Master-Key fuer SBOM-AES-GCM, Henne-Ei
  mit dem Parameter-Store-Cipher).

## Neue Tests

- `SystemParameterSecretCipherTest` (7): Round-Trip,
  Nicht-Determinismus, null-Behandlung, leerer Wert,
  Abwaertskompatibilitaet, Tag-Mismatch, isEncrypted.
- `SystemParameterServiceSecretEncryptionTest` (4): Create-
  Verschluesselung, ChangeValue mit Alt-Wert-Entschluesselung,
  Non-sensitive bleibt unveraendert, Reset-null.
- `SystemParameterResolverTest` (+1): sensitive wird entschluesselt.
- `SystemParameterCatalogTest` (+2): Secret-Konfiguration,
  Nicht-Secret-Eintraege.
- `SystemParameterCatalogBootstrapTest` (+1): Sensitive ohne Wert.

## Build

- `./mvnw -T 1C -pl cvm-application -am test` &rarr; 325 Tests
  gruen.
- `./mvnw -T 1C -pl cvm-app -am test` &rarr; 147 Web-Tests gruen.

## Vier Leitfragen (Oberflaeche)

Die admin-parameters-Seite zeigt die vier neuen Secret-Eintraege als
PASSWORD-Typ (Eingabefeld `type=password`), mit Chips `Sensibel`
und `Neustart noetig`. Der angezeigte Wert bleibt `***`, auch nach
dem Anlegen. Audit-Log traegt die maskierte Aenderung.

## Hinweise fuer den naechsten Start

- `cvm.encryption.parameter-secret` ist **nicht** in
  `application.yaml` eingepflegt; er nutzt den harten Default
  `cvm-dev-default-parameter-secret`. Fuer Produktion muss der Wert
  per Vault/OpenShift-Secret via Umgebungsvariable gesetzt werden.
  Diese Doku-Anforderung bleibt als Punkt in offene-punkte.md.
- Keine Flyway-/Dependency-Aenderung. Kein erneutes
  `./mvnw clean install -DskipTests` noetig.

## Callsite-Migration (getrennt offen)

Die Adapter `ClaudeApiClient`, `NvdFeedClient`, `GhsaFeedClient` und
`GitHubApiProvider` lesen ihre Secrets weiterhin ueber `@Value`; der
`restartRequired=true`-Marker signalisiert, dass die DB-Aenderung
erst nach Neustart greift. Eine Umstellung auf den Resolver
(+ Lazy-Bean-Build) ist als offener Punkt eingetragen.
