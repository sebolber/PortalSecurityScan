# Iteration 30 – Plan

**Thema**: Severity-Farbentscheidung formell dokumentieren
**Jira**: CVM-71
**Datum**: 2026-04-18

## Hintergrund

Die Severity-Skala (`CRITICAL` / `HIGH` / `MEDIUM` / `LOW` /
`INFORMATIONAL` / `NOT_APPLICABLE`) wird im CVM ueber dedizierte
CSS-Token (`--color-severity-*-bg/-fg`) gefuehrt. Die Token-Werte
weichen bewusst von der Carbon-Default-Severity-Skala und auch vom
adesso-Corporate-Styleguide ab, weil beide keine gemeinsame
WCAG-AA-Entscheidung liefern.

`docs/20260418/offene-punkte.md` (Stand nach Iteration 27) nennt
"Severity-Farbentscheidung formell dokumentieren" als offenen Punkt -
damit ein Audit nachvollziehen kann, warum die Skala so gewaehlt ist.

## Arbeitsschritte

1. `docs/konzept/severity-farbentscheidung.md` anlegen. Inhalt:
   - Ampel-Mapping CVM-Severity → Farbrolle → Hex-Wert
   - Begruendung (WCAG-AA, adesso-Bezug, Kollision mit
     Banner-Farben)
   - Kontrastverhaeltnisse fuer jeden Foreground-Background-Paar
     (konkrete Zahlen, damit die Entscheidung ueberpruefbar ist)
   - Beziehung zur Token-Datei
     `cvm-frontend/src/styles/tokens/colors.scss`
   - Abgrenzung zu `--color-banner-*`
2. `CLAUDE.md`-Verweise pruefen (keine Aenderung noetig, das Dokument
   haengt unter `docs/konzept/`).
3. `docs/20260418/offene-punkte.md` aktualisieren.

## Stopp-Kriterien

Keine - reine Dokumentation.
