# Iteration 58 - PURL-Canonicalization

## Ziel

Kleinere Kosmetik-Unterschiede in PURLs (Gross-/Kleinschreibung im
type/namespace/name, Reihenfolge der Qualifier, leere Qualifier)
duerfen nicht dazu fuehren, dass OSV-Treffer den Scan-PURL nicht
wiederfinden. Ein kleiner Canonicalizer schliesst diese Luecke.

## Vorgehen

1. Neue Klasse `com.ahs.cvm.domain.purl.PurlCanonicalizer` (pure,
   keine Framework-Abhaengigkeit, erlaubt Domain-Platzierung):
   - lowercase type/namespace/name
   - Qualifier alphabetisch sortieren, leere Werte entfernen
   - Subpath und Version Case-sensitive belassen
   - Graceful Fallback bei unpassenden Eingaben (Rueckgabe des
     Originals)
2. Integration in `ComponentCveMatchingOnScanIngestedListener`:
   beide Schritte (Lookup-Vorbereitung + Treffer-Zuordnung) nutzen
   den kanonisierten PURL.
3. Tests fuer den Canonicalizer (10 Faelle).

## Jira

`CVM-108` - PURL-Canonicalization.
