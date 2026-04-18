# UI-Exploration – Eigen-Review der Screenshots

**Quelle**: `docs/20260418/ui-exploration/screenshots/*.png`
**Reviewed by**: Claude Code (Stand 2026-04-18, view-Tool auf allen
Screenshots)
**Ergaenzt den automatischen Report**:
`docs/20260418/ui-exploration-report.md`

## Vorgehen

Gemaess CLAUDE.md §10 "Eigenverantwortung beim Bewerten der Oberflaeche"
wurden die 21 Screenshots einzeln im view-Tool geoeffnet. Pro Route
habe ich die vier Leitfragen angewendet:

1. Wuerde ein Admin wissen, was auf dieser Seite zu tun ist?
2. Ist erkennbar, ob eine Aktion erfolgreich war?
3. Sind Daten sichtbar, die im Backend existieren?
4. Gibt es einen Weg zurueck/weiter?

Der Skript-Report klassifiziert alle 19 Routen als FEHLER. Das ist
primaer CORS-bedingt (siehe Finding HIGH-3). Die Seiten rendern
trotzdem groesstenteils, sodass sich die UX-Bewertung lohnt.

## Zusammenfassung

| Prio | Anzahl | Kurz |
|---|---|---|
| HIGH | 4 | Blocker oder strukturelle Fehler |
| MEDIUM | 6 | UX-Luecken, die die Nutzung hemmen |
| LOW | 5 | Kosmetik / kleine Inkonsistenzen |

## HIGH

### HIGH-1 – Material-Icons laden nicht

**Betrifft**: alle 21 Screens.
**Symptom**: Statt Icons erscheinen 2-3-Buchstaben-Fallbacks des
jeweiligen Material-Icon-Ligatur-Namens. Beispiele aus den
Screenshots:

- Sidebar: `da` (dashboard), `cl` (cloud_upload), `ru` (rule),
  `ac` (account_tree), `ve` (verified), `se` (search/sensors),
  `de` (description), `bu` (build), `in` (inventory)
- Buttons: `fo` (folder_open), `ur` (upload), `re` (refresh),
  `ca` (category), `ga` (gavel), `tu` (tune), `se` (search)
- Empty-States: `lay` (layers), `his` (history), `ru` (rule),
  `ac` (account_tree), `ve` (verified), `se` (sensors)
- CVE-Tabelle KEV-Spalte: `war` (warning) in Rot
- Dashboard-Header: `da`, `ac`, `sh` (share), `CVM`

**Ursache (Vermutung)**: Die Material-Icons-Font (`material-icons`
oder `material-symbols-outlined`) ist nicht im
`styles.scss`/`index.html` eingebunden oder das Font-File scheitert
am CDN. Die Ligaturen sind im DOM, aber ohne Font bleibt der Text.

**Fix-Ansatz**: `cvm-frontend/src/index.html` oder `styles.scss`
auf `@import url('https://fonts.googleapis.com/icon?family=Material+Icons')`
pruefen; ggf. self-hosted Asset einbauen (Offline-Runtime!).

**Impact**: Macht das Produkt fuer einen neuen Admin kaum navigierbar.
Jede Aktion wirkt kryptisch, weil der visuelle Anker fehlt.

### HIGH-2 – Top-Navigation-Overflow (Rollen-Pills)

**Betrifft**: alle Screens.
**Symptom**: Die Rollen-Badges (Regel-Autor, Freigeber,
Regel-Freigeber, Admin, Berichte, Profil-Autor, Profil-Freigeber,
KI-Audit) fuellen den rechten Header und ueberlappen mit dem
Nutzer-Avatar und dem Produkt-Umgebung-Switcher links (der selbst in
das Logo-Feld hineinragt).

**Ursache (Vermutung)**: Die Rollen-Liste wird flexbox-mittig
ausgerichtet, ohne `overflow: hidden` oder Collapse auf einen
Popover. Auf 1280 px Breite ragt sie in andere Header-Elemente.

**Fix-Ansatz**: Rollen-Pills in einen Popover/Overflow-Menu oben
rechts legen ("Meine Rollen" Button, der bei Klick alle Pills
zeigt). Alternativ: Badges auf 1 Zeile kuerzen + "+N more".

**Impact**: Hauptnavigation verliert die Wiedererkennbarkeit des
Produktnamens. Mandanten-Auswahl wird gebrochen dargestellt (siehe
`- Produkt / Umgebung waehlen -`-Label rutscht heraus).

### HIGH-3 – Keycloak-CORS blockiert `/realms/cvm-local/account`

**Betrifft**: Jede Route loest einen Fehler
`Access to XMLHttpRequest at 'http://localhost:8080/realms/cvm-local/account'
from origin 'http://localhost:4200' has been blocked by CORS policy`
aus. Daraus entstehen die 19 FEHLER im Report.

**Ursache**: Der Keycloak-Realm `cvm-local` erlaubt `localhost:4200`
nicht als `Web Origin` oder der `account`-Endpoint wird ueberhaupt
genutzt (vermutlich `keycloak-angular` default).

**Fix-Ansatz** (zwei Optionen):

1. Keycloak-Realm-Config im Seed (`docker/keycloak/...-realm.json`)
   um `"webOrigins": ["http://localhost:4200", "+"]` fuer den
   passenden Client ergaenzen.
2. In `app.config.ts` `loadUserInfoAtStartUp: false` und den
   Account-Call deaktivieren, weil CVM die Rollen aus dem ID-Token
   bezieht.

**Impact**: Solange das scheitert, zeigt der Skript-Report jeden
Screen als FEHLER und jede echte Konsolenfehler-Analyse braucht
manuellen Filter.

### HIGH-4 – NG0600 "Signal write in effect" auf `/queue`

**Betrifft**: `/queue` (und moeglicherweise mehr).
**Symptom**: Konsolenfehler
```
NG0600: Writing to signals is not allowed in a `computed` or an
`effect` by default. Use `allowSignalWrites` in the
`CreateEffectOptions` to enable this inside effects.
  _QueueStore.reload (chunk-IRFZX6TV.js:181:23)
```

**Sichtbare Folge**: Queue-Tabelle bleibt leer ("Keine offenen
Vorschlaege fuer die aktuellen Filter."). Unklar, ob es tatsaechlich
keine gibt oder ob der State nicht gesetzt wird.

**Fix-Ansatz**: `QueueStore.reload` aus dem `effect()` heraus mit
`allowSignalWrites: true` markieren ODER die Signal-Writes in eine
Microtask / untracked-Block verschieben.

**Impact**: Hauptworkflow-Seite der Assessoren. Stoppt die
KPI-Arbeit.

## MEDIUM

### MEDIUM-1 – Severity-Farben auf `/cves` fehlen

**Betrifft**: `/cves` (CVE-Inventar-Tabelle).
**Symptom**: Severity-Pill "INFORMATIONAL" wird **grau** gerendert,
obwohl `docs/konzept/severity-farbentscheidung.md` Informational =
`#006ec7` (adesso-Blau) fordert. Auf `/tenant-kpi` sind die Pills
korrekt farbig (CRITICAL rot, HIGH orange, MEDIUM gelb, LOW teal,
INFORMATIONAL blau) - das zeigt: der `SeverityBadgeComponent`
funktioniert, wird auf der CVE-Seite aber nicht genutzt.

**Fix-Ansatz**: In `cve-inventar.component.html` den gemeinsamen
`<cvm-severity-badge>`-Selector verwenden statt der lokalen
`.severity-chip`-CSS-Klasse.

### MEDIUM-2 – `/profiles` hat keinen Empty-State und keinen CTA

**Symptom**: Der Screen zeigt nur Titel "Kontextprofile" +
Erklaerungstext + `[re] Neu laden`-Button. Es gibt weder eine
Liste vorhandener Profile noch einen "Profil anlegen"-Button, noch
einen Empty-State "Noch keine Profile - lege das erste an".

**Fix-Ansatz**: Analog zu `/admin/environments` ("Keine
Umgebungen - Lege die erste Umgebung an") einen Empty-State mit
CTA `Profil anlegen` einbauen (fuer Rolle `CVM_PROFILE_AUTHOR`).
Wenn Daten existieren, eine Card-Liste.

### MEDIUM-3 – Admin-Theme verspricht "Rollback innerhalb 24h"

**Betrifft**: `/admin/theme`.
**Symptom**: Infotext lautet woertlich "Aenderungen gelten sofort;
ein Rollback ist innerhalb von 24 Stunden moeglich."

**Problem**: Iteration 31 hat `branding_config_history` ohne
Zeit-Limit ausgeliefert. Der Endpoint
`POST /api/v1/admin/theme/rollback/{version}` akzeptiert **jede**
historisierte Version, nicht nur solche juenger als 24 h.

**Fix-Ansatz (zwei Varianten)**:

- A) Infotext anpassen: "Jede frueher gespeicherte Version kann
  zurueckgeholt werden" (ehrlichste Loesung, passt zur
  Implementierung).
- B) Im `BrandingService.rollbackForCurrentTenant` eine 24-h-Klausel
  einbauen (wenn fachlich gewollt). Erfordert Konzept-Entscheidung.

### MEDIUM-4 – Admin-Theme hat keine Rollback-UI

**Betrifft**: `/admin/theme`.
**Symptom**: Die in Iteration 31 eingebauten Endpoints
`GET /admin/theme/history` und `POST /admin/theme/rollback/{version}`
sind nicht im Formular angebunden. Kein Versions-Select, kein
"Fruehere Version wiederherstellen"-Button, keine Liste der
Audit-Eintraege.

**Fix-Ansatz**: `AdminThemeComponent` um eine Card "Historie"
ergaenzen, die `GET /admin/theme/history` aufruft und pro
Eintrag einen Rollback-Button mit Bestaetigungs-Dialog anbietet.

**Hinweis**: Dieser Finding ist in `offene-punkte.md` bereits als
TODO nach Iteration 31 vermerkt - die Exploration bestaetigt das.

### MEDIUM-5 – Reports verlangen manuelle UUID-Eingabe

**Betrifft**: `/reports`.
**Symptom**: Fuer "Hardening-Report erzeugen" muessen
"Produkt-Version (UUID)" und "Umgebung (UUID)" als Text eingegeben
werden. Es gibt keinen Picker.

**Fix-Ansatz**: Beide Felder auf Autocomplete-Dropdown umstellen,
das `/api/v1/product-versions` und `/api/v1/environments` auflistet.

### MEDIUM-6 – Seitentitel sind inkonsistent

**Symptom**: Einige Routen haben einen kleinen Untertitel-Text
("Einstellungen", "Regeln", "CVE-Inventar", "Komponenten",
"KI-Audit-Trail", "Kontextprofile"), andere einen echten H1
("Waiver-Verwaltung", "Anomalie-Board", "Alert-Historie",
"Reachability-Analysen", "Fix-Verifikation", "Mandanten-KPIs",
"Theme & Branding", "Umgebungen").

**Fix-Ansatz**: Entscheidung treffen, ob jede Seite einen H1 hat
(dann die genannten Routen anpassen). Meine Empfehlung: H1 fuer
alle. Macht das Wiederfinden im Screenshot-Vergleich leichter und
verbessert Screenreader-Semantik.

## LOW

### LOW-1 – Admin-Theme: doppelte Eingabefelder pro Farbe

Fuer "Primaerfarbe (Hex)", "Kontrastfarbe", "Akzentfarbe" und
"Schriftart" gibt es jeweils zwei nebeneinander liegende
Input-Felder. Das rechte scheint ohne Zweck - wahrscheinlich
Reste eines nicht gerenderten Farb-Pickers.

### LOW-2 – Admin-Theme: Akzentfarbe ohne Preview-Swatch

Primaerfarbe zeigt ein blaues `#006ec7`-Preview-Kaestchen, die
Akzentfarbe `#887d75` daneben nicht. Inkonsistent.

### LOW-3 – Settings: Farbschema-Toggle verwirrend

Der Slider wirkt im Off-Zustand (grauer Kreis links) und das Label
daneben sagt "Light-Mode". Beide sind im Licht-Modus korrekt, aber
der Toggle sollte sichtbar **ein** aktiven Zustand zeigen. Besser
Segment-Control mit "Hell / Dunkel / System".

### LOW-4 – `/cves`: Severity-Quicktoggles ohne Farbcodierung

Die Filter-Buttons `Alle / CRITICAL / HIGH / MEDIUM / LOW /
INFORMATIONAL` sind alle weiss/grau. Eine dezente Farbandeutung
(z. B. Rand in Severity-Farbe) wuerde den Filter schneller
scanbar machen.

### LOW-5 – `/rules` Empty-State sehr duenn

Der Empty-State liefert nur "Es sind noch keine Regeln angelegt.
Nutze 'Neue Regel anlegen'." Keine Erklaerung, was eine Regel ist
oder welche fachliche Wirkung sie hat. Admins ohne Vorwissen
stehen hier leer.

## Empfehlung zur Priorisierung

1. **HIGH-1 (Icons)** + **HIGH-2 (Nav-Overflow)** zuerst - sie
   betreffen jeden Screen und machen den Eindruck kaputt.
2. **HIGH-3 (Keycloak-CORS)** parallel, weil er die Exploration
   selbst blockiert und jeden Report mit FEHLER markiert.
3. **HIGH-4 (NG0600)** als separater kleiner Fix.
4. **MEDIUM-1 (Severity-Farben /cves)** leicht loesbar mit
   Komponenten-Austausch.
5. **MEDIUM-3 / MEDIUM-4 (Admin-Theme)** gehoeren fachlich
   zusammen (Text + UI fuer Rollback) und koennen zusammen in
   einer kleinen Iteration kommen.
6. **MEDIUM-2 (/profiles CTA)** ist eine kleine Erweiterung, passt
   zur kommenden Iteration 32 (Produkt-/Profil-Edit + Soft-Delete).
7. Der Rest als Backlog.

## Verifikation nach Fix

Nach Behebung HIGH-1/HIGH-2/HIGH-3 neuen
`scripts/run-ui-exploration.sh`-Lauf ausloesen. Im dann erstellten
Bericht muss die FEHLER-Quote deutlich sinken (CORS-Fehler
entfernt). Screenshots erneut mit view-Tool pruefen, ob
Icon-Fallbacks verschwunden sind.
