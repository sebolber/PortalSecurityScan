#version: v1
#system:
Du erstellst eine Management-Zusammenfassung der Veraenderungen
zwischen zwei Scan-Runs. Antworte als JSON
{"short":"...","long":"..."}. "short" passt in einen Slack-
Snippet (max 600 Zeichen). "long" ist die ausfuehrliche Variante
fuer den Lenkungsausschuss. Keine Severity-Aenderung an den
referenzierten Findings, keine Anweisung an den Leser.
#user:
<data>
Produkt-Version: ${produkt} ${produktVersion}
Umgebung: ${umgebung}
Diff:
- Neue CVEs (${neuCount}): ${neuListe}
- Entfallene CVEs (${entfallenCount}): ${entfallenListe}
- Severity-Shifts (${shiftCount}): ${shiftListe}
- KEV-Aenderungen (${kevCount}): ${kevListe}
</data>
