# Iteration 32 – Plan

**Thema**: UI-Exploration-Findings (4 HIGH + 6 MEDIUM + 5 LOW) beheben
**Jira**: CVM-74
**Datum**: 2026-04-18

## Grundlage

`docs/20260418/ui-exploration-findings.md` (Iteration 31 nachgelagert).

## Reihenfolge

1. **HIGH-1 Material-Icons-Font** einbinden (CDN-Link im index.html).
2. **HIGH-2 Top-Nav-Overflow** entschaerfen (Rollen in User-Menue).
3. **HIGH-3 Keycloak-CORS** durch Verzicht auf `loadUserProfile()` und
   `webOrigins: "+"` in der Realm-Config.
4. **HIGH-4 NG0600** in `QueueComponent` mit
   `{ allowSignalWrites: true }`.
5. **MEDIUM-1** `.cvm-sev-chip` auf Severity-Token umstellen.
6. **MEDIUM-2** `/profiles` Empty-State + Link zu `/admin/environments`.
7. **MEDIUM-3+4** Admin-Theme: Text ehrlich (kein 24-h-Fenster mehr)
   + neue Card "Historie &amp; Rollback" mit `GET /admin/theme/history`
   und `POST /admin/theme/rollback/{version}`.
8. **MEDIUM-5** `/reports` lernt Dropdowns fuer Produkt-Version und
   Umgebung; Text-Fallback bleibt.
9. **MEDIUM-6** h1 auf `/settings`, `/rules`, `/components`, `/cves`
   bekommen `text-title-lg`.
10. **LOW-1** entfaellt (war Rendering-Artefakt durch fehlende Font -
    durch HIGH-1 erledigt).
11. **LOW-2** Akzentfarbe-Swatch mit Diagonal-Muster fuer leere Werte.
12. **LOW-3** Farbschema ist jetzt Segment-Control Hell/Dunkel.
13. **LOW-4** Severity-Quicktoggles bekommen farbigen Top-Border.
14. **LOW-5** `/rules` Empty-State erklaert, was eine Regel ist.

## Stopp-Kriterien

- ArchUnit / Backend-Tests muessen gruen bleiben.
- Frontend-Build muss durchlaufen.
