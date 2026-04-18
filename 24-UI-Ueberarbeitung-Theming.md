# Iteration 23 – UI-Komplettüberarbeitung: Modernes, harmonisches Design mit Theming

**Jira**: CVM-61
**Abhängigkeit**: alle vorherigen inkl. Iteration 22 (Settings-Center); UI-Stellen aus 07, 08, 10, 14, 18, 19, 21
**Ziel**: Die Angular-Oberfläche wird nach visuellen und strukturellen Problemen komplett
überarbeitet. Ergebnis: ein konsistentes, modernes, barrierefreies Design, das über ein
zentrales Theming-System anpassbar ist (Farben, Schriftart, Logo).

---

## 0 Kontext

Die Funktionalität ist vorhanden, aber das UI wirkt unausgereift: uneinheitliche
Abstände, inkonsistente Schriftgrößen, überlappende Eingabefelder, nicht skalierende
Bilder, wechselnde Komponentenstile zwischen den Feature-Bereichen. Diese Iteration
macht **kein** neues Feature, sondern hebt die Qualität der bestehenden Oberfläche auf
produktives Niveau und legt die Grundlage für mandantenspezifisches Branding.

**Lies zuerst**:
- `CLAUDE.md`
- `docs/konzept/CVE-Relevance-Manager-Konzept-v0.2.md`, Abschnitt 8 (UI-Konzept)
- das Ergebnis von Iteration 07 (Shell), 08 (Queue), 10 (Report-Preview),
  14 (Copilot-Streaming), 18 (Profil-Assistent-Dialog), 19 (NL-Query, Exec-Reports),
  21 (Dashboard mit KPIs/Trends)

**Bevor du irgendwas änderst**:
Durchforste `cvm-frontend/` und erstelle eine **Bestandsaufnahme** unter
`docs/YYYYMMDD/iteration-23-ui-audit.md`. Darin listest du:
- jede eigene Komponente (Name, Pfad, Zweck)
- alle direkt verwendeten Angular-Material-Komponenten
- alle Stellen mit Inline-Styles oder hardkodierten Farben/Abständen
- konkrete Problembeispiele (Screenshots unter `docs/YYYYMMDD/audit/*.png`,
  wenn Playwright dir zur Verfügung steht; sonst textuelle Beschreibung mit
  Datei- und Zeilenverweis)
- Liste der Stellen, an denen Eingabefelder/Bilder überlappen oder Proportionen
  nicht stimmen

Diese Bestandsaufnahme ist **Voraussetzung** für die Umsetzung – sie ist auch die
Abnahmebasis für Sebastian.

---

## 1 Leitprinzipien dieser Iteration

1. **Design-Tokens vor Code.** Keine Farbe, kein Font, kein Spacing-Wert taucht
   irgendwo als Literal in HTML/SCSS/TS auf. Alles läuft über CSS-Variablen, die
   aus einem zentralen Token-Layer kommen.
2. **Ein Komponenten-Kit.** Sämtliche UI-Bausteine (Button, Input, Card,
   Table, Badge, Dialog, Tabs, Tooltip, Banner, Empty-State) existieren **genau
   einmal** im Modul `shared/ui/` und werden überall wiederverwendet.
   Angular Material bleibt als Fundament, wird aber konsequent mit dem
   Token-Layer verschalt, sodass Material-Komponenten das gleiche
   Erscheinungsbild wie eigene haben.
3. **Grid-basiertes Layout.** Jede Seite folgt einem 12-Spalten-Grid mit
   definierten Breakpoints. Keine absoluten Positionierungen außer wo
   fachlich nötig (Overlay-Elemente).
4. **Keine neue Funktionalität.** Weder Features hinzufügen noch wegnehmen.
   Wenn unterwegs ein Bug auffällt: notieren unter `offene-punkte.md`, nicht
   mitsanieren.
5. **Barrierefreiheit als harte Anforderung**, nicht als Nice-to-have.
   Kontrastwerte, Fokus-Reihenfolge, Tastaturbedienung, Screenreader-Labels.
6. **Theming ist Betriebsmittel**, nicht Spielerei – die finale Lösung muss
   eine mandantenspezifische Anpassung ohne Rebuild erlauben.

---

## 1A Visuelle Grundlage: Carbon Design System als Referenz

Die Designsprache des Portals orientiert sich an **Carbon Design System v11**
(IBMs Open-Source-Enterprise-Designsystem, <https://carbondesignsystem.com/>).
Carbon ist explizit für daten-intensive Admin-, Analyse- und Compliance-
Oberflächen entworfen – genau das Profil von CVM – und liefert dokumentierte,
getestete Muster für alle zentralen Interaktionen (Dashboards, Tabellen,
Detail-Panels, Formulare, Dialoge, Queues). Die visuelle Tonalität (ruhig,
technisch, seriös) passt zum Zielpublikum (BKK-Systemadministration, CISO,
Lenkungsausschuss).

### Was übernommen wird
- **Token-Skalen**: Carbon-Gray-Skala (10–100), Carbon-Spacing-Skala (4 px–96 px),
  Carbon-Elevation-Stufen (0–4), Carbon-Typografie-Skala (Helper, Body-01/02,
  Heading-01 bis 07), Carbon-Radius-Werte. Diese Werte fließen 1:1 als
  Roh-Werte in die Token-Dateien aus Abschnitt 2.1 ein und werden dort auf
  **semantische CVM-Token** (`--color-surface`, `--space-4`, `--shadow-md` …)
  gemappt.
- **Komponenten-Konventionen**: Padding, Höhen, Hover/Focus-States, Fehler-
  darstellung, Button-Hierarchie (Primary/Secondary/Ghost/Danger), Input-
  Label-Position (immer oben), Table-Patterns, Modal-Layout, Notification-
  Banner. Claude Code nimmt die Carbon-Dokumentation als Maßstab, wenn eine
  Design-Detailfrage auftaucht.
- **Layout-Muster**: Carbons "UI Shell" (Header + Side-Nav + Content-Grid),
  die "Data Table"-Muster, die "Tile"-/"Structured-List"-Muster und die
  Carbon-Breakpoints (sm 320 px, md 672 px, lg 1056 px, xl 1312 px, max
  1584 px) als Orientierung. Die in 2.5 geforderten 12-Spalten-Grid-Werte
  werden an Carbons Raster ausgerichtet.
- **A11y-Referenz**: Carbons dokumentierte Kontrast- und Fokus-Regeln sind
  der Messstab. Wo unsere axe-core-Prüfung rot ist, ist Carbons Muster die
  Referenz-Lösung.

### Was NICHT übernommen wird
- **Kein `@carbon/angular`-npm-Paket** wird eingebunden. Die Komponenten im
  `shared/ui/`-Kit entstehen eigenständig auf Basis von Angular Material
  (aus dem bestehenden Stack) und werden **visuell** an Carbon ausgerichtet.
  Grund: Zwei Komponenten-Bibliotheken parallel erzeugen Stil- und Paket-
  Konflikte; das Token-System bekäme zwei konkurrierende Quellen.
- **Kein IBM-Branding**: keine IBM-Logos, keine IBM-Farbe als Primär-
  farbe, keine IBM-Plex-Schrift als Default (Plex darf optional verwendet
  werden, ist aber nicht Default).
- **Keine wörtliche Kopie**: Carbon ist Referenz, nicht Abklatsch. Wo eine
  CVM-spezifische Anforderung sinnvoll abweicht (z. B. Severity-Badges
  mit für GKV-Audit üblicher Farbcodierung, Bewertungs-Queue-Shortcuts aus
  Iteration 08), gilt die CVM-Anforderung.

### adesso-spezifische Overrides (bleiben Default-Theme)

Werte gemäß **adesso Corporate Design Styleguide März 2026**. Der konkrete
Default-Theme-Datensatz liegt als `default-theme.json` im Repository und
wird vom Bootstrap in die `branding_config`-Tabelle geschrieben.

- **Primärfarbe**: adesso-Blau `#006ec7` (RGB 0/110/199, CMYK 100/30/0/0,
  Pantone 3005). **Styleguide-Constraint: keine Farbabstufungen zulässig.**
  UI-State-Variationen (Hover, Pressed, Disabled) werden über Opacity-
  Layering mit Weiß/Schwarz erzeugt, nicht über Blau-Ton-Verschiebungen.
  Diese Interpretation ist zwischen Entwicklung und Brand Management
  zu bestätigen.
- **Sekundärfarbe**: adesso-Grau `#887d75` (RGB 136/125/117). Abstufungen
  10–100 % sind laut Styleguide ausdrücklich erlaubt und bilden die
  neutrale Skala des Portals (Hintergründe, Borders, Text-Sekundär).
- **Primär-Schrift**: **Fira Sans**. Klavika ist laut Styleguide S. 28
  ausschließlich den Marketing-Abteilungen vorbehalten (Process-Type-
  Foundry-Lizenz) und **darf im Portal nicht verwendet werden**. Fira Sans
  ist die adesso-freigegebene, frei lizenzierte Alternative, bereits in allen
  MS-Office-Anwendungen eingesetzt. Variable Font, lokal ausgeliefert,
  keine CDN-Abhängigkeit.
- **Mono-Schrift**: Fira Code oder Fira Mono (hält die Familien-
  Konsistenz).
- **Logo**: Basic-Variante oder health-solutions-Subsidiary-Variante
  (Styleguide S. 21) oben rechts. Diese Abweichung von Carbons Shell-
  Muster (dort oben links) ist bewusst und entspricht dem Benutzerwunsch.
  Mindestfreiraum = Höhe des „d"-Zeichens, Mindestgröße digital 40 px
  (zur Wahrung der Styleguide-Mindestgröße 8,9 mm auf üblichen
  Displays). Zulässige Hintergründe: weiß, hellgrau, adesso-Blau
  (weißes Logo), adesso-Grau (weißes Logo). Unzulässig: schwarz,
  beliebige Farben, unruhige Bilder.
- **Icon-System**: **FontAwesome Thin** gemäß Styleguide S. 34,
  Bezugsquelle Celum (`Mediathek > Corporate Design > Piktogramme`).
  Dies ersetzt die in früheren Entwürfen referenzierte Lucide-Bibliothek.
  Lizenzstufe (FontAwesome Free vs. Pro) ist mit Brand Management zu
  bestätigen.
- **Severity-Farbpalette**: CVM-spezifisch, mit **bewusster Abweichung vom
  Styleguide** für `CRITICAL`. Begründung: Security-Ops-Konvention fordert
  eindeutige Rot-Signalisierung für kritische Findings; ein rosa/pink-
  basierter Ansatz würde die funktionale Lesbarkeit im Queue-Betrieb
  verschlechtern. Die Farben `HIGH` (Orange), `LOW` (Türkis) und
  `INFORMATIONAL` (Blau) übernehmen adesso-Schmuckfarben direkt (mit
  Kontrastanpassung für WCAG AA). Diese Severity-Entscheidung gehört in
  `docs/konzept/severity-farbentscheidung.md` und erfordert einmalige
  Abstimmung mit Brand Management.
- **Schmuckfarben** (Akzente für Charts, Tags, Illustrationen): Gelb
  `#ffff00`, Orange `#ff9868`, Pink `#f566ba`, Violett `#461ebe`,
  Türkis `#28dcaa`, Grün `#76c800` gemäß Styleguide S. 26. Einsatz
  sparsam („weniger ist mehr", Styleguide-Vorgabe). Nicht verwechseln mit
  den kontrastoptimierten Severity-Farben, die eigene Tokens haben.
- **Verbotene Farben** (Styleguide S. 26 „nicht erwünscht"): stumpfe
  Herbsttöne, dunkle Ocker, rot-braune Bereiche. Diese dürfen **nirgendwo**
  in Charts, Diagrammen oder Illustrationen auftauchen. Der
  CSS-Invarianten-Test aus Abschnitt 5 wird um eine entsprechende
  Negativliste erweitert.
- **Bildsprache**: Authentische Personen- und Still-Fotografie bevorzugt.
  Verboten: Roboterhände, holografische KI-Klischees, dunkle/blaustichige
  Motive (Styleguide S. 33). Für das Portal v. a. relevant bei Login-Splash
  und Leerzuständen (`<ahs-empty-state>`).

**Offene Brand-Management-Punkte** (vor Go-Live zu klären, siehe auch
Go-Live-Checkliste Abschnitt 9):
1. Welche Logo-Variante ist für CVM korrekt – reine adesso-Basic-Marke,
   adesso-health-solutions-Subsidiary-Form, oder langfristig ein eigener
   „ad"-Präfix-Schriftzug (z. B. *adsec* oder *adcvm*)?
2. FontAwesome-Lizenzstufe: reicht Free Regular, oder ist Pro-Thin
   produktivbetrieblich freigegeben?
3. Bestätigung der Opacity-Layering-Interpretation für Blau-UI-Zustände.
4. Dark-Mode-Strategie – der Styleguide sagt dazu nichts (reines
   Print-/Marketing-Denken); eigene Entscheidung erforderlich.

### Orientierungswerte (aus Carbon v11, Ausgangsbasis für unsere Tokens)

| Kategorie | Carbon-Werte (Auszug) | Unsere semantischen Token |
|---|---|---|
| Spacing | 4 / 8 / 12 / 16 / 24 / 32 / 40 / 48 / 64 px | `--space-1` … `--space-12` |
| Radius | 0 / 4 / 8 px | `--radius-sm` / `--radius-md` / `--radius-lg` |
| Font-Size | 12 / 14 / 16 / 18 / 20 / 24 / 28 / 32 px | `--text-xs` … `--text-3xl` |
| Elevation | 0 / sm / md / lg / xl | `--shadow-xs` … `--shadow-lg` |
| Breakpoints | 320 / 672 / 1056 / 1312 / 1584 px | `--bp-sm` … `--bp-xl` |

Die exakten Werte werden beim Anlegen des Token-Layers aus der Carbon-
Dokumentation übernommen. Spätere Anpassungen (z. B. für einen Mandanten
mit engeren Abständen) erfolgen **über Theme-Overrides**, nicht durch
Änderung der Basis-Skala.

### Konsequenz für Claude Code
Wenn während dieser Iteration eine Design-Entscheidung zu treffen ist, für
die weder das Konzept noch diese Iterationsbeschreibung eine eindeutige
Vorgabe macht (z. B. "wie hoch ist der Zeilenabstand in einer Tabelle?"
oder "wie weit ragt ein Popover aus seinem Trigger?"), wird **Carbon v11**
herangezogen – nicht Material-Defaults, nicht frei erfunden. Diese Regel
reduziert den Abstimmungsbedarf und sichert visuelle Konsistenz über alle
Feature-Bereiche hinweg.

---

## 2 Scope IN

### 2.0 Funktionale Vollständigkeit: Frontend-Backend-Coverage-Audit

Diese Iteration darf kein Polieren einer Fassade mit Löchern dahinter werden.
Die Iterationen 01–22 haben eine große Zahl funktionsfähiger Backend-Features
geliefert, die im aktuellen Stand des Frontends nicht oder nur teilweise sichtbar
sind. Sebastian hat das explizit beobachtet: Navigationspunkte wie **CVEs**,
**Komponenten** und **Profile** führen auf leere Seiten, obwohl die
REST-Endpunkte und Daten dahinter existieren. Bevor Claude Code in dieser
Iteration irgendein Token anfasst, wird deshalb der Ist-Zustand aufgenommen.

#### 2.0.1 Coverage-Matrix als erster Arbeitsschritt

Schritt 1: automatische Bestandsaufnahme des Backends.
- Scan aller Controller-Klassen (`@RestController`, `@RequestMapping`) im Modul
  `cvm-application` und untergeordneten Modulen.
- Ergebnis: vollständige Liste der produktiv verfügbaren REST-Endpunkte mit
  Pfad, Methode, Rolle, verantwortlicher Iteration (abgeleitet aus Package-
  und Service-Namen).

Schritt 2: automatische Bestandsaufnahme des Frontends.
- Scan aller Angular-Services mit `HttpClient`-Aufrufen im Modul
  `cvm-frontend/src/app`.
- Scan aller Route-Definitionen in `app-routing.module.ts` und untergeordneten
  Feature-Routing-Modulen.
- Ergebnis: Liste der Frontend-Routen mit ihren zugehörigen Komponenten und
  den REST-Endpunkten, die sie tatsächlich konsumieren.

Schritt 3: Abgleich in einer Markdown-Matrix unter
`docs/YYYYMMDD/frontend-backend-coverage.md`. Spalten:

| Iteration | Backend-Endpunkt | Frontend-Route | Komponente | Status | Aktion |
|---|---|---|---|---|---|

`Status`-Werte: `ANGEBUNDEN` / `NAV_OHNE_INHALT` (Route vorhanden, Seite leer
oder ohne Datenbezug) / `FEHLT_GANZ` (weder Route noch Komponente) /
`BACKEND_FEHLT` (Frontend-Aufruf zeigt ins Leere).

#### 2.0.2 Prüfbereiche (mindestens abzudecken)

Die Matrix muss diese aus den Iterationen 01–22 abgeleiteten Bereiche
enthalten. Die Liste ist nicht abschließend – das Audit-Skript findet weitere:

- **CVE-Browser** (Iteration 03): Liste aller im System bekannten CVEs mit
  Filter nach Severity, KEV-Flag, EPSS-Schwelle, Quelle; Detail-Seite mit
  CVSS-Vector, CWEs, Referenzen, betroffenen Komponenten, Assessment-Historie
- **Komponenten-Browser** (Iteration 02): alle aus SBOMs extrahierten
  Komponenten (purl), Version, Herkunfts-Scan, Anzahl Findings
- **Kontextprofil-Editor** (Iteration 04): Liste der YAML-Profile, Editor mit
  Versionierung, Dry-Run gegen Beispielscan, Diff zu Vorgängerversion
- **Regel-Verwaltung** (Iteration 05): Regel-Liste, Editor für JSON-DSL,
  Dry-Run-Dialog, Aktivierungs-Workflow
- **Assessment-Historie** (Iteration 06): Zeitleiste aller Entscheidungen
  eines Findings, Vier-Augen-Signaturen
- **Alert-Historie** (Iteration 09): bisherige E-Mail-Alerts mit Empfänger,
  Cooldown-Status, Eskalationsstufe
- **Report-Archiv** (Iteration 10): Liste erzeugter PDF-Berichte mit Download
- **KI-Call-Audit-Browser** (Iteration 11): Liste aller LLM-Calls mit Prompt,
  Response, Token-Kosten, Modell, Feature-Flag-Kontext
- **KI-Vorbewertungs-Queue** (Iteration 13): separate Ansicht der KI-
  vorbewerteten Findings, die auf menschliche Bestätigung warten
- **Copilot-Chat-Verlauf** (Iteration 14): bisherige Copilot-Sessions pro
  Finding, SSE-fähig
- **Reachability-Ergebnisse** (Iteration 15): Pro Finding die
  Call-Graph-Evidenz, JGit-Commit-Referenz, Sandbox-Log-Auszug
- **Fix-Verifikations-Board** (Iteration 16): Quality-Grade A/B/C pro Fix-Pfad
- **KI-Regel-Queue** (Iteration 17): vorgeschlagene Regeln aus
  Pattern-Detection, Dry-Run-Ergebnis gegen Historie
- **Anomalie-Board** (Iteration 18): gestoppte KI-Vorbewertungen, Profil-
  Assistenten-Dialog
- **NL-Query-Dashboard** (Iteration 19): Natürlichsprachlicher Eingangs-Prompt,
  Audience-Switch Board/Audit, Ergebnis-Charts
- **Waiver-Verwaltung** (Iteration 20): Liste, Editor, `validUntil`-Ablauf-
  Warner, VEX-Export-Dialog
- **Mandanten-Dashboard** (Iteration 21): Cross-Tenant-KPIs (sofern Rolle),
  Row-Level-Security-Beachtung
- **Settings-Rubriken 1–15** (Iteration 22): alle 15 Rubriken inklusive
  CVE-Import-Dialog müssen unter `/settings/*` erreichbar und funktional sein

#### 2.0.3 Entscheidungsregel pro Lücke

Jede Zeile mit Status ≠ `ANGEBUNDEN` bekommt genau eine der drei Aktionen
zugewiesen:

- **(A) Anbinden** innerhalb dieser Iteration – wenn eine Basis-Anbindung
  (Liste, optional Detail-Seite, einfache Filter) in **≤ 1 Manntag** machbar
  ist. Das deckt erfahrungsgemäß CVE-Browser, Komponenten-Browser,
  Report-Archiv, Alert-Historie und Waiver-Liste ab.
- **(B) Platzhalter mit Hinweistext** – wenn die Anbindung über den Scope
  dieser Iteration hinausgeht. Die Seite zeigt dann einen definierten
  Platzhalter-Banner (`<cvm-page-placeholder>`-Komponente) mit Text
  „Dieser Bereich wird in Iteration X bearbeitet / Ticket: CVM-YYY" und
  gehört explizit zur Roadmap. Das ist zulässig für Copilot-Chat-Verlauf,
  Reachability-Ergebnisse, KI-Regel-Queue, Anomalie-Board, NL-Dashboard.
- **(C) Aus Navigation entfernen** – wenn ein Bereich konzeptionell nicht
  (mehr) vorgesehen ist oder über einen anderen Einstieg erreichbar ist.
  Die Entscheidung muss in der Matrix begründet werden.

Nicht erlaubt: eine Lücke ohne Zuordnung. Der CI-Check am Ende der Iteration
prüft, dass keine Zeile der Matrix den Status `NAV_OHNE_INHALT` ohne
zugewiesene Aktion trägt.

#### 2.0.4 Zeitbox und Priorisierung

Das Audit und die Aktion (A)-Anbindungen dürfen maximal die erste Hälfte der
Iteration beanspruchen. Wenn nach Zeitbox-Ende Bereiche nicht fertig sind, die
ursprünglich als (A) geplant waren, werden sie auf (B) umklassifiziert, nicht
auf Kosten der Theming-Arbeit verlängert. Ziel dieser Iteration bleibt das
UI-Redesign; das Audit ist Qualitätssicherung, nicht Feature-Nachholbedarf.

Priorität bei (A)-Anbindungen:
1. Bereiche, die im Alltag des Bewerters sofort auffallen (CVE-Browser,
   Komponenten-Browser, Profile-Liste, Regel-Liste)
2. Bereiche, die Nachvollziehbarkeit und Revision betreffen (Report-Archiv,
   Assessment-Historie, Alert-Historie)
3. Bereiche, die Admin-Workflows abschließen (Waiver-Liste,
   Settings-Vollständigkeit, CVE-Import)

#### 2.0.5 Konsequenz für die Go-Live-Checkliste

Das Coverage-Audit wird dauerhafter Bestandteil des Prozesses: die
Go-Live-Checkliste bekommt in Abschnitt 1 einen neuen Prüfpunkt, der das
Vorhandensein einer aktuellen `frontend-backend-coverage.md` und den Abschluss
aller `NAV_OHNE_INHALT`-Zeilen fordert. In späteren Iterationen wird die Matrix
fortgeschrieben, nicht neu erstellt.

---

### 2.1 Design-Token-System
1. Ordner `cvm-frontend/src/styles/tokens/` mit Dateien
   `colors.scss`, `typography.scss`, `spacing.scss`, `radius.scss`,
   `elevation.scss`, `motion.scss`, `breakpoints.scss`.
2. Tokens als CSS-Custom-Properties auf `:root` (nicht SCSS-Variablen – damit
   Laufzeit-Theming möglich ist).
3. Semantische Tokens (NICHT Rohwerte im Code verwenden):
   - Farbe: `--color-surface`, `--color-surface-raised`, `--color-border`,
     `--color-text`, `--color-text-muted`, `--color-primary`,
     `--color-primary-contrast`, `--color-focus`, sowie Severity-Paar
     (`--color-severity-critical` … `--color-severity-informational`,
     `--color-severity-not-applicable`) jeweils mit Hintergrund und
     Kontrast.
   - Typografie: `--font-family-sans`, `--font-family-mono`,
     Font-Size-Skala `--text-xs/sm/base/lg/xl/2xl/3xl`, Line-Heights,
     Letter-Spacing, Font-Weights.
   - Spacing: 8-Punkt-Skala, `--space-1` bis `--space-12`
     (4px, 8px, 12px, 16px, 20px, 24px, 32px, 40px, 48px, 64px, 80px, 96px)
     – Rohwerte aus Carbon v11 Spacing-Skala übernommen, siehe Abschnitt 1A.
   - Radius: `--radius-sm/md/lg/pill`.
   - Elevation (Schatten): `--shadow-xs/sm/md/lg` – sparsam eingesetzt,
     bevorzugt 1px-Border statt Schatten für ruhige Flächen.
   - Motion: `--duration-fast/base/slow`, `--easing-standard/emphasized`.
   - Breakpoints: 640 / 960 / 1280 / 1600 (mobil erreichbar, auch wenn das
     Portal primär Desktop ist).
4. Dark-Mode-Unterstützung vorbereiten: zweites Token-Set unter
   `[data-theme='dark']`. Standardmäßig `light`. Dark-Mode-Toggle in
   Iteration 22 nur ausgeliefert, wenn es sich nebenbei ergibt – **nicht**
   Scope-Erweiterung.
5. Einen **harten Lint-Check** einrichten: Stylelint-Regel, die hex-Farben,
   direkte `px`-Werte außer `1px` und direkte `font-family`-Deklarationen
   in Komponenten-Styles verbietet. Verstoß = CI-Fail.

### 2.2 Laufzeit-Theming (Branding pro Mandant)
1. `ThemeService` in `core/theming/`:
   - Lädt beim Start `GET /api/v1/theme` (neuer Endpunkt, siehe 2.6).
   - Setzt CSS-Custom-Properties dynamisch auf `document.documentElement`.
   - Lädt Logo-Asset, Font-Asset.
   - Verifiziert Kontraste (WCAG AA) vor Anwendung; bei Verletzung wird
     auf Default-Theme zurückgefallen und ein Admin-Warnbanner gesetzt.
2. Theming-Modell (Frontend):
   ```typescript
   export interface BrandingConfig {
     tenantId: string;
     logoUrl: string;               // svg bevorzugt, png erlaubt
     logoAltText: string;
     faviconUrl?: string;
     primaryColor: string;          // Hex, z.B. "#0B5FFF"
     primaryContrastColor: string;  // Hex
     accentColor?: string;
     fontFamilyHref?: string;       // Google Fonts / lokale URL
     fontFamilyName: string;        // z.B. "Inter"
     fontFamilyMonoName?: string;
     appTitle?: string;             // Default: "CVE-Relevance-Manager"
     version: number;               // Optimistic-Locking
   }
   ```
3. Asset-Hosting: Logos und Fonts werden über das Backend ausgeliefert
   (MIME-Whitelist, max. Größe 512 KB). Kein externer CDN-Zugriff,
   damit die Compliance-Zone kontrollierbar bleibt.
4. Performance: kritische Tokens werden per Inline-`<style>` im
   `index.html` gerendert (verhindert FOUC/FOFT), übrige Werte kommen
   vom `ThemeService` nach.

### 2.3 Logo & Header-Leiste
1. Logo oben **rechts** im Header (laut User-Wunsch).
2. Neben dem Logo: aktuell eingeloggter User (Initialen-Avatar mit
   Dropdown: Profil, Theme-Vorschau für Admin, Logout).
3. Links im Header: App-Titel (konfigurierbar), Produkt-/Umgebungswähler
   (bestehend aus Iteration 07, aber stilistisch integriert).
4. Mittig: optional globales Suchfeld (falls aus Iteration 19 NL-Query
   bereits vorhanden – hier nur einbetten, keine neue Funktion).
5. Kein doppeltes Logo in Sidebar – die Sidebar bekommt nur ein
   neutrales Marker-Icon.
6. Responsive: unter 960 px wird der Header kompakt, Produktwähler
   wandert in ein Dropdown, Logo verkleinert sich proportional
   (max. Höhe 32 px im kompakten Zustand, 40 px sonst).

### 2.4 Komponenten-Kit `shared/ui/`
Anlegen bzw. konsolidieren (jede Komponente als Standalone-Angular-18-
Component, ohne externe Abhängigkeit außer Angular Material):
1. `AhsButton` (Varianten: `primary`, `secondary`, `ghost`, `danger`;
   Größen: `sm`, `md`, `lg`; Zustände: `loading`, `disabled`).
2. `AhsInput`, `AhsTextarea`, `AhsSelect` mit einheitlichen Label-,
   Hilfstext- und Fehleranzeige-Positionen. Labels immer **oben**, nie
   inline/floating, um das heutige Überlappungsproblem strukturell
   auszuschließen.
3. `AhsCard` (Varianten `plain`, `raised`, `outlined`).
4. `AhsTable` mit Sticky-Header, Zebra-Optional, Sortier-Indikator,
   Pagination, Leerzustand.
5. `AhsBadge` (Severity-Badge inkl. barrierefreier Texte, nicht nur Farbe).
6. `AhsDialog` (Modal, Slide-In von rechts, Slide-In von unten für mobil).
7. `AhsTabs`, `AhsSegmentedControl`.
8. `AhsTooltip`, `AhsPopover`.
9. `AhsBanner` (Info, Warn, Critical, Erfolg – für Eskalationen,
   Kostencap-Warnungen, KI-Anomalie-Hinweise).
10. `AhsEmptyState` (Icon, Überschrift, Text, optional Aktion – wird
    überall einheitlich genutzt).
11. `AhsStatCard` (Zahl, Trend-Indikator, Sparkline – für Dashboard-KPIs
    aus Iteration 21).
12. `AhsShortcutHelp` (Overlay mit Tastatur-Shortcuts – für Queue aus
    Iteration 08).
13. `AhsCopyInline` (kleine Copy-Aktion für CVE-IDs, Commit-Hashes etc.).

Jede dieser Komponenten:
- hat einen **Storybook-Eintrag** (oder ein lightweight Äquivalent, siehe 2.7),
- hat **Unit-Tests** für Zustände und A11y-Eigenschaften,
- hat **keine** direkt eingebauten Farben/Abstände – alles über Tokens.

### 2.5 Layout- und Raster-System
1. Layout-Komponente `AhsPage` mit Slots: `pageHeader`, `pageActions`,
   `pageContent`. Jede Feature-Seite nutzt sie.
2. 12-Spalten-Grid über CSS Grid, Gap aus Spacing-Token, Max-Content-Width
   `1440px`.
3. Abstände zwischen allen Sektionen sind exakt aus Tokens
   (`--space-6` zwischen Sektionen, `--space-4` innerhalb, `--space-2`
   für eng verwandte Elemente).
4. Verbindliche Regel: Formulare benutzen `AhsForm`-Wrapper, der vertikales
   Label-Input-Fehlertext-Stapeln durchsetzt. Eingabefelder dürfen nicht
   mehr horizontal über-/unterlappen.

### 2.6 Backend: Theming-API (minimal)
Kleines Ergänzungsmodul, damit Laufzeit-Theming End-to-End funktioniert.
Im `cvm-api`/`cvm-application`:

1. Flyway `V0016__branding.sql`:
   ```sql
   CREATE TABLE branding_config (
       tenant_id UUID PRIMARY KEY,
       logo_asset_id UUID,
       favicon_asset_id UUID,
       font_asset_id UUID,
       primary_color TEXT NOT NULL,
       primary_contrast_color TEXT NOT NULL,
       accent_color TEXT,
       font_family_name TEXT NOT NULL,
       font_family_mono_name TEXT,
       app_title TEXT,
       version INT NOT NULL DEFAULT 1,
       updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
       updated_by UUID NOT NULL
   );
   CREATE TABLE branding_asset (
       id UUID PRIMARY KEY,
       tenant_id UUID NOT NULL,
       kind TEXT NOT NULL,       -- LOGO | FAVICON | FONT
       content_type TEXT NOT NULL,
       size_bytes INT NOT NULL,
       sha256 TEXT NOT NULL,
       bytes BYTEA NOT NULL,
       uploaded_at TIMESTAMPTZ NOT NULL DEFAULT now(),
       uploaded_by UUID NOT NULL
   );
   ```
2. REST:
   - `GET /api/v1/theme` – public für eingeloggte User, liefert
     `BrandingConfig` + Asset-URLs.
   - `PUT /api/v1/admin/theme` – `CVE_ADMIN`, Optimistic-Lock-Feld
     `version`.
   - `POST /api/v1/admin/theme/assets` (Multipart) – Asset-Upload,
     MIME-Whitelist (`image/svg+xml`, `image/png`, `image/x-icon`,
     `font/woff2`), Max-Size-Check.
   - `GET /api/v1/theme/assets/{id}` – Asset-Auslieferung, Caching-Header,
     ETag aus `sha256`.
3. Validierung:
   - Kontrastprüfung (WCAG AA) zwischen `primaryColor` und
     `primaryContrastColor`; Ablehnung unter Schwelle.
   - SVG-Sanitizing bei Logo-Upload (keine eingebetteten Scripts, keine
     externen Referenzen). Library: z. B. eigene Whitelist-Parser-Lösung,
     keine JavaScript-Aktivierung im SVG.
   - Font-Upload: nur `woff2`, Größenlimit, Herkunftsnachweis-Feld
     (Lizenz-Bezeichnung optional mitspeichern).
4. Audit: jede Änderung erzeugt `AuditTrail`-Eintrag.
5. Vier-Augen-Prinzip bei Theme-Änderungen **nicht** verpflichtend
   (Branding ist kein sicherheitsrelevanter Vorgang). Aber: Änderung
   erzeugt Banner „Theme geändert, Rollback innerhalb 24 h möglich"
   mit einem One-Click-Rollback, das auf die vorherige Version zurückspringt
   (`branding_config_history` als zweite Tabelle, nicht ins MVP aber im
   Modell vorbereitet über `version`-Feld).

### 2.7 Theme-Admin-UI
Neue Route `/admin/theme` (Rolle `CVE_ADMIN`):
1. Linker Bereich: Formular mit Farb-Pickern (`primaryColor`,
   `primaryContrastColor`, `accentColor`), Font-Auswahl (Liste
   freigegebener Fonts oder Upload), Logo-Upload (Drag-&-Drop,
   SVG/PNG bis 512 KB), Favicon-Upload, App-Titel.
2. Rechter Bereich: **Live-Vorschau** in einem isolierten Iframe, das das
   Theme im Kontext einer Beispielseite zeigt (Header, eine Card, eine
   Tabelle mit Severity-Badges, ein Eingabeformular). Vorschau lädt die
   Tokens ohne globales Anwenden.
3. Kontrast-Indikator live neben jedem Farbpaar (AA/AAA/failing).
4. Button „Speichern" schaltet nach Bestätigungsdialog
   („Änderung betrifft alle Nutzer des Mandanten.") aktiv.
5. Button „Als Standard zurücksetzen" lädt das ausgelieferte
   Default-Theme (adesso-health-solutions-Voreinstellung).

### 2.8 Migrations-Schritt aller bestehenden Views
Vollständige Umstellung der vorhandenen Features auf das neue Kit. Reihenfolge
(je ein sauberer Commit pro Feature-Bereich):
1. Shell + Header + Sidebar (Iteration 07)
2. Dashboard + KPI-Cards + Trend-Charts (Iterationen 07, 21)
3. Bewertungs-Queue + Detail-Panel + Shortcuts (Iteration 08)
4. CVE-Detail, Komponenten-Inventar (falls vorhanden)
5. Profil-Editor + Profil-Assistent (Iterationen 04, 18)
6. Regel-Editor + Regel-Vorschlags-Tab (Iterationen 05, 17)
7. Reports (Hardening-Report-Vorschau, Executive) (Iterationen 10, 19)
8. KI-Audit-Ansicht (Iteration 11 ff.)
9. Waiver-Liste (Iteration 20)
10. **Settings-Center inkl. aller Unterdialoge** (Iteration 22) – alle neu eingeführten
    Dialoge auf `shared/ui/` umstellen, Live-Vorschau und Form-Validierung bleiben
    funktional unverändert
11. Theme-Admin (neu, diese Iteration)

Bei jedem Bereich:
- Alten SCSS-Code entfernen, durch Tokens ersetzen.
- Tabellen, Cards, Formulare, Buttons auf die `Ahs*`-Komponenten umstellen.
- Überlappungs-Problemstellen explizit dokumentieren und fixen (siehe
  Audit aus Abschnitt 0).
- Charts auf Severity-Farb-Tokens umstellen; ECharts-Theme-Definition
  via `ChartThemeService`, der die Tokens ausliest.
- Mindestabstände zu Nachbarsektionen prüfen.

### 2.9 Charts (ECharts) – Theme-Integration
1. `ChartThemeService.buildTheme()` liest CSS-Variablen aus und baut ein
   ECharts-Theme-Objekt.
2. Abonniert sich auf `ThemeService.onThemeChanged$` und aktualisiert
   live.
3. Alle Charts verwenden dieses Theme – keine Direktfarben mehr.

### 2.10 Typografie-Feintuning
1. Standard-Font: **Inter** (oder in Standard-Theme adesso-CI-Schrift,
   falls Lizenz vorhanden). Variable Font, `font-display: swap`.
2. Lokal ausgeliefert in `cvm-frontend/src/assets/fonts/`, nicht von
   Google Fonts CDN (Compliance-Zone).
3. Typographische Hierarchie (Seitentitel, Sektionstitel, Feldlabel,
   Body, Caption) als Utility-Klassen `.text-title-lg`,
   `.text-title-md`, `.text-body`, `.text-caption`, `.text-code`
   (letztere mit Mono-Font).
4. Konsequenzen: keine hardcoded `font-size` irgendwo, Seitentitel ist
   immer gleich groß, Tabellen-Header ist immer gleich hoch.

### 2.11 Barrierefreiheit
1. Alle interaktiven Elemente mit sichtbarem Fokus-Ring (`outline` aus
   Token `--color-focus`).
2. Tastaturbedienung durchgeprüft und dokumentiert (siehe 2.13).
3. Severity wird **nie** nur über Farbe kommuniziert – immer auch Text
   oder Icon.
4. Aria-Labels auf allen Icon-Only-Buttons.
5. Kontrast-Check automatisiert via axe-core in Playwright-Tests.

### 2.12 Motion & Mikro-Interaktionen
1. Einheitliche Transitions: `--duration-base 180ms`, `--easing-standard
   cubic-bezier(0.2, 0, 0, 1)`.
2. Keine auffälligen Animationen. Ziel: ruhig, aber lebendig.
3. `prefers-reduced-motion` strikt respektieren.

### 2.13 Tastatur-Shortcuts
1. Globale Shortcuts konsolidieren: `?` öffnet
   Shortcut-Overlay, `g q` → Queue, `g d` → Dashboard, `g p` → Profil,
   `g r` → Regeln.
2. Bereichsinterne Shortcuts (Queue aus Iteration 08) nicht doppeln,
   sondern in Overlay aufnehmen.

---

## 3 Scope NICHT IN

- Keine neuen Features, keine neuen Endpunkte außer den in 2.6 genannten
  Theming-Endpunkten.
- Keine Änderung an Geschäftslogik, Datenmodell (außer `branding_*`-Tabellen),
  Scan-Ingest, Cascade, Regel-Engine, KI-Services.
- Kein Dark-Mode als explizites Lieferergebnis (nur Vorbereitung).
- Keine Sprachwechsel (i18n bleibt wie gehabt, Default Deutsch).
- Keine Änderung am PDF-Report-Layout (Iteration 10) – PDF-Thymeleaf ist
  serverseitig und betrifft diese UI-Iteration nicht.

---

## 4 Aufgaben (empfohlene Reihenfolge in der Session)

1. **Technischer Audit** (Abschnitt 0 der Iteration, siehe Prompt-Start):
   Stand der View-Landschaft aufnehmen. **Erst nach Audit** mit Umbau
   beginnen.
2. **Frontend-Backend-Coverage-Audit gemäß Abschnitt 2.0**: Matrix
   erzeugen, Lücken zuweisen, Zeitbox setzen.
3. (A)-Lücken abarbeiten – sofortige Anbindungen (CVE-Browser,
   Komponenten-Browser, Profile-Liste, Report-Archiv, Waiver-Liste,
   Alert-Historie). Jeweils Basis-View, sauberer Commit, Eintrag in
   die Matrix auf `ANGEBUNDEN`.
4. (B)-Lücken mit `<cvm-page-placeholder>` versehen und in Matrix
   entsprechend markieren.
5. (C)-Bereiche aus der Navigation entfernen, Begründung in Matrix.
6. Token-Layer (`cvm-frontend/src/styles/tokens/`) implementieren.
7. Stylelint-Regeln und Guard-Checks in CI aktivieren (so früh wie möglich
   – ab jetzt wird hardkodierter Code abgelehnt).
8. `shared/ui/`-Komponenten bauen, mit Tests – darunter
   `<cvm-page-placeholder>` als eigenständige Komponente mit Prop für
   Iterations-Verweis.
9. `ThemeService` und Laufzeit-Token-Injektion.
10. Backend: `branding_config`, `branding_asset`, REST-Endpunkte,
    MIME-/SVG-/Font-Validierung, Audit.
11. Feature-Bereiche in der in 2.8 genannten Reihenfolge migrieren,
    inklusive der neu angebundenen Views aus Schritt 3.
    Pro Bereich: sauberer Commit, kurzer Vorher-/Nachher-Vergleich im
    Fortschrittsbericht.
12. Charts auf `ChartThemeService` umstellen.
13. Theme-Admin-UI (`/admin/theme`) mit Live-Vorschau.
14. Barrierefreiheits-Durchgang mit axe-core (Playwright).
15. `FullNavigationWalkThroughTest` grün bekommen – falls er bei einzelnen
    Routen rot ist, ist die zugehörige Matrix-Zeile unbearbeitet. Zurück
    zu Schritt 3 bis 5, bis alle Routen entweder Inhalt oder Platzhalter
    haben.
16. Abschluss: Screenshots aller Hauptseiten unter
    `docs/YYYYMMDD/iteration-23-after/` sammeln, Vorher-/Nachher-Tabelle
    im Fortschrittsbericht.

---

## 5 Test-Schwerpunkte

### Unit-Tests (Jasmine/Karma)
- `ThemeServiceTest`: Tokens werden gesetzt, Fehlkonfiguration fällt auf
  Default zurück, Kontrastverletzung wird abgelehnt.
- Je `Ahs*`-Komponente: Varianten, Fehlerzustände, Fokus-Verhalten.
- `ChartThemeServiceTest`: ECharts-Theme liest Tokens.

### Contract-Tests (Backend)
- `BrandingControllerTest`: GET/PUT/Upload, Validierung, Audit-Eintrag.
- `SvgSanitizerTest`: rejected SVGs mit `<script>`, `xlink:href` auf
  externe URLs, ausführbarem `onload`-Attribut.
- `FontUploadTest`: nur woff2, Größenlimit.
- `ContrastValidatorTest`: AA-Schwelle wird durchgesetzt.

### E2E-Tests (Playwright)
- Login → Shell erscheint mit Default-Theme, Logo sichtbar oben rechts.
- Theme-Admin: Farbe ändern → Speichern → andere geöffnete Seiten
  aktualisieren sich live (über Benachrichtigung/Service).
- Queue: alle Aktionen erreichbar ohne Maus.
- Eingabeformular (Profil-Editor): keine Überlappungen bei Viewport-
  Breiten 1280, 1440, 1920.
- axe-core-Lauf auf allen Hauptrouten, 0 Verstöße vom Level „serious"
  oder „critical".
- **`FullNavigationWalkThroughTest`**: Test iteriert über jede Route, die
  in der Sidebar-Navigation als anklickbarer Menüpunkt existiert. Für jede
  Route muss die geladene Seite entweder
  (a) mindestens ein Datenelement zeigen (Tabelle, Liste, Formular, Chart)
      oder
  (b) die `<cvm-page-placeholder>`-Komponente mit erkennbarem Hinweistext
      und Iterations-Referenz enthalten.
  Eine Seite ohne (a) und ohne (b) schlägt den Test fehl. Das ist die
  Sicherung dafür, dass keine stummen Lücken mehr existieren.

### Visuelle Regression (optional, Nice-to-have)
- Playwright-Screenshot-Vergleich für Shell, Dashboard, Queue, Profil,
  Theme-Admin. Baselines erstmalig erzeugen.

### Harte Invarianten
- Ein Test scannt das gebundelte CSS nach verbotenen Mustern (hex-Farben
  außerhalb Token-Datei, `font-family:` in Komponenten, `px`-Werte
  außerhalb Token-Datei) und schlägt bei Fund fehl. Das ist die
  Sicherung gegen Rückfall in den alten Zustand.
- **`FrontendBackendCoverageGate`**: Ein Skript parst
  `docs/YYYYMMDD/frontend-backend-coverage.md` und verifiziert, dass keine
  Zeile den Status `NAV_OHNE_INHALT` ohne zugewiesene Aktion (A/B/C)
  aufweist. Die Matrix wird dadurch zum verbindlichen Artefakt, nicht zur
  losen Dokumentation. Fehlt die Datei, schlägt der Test fehl.

**@DisplayName-Beispiele**:
- `@DisplayName("Theme: Primaerfarbe mit zu geringem Kontrast wird abgelehnt")`
- `@DisplayName("SVG-Sanitizer: eingebettetes Script-Element fuehrt zu Ablehnung")`
- `@DisplayName("ChartTheme: Severity-Farben stimmen mit CSS-Tokens ueberein")`

---

## 6 Definition of Done

- [ ] Audit-Dokument `iteration-23-ui-audit.md` liegt vor.
- [ ] **Frontend-Backend-Coverage-Matrix** `docs/YYYYMMDD/frontend-backend-coverage.md`
      liegt vor, vollständig für alle Prüfbereiche aus 2.0.2, jede
      Zeile mit einem der Status `ANGEBUNDEN` / `NAV_OHNE_INHALT` /
      `FEHLT_GANZ` / `BACKEND_FEHLT` und – bei allem außer `ANGEBUNDEN` –
      einer Aktion `(A)`, `(B)` oder `(C)` mit Begründung.
- [ ] **Kein Menüpunkt in der Sidebar führt auf eine komplett leere Seite**.
      Jede Route zeigt entweder echten Inhalt oder einen
      `<cvm-page-placeholder>` mit erkennbarer Iterations-Referenz.
- [ ] `FullNavigationWalkThroughTest` grün.
- [ ] `FrontendBackendCoverageGate` grün (keine stummen Lücken in der
      Matrix).
- [ ] Mindestens die folgenden bisher leeren Bereiche sind jetzt mit
      Basis-Listenansicht angebunden: **CVE-Browser, Komponenten-Browser,
      Profile-Liste, Report-Archiv, Waiver-Liste, Alert-Historie**
      (Priorität 1+2 aus 2.0.4).
- [ ] Token-Layer komplett, kein Komponenten-SCSS mit Rohwerten.
- [ ] `shared/ui/`-Kit vollständig, Tests grün, Storybook-/Previews vorhanden,
      inklusive `<cvm-page-placeholder>`-Komponente.
- [ ] Alle in 2.8 genannten Feature-Bereiche migriert – keine alte
      Komponente mehr im Produktivcode.
- [ ] Header oben rechts mit Logo, konfigurierbar, responsiv.
- [ ] `ThemeService` + Backend-Endpunkte + Admin-UI lauffähig.
- [ ] Live-Theme-Wechsel in der Angular-App ohne Reload funktioniert.
- [ ] Stylelint-Guard und CSS-Invarianten-Test schlagen bei Verstößen fehl.
- [ ] axe-core 0 Verstöße Level serious+critical auf Hauptseiten.
- [ ] Keine funktionalen Regressionen: alle vorherigen E2E-Tests der
      Iterationen 07, 08, 14, 18, 19, 21 weiterhin grün.
- [ ] Overlap-Problemstellen aus Audit nachweislich behoben (im
      Fortschrittsbericht je Stelle mit Vorher/Nachher-Verweis).
- [ ] Fortschrittsbericht `docs/YYYYMMDD/iteration-23-fortschritt.md` mit:
      Umbauliste, Vorher-/Nachher-Screenshots, Coverage-Matrix-Zusammenfassung,
      Liste abgelehnter Erweiterungsversuche, offene Punkte.
- [ ] Commit-Folge gemäß 2.8, jeder Commit conventional:
      `chore(audit): Frontend-Backend-Coverage-Matrix erstellt\n\nCVM-60`
      `feat(cve): Basis-Ansicht CVE-Browser\n\nCVM-60`
      `feat(components): Basis-Ansicht Komponenten-Browser\n\nCVM-60`
      `feat(profiles): Basis-Ansicht Profile-Liste\n\nCVM-60`
      `feat(placeholders): Platzhalter-Banner fuer offene Bereiche\n\nCVM-60`
      `refactor(ui): Token-Layer und shared/ui-Komponenten-Kit\n\nCVM-60`
      `refactor(queue): Queue-Ansicht auf shared/ui migriert\n\nCVM-60`
      …
      `feat(theme): Branding-API und Theme-Admin-UI ergaenzt\n\nCVM-60`

---

## 7 TDD-Hinweis

Die größte Fehlerquelle bei großflächigen UI-Umbauten ist stilles
Zurückrutschen in die alten Muster. Dagegen sichern dich drei harte Tests:

1. **CSS-Invarianten-Test** (Abschnitt 5) – hartes Gate gegen hardkodierte
   Werte.
2. **axe-core-Lauf** in Playwright – hartes Gate gegen
   Barrierefreiheits-Rückfälle.
3. **Funktionale E2E-Tests der vorherigen Iterationen** – hartes Gate
   gegen funktionale Regressionen.

**Ändere NICHT diese Tests**, wenn sie rot werden. Wenn ein Token neu
gebraucht wird, ergänze ihn im Token-Layer. Wenn eine Komponente fehlt,
ergänze sie im Kit. Wenn ein Kontrast nicht reicht, ändere die Vorgabe –
nicht den Test.

---

## 8 Abschlussreport

Unter `docs/YYYYMMDD/iteration-23-fortschritt.md`:

1. Was wurde umgebaut (Feature-Bereiche und Commits in Reihenfolge)
2. Welche Probleme aus dem Audit wurden wie gelöst (Tabelle:
   Problem → Root Cause → Fix → Stelle im Code)
3. Screenshots Vorher/Nachher der Haupt-Routen
4. Theming-Beispiel: zweiter Branding-Stand (z. B. fiktiver Mandant
   „BKK-Testfarbe") als JSON plus Screenshot
5. Welche Tokens sind aktuell erlaubt (Tabelle semantischer Namen), mit
   Angabe, welche Rohwerte aus Carbon v11 übernommen wurden und wo bewusst
   abgewichen wurde
6. Welche Komponenten existieren im Kit (Tabelle mit Zweck), mit Verweis
   auf das jeweilige Carbon-Referenzmuster (z. B. `AhsTable` → Carbon
   "Data Table")
7. Offene Punkte (aus Audit übrige Fundstellen, kleinere Polishing-Themen)
8. Empfehlung für Folge-Iteration: Dark-Mode scharfschalten? Weitere
   Theme-Presets ausliefern? Storybook als eigenes Projekt ziehen?

---

## 9 Was Sebastian von dieser Iteration erwarten darf

- Ein visuell konsistentes Portal, das im Produktivbetrieb nicht mehr als
  „Prototyp" wirkt.
- Ein einziger Ort, um Farben, Schriftart und Logo anzupassen
  (`/admin/theme`).
- Ein technischer Unterbau (Tokens + Kit), auf dem zukünftige Features
  ohne UI-Regression gebaut werden können.
- Keine Überraschungen in der Geschäftslogik – alle Funktionen verhalten
  sich exakt wie vorher.
- Eine Grundlage, um mandantenspezifisches Branding (BKK-Logos, andere
  Primärfarben) im Rollout aus Iteration 21 ohne Rebuild auszurollen.

---

**Stopp-Kriterien erinnern** (aus CLAUDE.md Abschnitt 10):
- Wenn während des Umbaus eine funktionale Regression auffällt, die ein
  bestehender Test nicht abdeckt: stopp, Test ergänzen, dann fixen.
- Wenn eine geforderte Theming-Option die Barrierefreiheit unterläuft
  (Kontrast, Motion): stopp, Rückfrage an Sebastian.
- Wenn der Umbau eines Feature-Bereichs Scope sprengen würde (Feature
  dort ist fachlich fehlerhaft, nicht nur stilistisch): Feature-Bereich
  zurückstellen, in `offene-punkte.md` vermerken, nächsten Bereich
  angehen.

Viel Erfolg.
