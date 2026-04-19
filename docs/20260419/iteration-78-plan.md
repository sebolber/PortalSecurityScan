# Iteration 78 - Plan: OSV-Mirror Versions-Filter

**Jira**: CVM-315

## Ziel

Iteration 72 indiziert OSV-Advisories grob per PURL und liefert
fuer jeden Treffer alle CVE-IDs der Advisory, unabhaengig von der
in der PURL steckenden Komponenten-Version. Das kann zu False
Positives fuehren, wenn eine Advisory nur fuer bestimmte Versionen
gilt.

Diese Iteration fuegt den einfachsten Filter hinzu: **exakte
Versions-Liste**. Viele OSV-Dumps geben pro Advisory ein Array
`affected[*].versions`. Wenn dieses gepflegt ist, verwenden wir
es; sonst bleibt das bisherige "match all" als konservative
Default-Annahme.

## Umfang

- `OsvJsonlMirror` speichert Advisories jetzt pro `(basePurl,
  Optional<versions>, cveIds)`.
  - `basePurl` ist die PURL ohne den `@version`-Teil.
  - `versions` ist ein `Set<String>` mit den in
    `affected.versions` gelisteten Werten (leer bedeutet "alle
    Versionen betroffen").
- `findCveIdsForPurls(List<String>)` trennt bei Query den
  `basePurl` vom `version`-Teil und matcht nur Advisories,
  deren `versions`-Set leer ist **oder** den Query-Version-
  String enthaelt.
- Abwaertskompatibilitaet: Queries ohne Version treffen weiter
  alle basePurl-Advisories.

## TDD

Neue Tests in `OsvJsonlMirrorTest`:

- Advisory ohne `versions` (nur `purl`) -> match mit Version.
- Advisory mit `versions=["1.0.0"]` -> match fuer Query
  `pkg:...@1.0.0`, KEIN match fuer `pkg:...@1.1.0`.
- Advisory mit `versions=["1.0.0","1.0.1"]` -> beide Versionen
  matchen, andere nicht.
- Query ohne Version -> trifft basePurl unabhaengig von
  `versions`-Angabe.

## Nicht-Umfang

- Keine `ranges`-/`events`-Semantik (semver introduced/fixed).
  Folge-Iteration.
- Keine ecosystem-spezifische Version-Sortierung.

## Abnahme

- `./mvnw -T 1C test` gruen.
- Bestehende `OsvJsonlMirrorTest`-Cases weiter gruen; vier neue
  Cases.
