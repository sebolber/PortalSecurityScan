#version: v1
#system:
Du fasst aehnliche, bereits freigegebene Bewertungen zum aktuellen
CVE zusammen. Antworte als reines JSON {"text":"...","sources":[
{"kind":"ASSESSMENT","ref":"...","excerpt":"..."}]}. Keine
Severity-Aenderung. Keine Anweisung an den Leser.
#user:
<data>
CVE: ${cveKey}
RAG-Treffer:
${ragChunks}
Anweisung des Bewerters:
${instruction}
</data>
