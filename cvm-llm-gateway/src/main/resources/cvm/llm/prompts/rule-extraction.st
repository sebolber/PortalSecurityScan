#version: v1
#system:
Du erzeugst aus einem Cluster historischer CVE-Bewertungen einen
Regel-Vorschlag fuer den CVE-Relevance-Manager. Antworte nur in JSON:
{
  "proposedRule":{
    "name":"kurzer sprechender Name",
    "condition":{"all":[{"path":"...","op":"equals","value":"..."}]},
    "proposedSeverity":"CRITICAL|HIGH|MEDIUM|LOW|INFORMATIONAL|NOT_APPLICABLE",
    "rationaleTemplate":"Template-String, kann Platzhalter {cveKey} enthalten"
  },
  "clusterRationale":"kurze Erlaeuterung, was den Cluster verbindet",
  "exampleAssessmentIds":["...","..."]
}

Regeln (verbindlich):
- proposedSeverity DARF nur einer der sechs AhsSeverity-Werte sein.
- "condition" enthaelt nur objektive, deterministisch auswertbare
  Felder (Profilpfade, KEV, komponenten-Typ, cvssScore). Keine
  Freitext-Bedingungen.
- rationaleTemplate enthaelt KEINE Anweisung an den Leser.
- Keine Angaben ausserhalb des Schemas.
#user:
<data>
Cluster-Groesse: ${clusterSize}
Distinkte CVEs: ${cveCount}
Beispiel-Assessments:
${assessments}
Haeufige Profil-Felder:
${commonProfileFields}
Haeufige Rationale-Fragmente:
${rationaleSnippets}
</data>
