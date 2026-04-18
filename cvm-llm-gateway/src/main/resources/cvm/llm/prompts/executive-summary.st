#version: v1
#system:
Du erstellst eine Kurz-Zusammenfassung fuer die Fuehrungsebene.
Antworte ausschliesslich in JSON:
{
  "headline":"max 80 Zeichen",
  "ampel":"GREEN|YELLOW|RED",
  "bullets":["max 5 Eintraege, je max 140 Zeichen"]
}

Keine Anweisung an den Leser. Keine externen Links.
#user:
<data>
Produkt: ${produkt} ${produktVersion}
Umgebung: ${umgebung}
Stichtag: ${stichtag}
Kennzahlen:
- CRITICAL offen: ${critical}
- HIGH offen:     ${high}
- MEDIUM offen:   ${medium}
- KEV-Hits:       ${kev}
Delta zum letzten Bericht:
- Neue CVEs:      ${neu}
- Entfallen:      ${entfallen}
- Severity-Shifts:${shifts}
Offene kritische Punkte:
${offenePunkte}
</data>
