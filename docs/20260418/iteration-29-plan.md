# Iteration 29 – Plan

**Thema**: Karma-Blocker `chart-theme.service.spec.ts` aufloesen
**Jira**: CVM-70
**Datum**: 2026-04-18

## Problem

`chart-theme.service.spec.ts` schlaegt mit TS4111 fehl, weil
`ChartThemeService.severityColors()` als `Record<string, string>`
typisiert ist und `noPropertyAccessFromIndexSignature: true` in der
`tsconfig.json` Punkt-Zugriff auf Index-Signatures verbietet.
Damit blockiert die Spec den gesamten Karma-Lauf.

Zweitens dokumentiert `docs/20260418/offene-punkte.md` den Fehler
als "pre-existent" - die Karma-Suite darf damit nicht weiter blockiert
bleiben, wenn sie in CI aktiviert wird.

## Loesung

Rueckgabetyp `Record<string, string>` ersetzen durch `Record<Severity,
string>`. Union-Typen fuehren bei TypeScript nicht zu einer
Index-Signature; Punkt-Zugriff ist erlaubt, und der dynamische
Zugriff `colors[e.severity]` im Dashboard bleibt gueltig, weil
`e.severity: Severity` typkompatibel ist.

## Arbeitsschritte (TDD)

1. Spec um vollstaendigen Severity-Zugriff erweitern (CRITICAL,
   HIGH, LOW, NOT_APPLICABLE via Punktzugriff), damit der Test den
   neuen Vertrag erfasst.
2. `ChartThemeService.severityColors` auf `Record<Severity, string>`
   ziehen; `Severity` aus dem bereits vorhandenen
   `severity-badge.component.ts` wiederverwenden.
3. `ng build`, `ng lint`, `./mvnw -T 1C test` gruen laufen lassen.

## Stopp-Kriterien

- `ng build` schlaegt fehl und laesst sich nach drei Versuchen
  nicht begruenden.
- ArchUnit-Regeln reagieren (nicht zu erwarten, reines Frontend).
