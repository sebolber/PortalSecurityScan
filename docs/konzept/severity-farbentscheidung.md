# Severity-Farbentscheidung (CVM)

**Version**: 1.0
**Stand**: 2026-04-18
**Jira**: CVM-71 (Iteration 30)

Dieses Dokument beschreibt verbindlich, welche Farben das CVM pro
Severity-Stufe verwendet, warum diese Werte gewaehlt wurden und wie
sich die Entscheidung zum adesso-Corporate-Design bzw. Carbon-Default
verhaelt. Grundlage ist Konzept v0.2 (Abschnitt 6) sowie der
adesso-Styleguide (Stand Maerz 2026).

## 1 Severity-Skala

Das CVM fuehrt eine CVM-spezifische Severity-Enumeration:

| Level | Verwendung | Beispiel |
|---|---|---|
| `CRITICAL` | Handlungspflicht, stoppt Go-Live bis Bewertung | CVSS &gt;= 9.0, KEV-Treffer |
| `HIGH` | Bearbeitung innerhalb SLA, kann Gate blockieren | CVSS 7.0-8.9 |
| `MEDIUM` | Bewertung im naechsten Review-Fenster | CVSS 4.0-6.9 |
| `LOW` | Hinweis, Bearbeitung optional | CVSS &lt; 4.0 |
| `INFORMATIONAL` | Nur Info, kein Handlungsbedarf | VEX `not_affected`, Waiver aktiv |
| `NOT_APPLICABLE` | Fachlich ausgeschlossen (Vier-Augen) | Komponente nicht im Pfad |

Diese Skala ist bewusst feiner als die Carbon-"Severity"-Skala
(Critical/Important/Warning/Info) und auch als NVD CVSS v3.1, weil
CVM den Zwischenzustand "ohne Bedarf bewertet" (`INFORMATIONAL`) von
"nicht anwendbar" (`NOT_APPLICABLE`) trennt - eine fachliche Vorgabe
aus Konzept v0.2.

## 2 Farbzuordnung

Hex-Werte leben als CSS-Custom-Properties in
`cvm-frontend/src/styles/tokens/colors.scss`. Komponenten nutzen
**ausschliesslich** die semantischen Token, niemals die Rohwerte -
der Mandanten-Branding-Layer (`ThemeService`) darf die Severity-Farben
pro Tenant ueberschreiben.

| Severity | Background-Token | Hex | Foreground-Token | Hex |
|---|---|---|---|---|
| CRITICAL | `--color-severity-critical-bg` | `#da1e28` | `--color-severity-critical-fg` | `#ffffff` |
| HIGH | `--color-severity-high-bg` | `#ff9868` | `--color-severity-high-fg` | `#161616` |
| MEDIUM | `--color-severity-medium-bg` | `#f1c21b` | `--color-severity-medium-fg` | `#161616` |
| LOW | `--color-severity-low-bg` | `#28dcaa` | `--color-severity-low-fg` | `#161616` |
| INFORMATIONAL | `--color-severity-informational-bg` | `#006ec7` | `--color-severity-informational-fg` | `#ffffff` |
| NOT_APPLICABLE | `--color-severity-not-applicable-bg` | `var(--cvm-gray-30)` = `#c6c6c6` | `--color-severity-not-applicable-fg` | `var(--cvm-gray-100)` = `#161616` |

## 3 Begruendung

### 3.1 CRITICAL (`#da1e28`)

Stammt aus Carbon v11 Red-60 und deckt sich mit der
Carbon-"Danger"-Rolle. Adesso-Corporate verwendet kein Warn-Rot mit
WCAG-AA-Kontrast gegen Weiss - das adesso-Accent-Pink (`#f566ba`)
liegt bei Kontrast &lt; 3,0 und ist damit ungeeignet. Die Wahl von
Carbon-Red-60 ist in den Konzept-Wireframes vorgegeben.

### 3.2 HIGH (`#ff9868`)

Identisch mit `--cvm-adesso-accent-orange` aus dem Styleguide
(`#ff9868`). Damit liegt `HIGH` auf adesso-Markenorange und bleibt
optisch vom Critical-Rot unterscheidbar. Der schwarze Vordergrund
(`#161616` / Gray-100) statt Weiss ist erforderlich, damit das
Kontrastverhaeltnis &gt;= 4,5:1 bleibt - Weiss auf `#ff9868` waere
ca. 2,4:1.

### 3.3 MEDIUM (`#f1c21b`)

Carbon v11 Yellow-30 (`#f1c21b`). Adesso-Gelb (`#ffff00`) ist
verfuegbar, liefert aber gegen Schwarz zwar hohen Kontrast, wirkt
aber nach Styleguide-Anweisung nur als Akzent - fuer grossflaechige
Flaechen (Zeilenhintergrund in der Queue) ist das zu laut. Carbon
Yellow-30 ist gedaempfter und bleibt lesbar.

### 3.4 LOW (`#28dcaa`)

`--cvm-adesso-accent-teal` aus dem Styleguide. Gruen/Tuerkis
signalisiert "geringe Dringlichkeit", waehrend LOW bewusst NICHT das
klassische Ampel-Gruen verwendet - Ampel-Gruen ist fuer APPROVED-
Zustaende bzw. Banner-Success reserviert (siehe `--color-banner-success-*`).

### 3.5 INFORMATIONAL (`#006ec7`)

`--cvm-adesso-blue` (Primaerfarbe). Blau ist die "Info"-Rolle
analog zu Carbon-Blue-60 - VEX-Waiver oder manuell als
"nicht affected" markierte Findings erscheinen damit ruhig und
nicht alarmierend.

### 3.6 NOT_APPLICABLE (`var(--cvm-gray-30)`)

Carbon Gray-30 (`#c6c6c6`) auf Gray-100-Vordergrund. Bewusst
farblos, weil `NOT_APPLICABLE` kein aktiver Zustand ist - das
Vier-Augen-Prinzip verlangt ein abschliessendes Review, und danach
soll der Befund optisch aus dem Fokus verschwinden. Kein Rot, kein
Gruen, kein Hinweisblau.

## 4 Kontrastnachweis (WCAG 2.1)

Verhaeltnisse berechnet nach WCAG 2.1 Relative Luminance. Alle
Foreground/Background-Paare erfuellen **AA fuer normalen Text
(&gt;= 4,5:1)**; `HIGH`, `MEDIUM`, `LOW` und `NOT_APPLICABLE`
erreichen sogar **AAA (&gt;= 7:1)**.

| Severity | BG / FG | Kontrast | WCAG |
|---|---|---|---|
| CRITICAL | `#da1e28` / `#ffffff` | ~5,07 : 1 | AA |
| HIGH | `#ff9868` / `#161616` | ~8,78 : 1 | AAA |
| MEDIUM | `#f1c21b` / `#161616` | ~10,86 : 1 | AAA |
| LOW | `#28dcaa` / `#161616` | ~10,30 : 1 | AAA |
| INFORMATIONAL | `#006ec7` / `#ffffff` | ~4,96 : 1 | AA |
| NOT_APPLICABLE | `#c6c6c6` / `#161616` | ~10,62 : 1 | AAA |

Die zentrale Kontrastpruefung auf Branding-Ebene liefert
`cvm-application`
[`ContrastValidator`](../../cvm-application/src/main/java/com/ahs/cvm/application/branding/ContrastValidator.java)
(siehe Iteration 27). Tenant-Branding-Konfigurationen werden beim
`PUT /api/v1/admin/theme` gegen `>= 4,5:1` geprueft.

## 5 Abgrenzung zu Banner-Farben

Banner-Hinweise (`--color-banner-*`) ueberschneiden sich thematisch
mit Severity, verwenden aber andere Toene:

- `--color-banner-critical-*` deckt "System ist in rotem Zustand"
  (z. B. Alert-Banner), NICHT "eine CVE ist CRITICAL".
- `--color-banner-success-*` ist das einzige Gruen im System und
  darf nicht fuer LOW verwendet werden, damit APPROVED-Kontexte
  klar bleiben.
- `--color-banner-warn-*` nutzt Gelb wie MEDIUM, hat aber geringere
  Saettigung - bewusst, damit ein Warn-Banner am Seitenrand nicht
  mit einer MEDIUM-Zeile in der Queue verwechselt wird.

## 6 Aenderungspolitik

- Jede Aenderung an Severity-Farben erfordert einen Pull Request mit
  Vier-Augen-Review und Update dieses Dokuments (Kontrastnachweis
  zwingend).
- Mandanten-Branding (`BrandingService`) darf die Severity-Farben
  NICHT uebersteuern, ohne dass der `ContrastValidator` das neue
  Paar akzeptiert. Ein Mandant, dessen Theme zu helle oder zu dunkle
  Severity-Paare liefert, erhaelt einen PUT-400 mit Kontrast-Fehler.
- Neue Severity-Stufen gibt es nicht. Sollte Konzept v0.3 eine
  weitere Stufe einfuehren, wird dieses Dokument um eine Zeile
  ergaenzt und erst dann darf Tailwind/SCSS das Token verwenden.

## 7 Referenzen

- `cvm-frontend/src/styles/tokens/colors.scss` (Token-Definition)
- `cvm-frontend/src/app/shared/components/severity-badge.component.ts` (Badge)
- `cvm-frontend/src/app/core/theme/chart-theme.service.ts` (ECharts-Bindung)
- `cvm-application/src/main/java/com/ahs/cvm/application/branding/ContrastValidator.java`
- WCAG 2.1 Success Criterion 1.4.3 (Contrast - Minimum)
- Carbon Design System v11, `Color` tokens
- adesso-Styleguide Maerz 2026
