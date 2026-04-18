# Iteration 30 – Fortschritt

**Thema**: Severity-Farbentscheidung formell dokumentieren
**Jira**: CVM-71
**Datum**: 2026-04-18

## Umgesetzt

- Neues Dokument `docs/konzept/severity-farbentscheidung.md`:
  - Mapping Severity -> Token -> Hex pro Stufe
  - Begruendung je Severity (Carbon vs. adesso vs. Banner-Abgrenzung)
  - Kontrastnachweis (WCAG 2.1) mit Verhaeltnissen pro Paar
  - Aenderungspolitik inkl. Vier-Augen-Pflicht und
    `ContrastValidator`-Verdrahtung auf Branding-PUT
  - Referenzen auf Token-Datei, Badge-Komponente,
    `ChartThemeService` und adesso-Styleguide.

## Verifikation

- Reines Konzept-Dokument, keine Code-Aenderung.
- Die Hex-Werte wurden gegen `cvm-frontend/src/styles/tokens/colors.scss`
  abgeglichen.
- Kontrastverhaeltnisse sind ueber die WCAG-2.1-Luminanzformel
  berechnet; alle Paare bestehen AA, vier Paare sogar AAA.

## Offene Punkte

- Keine neuen. Offener Punkt "Severity-Farbentscheidung formell
  dokumentieren" aus den Iteration-27-Backlogs ist damit geschlossen.
