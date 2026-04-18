# UI-Exploration (CVM-60)

Playwright-basiertes Diagnose-Skript, das stumme oder funktionsarme
Oberflaechen-Bereiche sichtbar macht. Lesegelenktes Gegenstueck zu
`scripts/audit/list-endpoints.sh` und `list-frontend-calls.sh`.

Details und Pflicht-Ablauf siehe
[`docs/prompts/ui-exploration.md`](../../docs/prompts/ui-exploration.md).

## Lokaler Lauf

Voraussetzungen:

- Docker-Stack hoch: `docker compose up -d`
- Backend laeuft auf `http://localhost:8081`
- Frontend laeuft auf `http://localhost:4200`
- Keycloak-Realm `cvm-local` importiert, Admin-User
  `a.admin@ahs.test / admin`.

```bash
cd scripts/explore-ui
npm install
npm run install-browsers     # Chromium-Binary fuer Playwright
export CVM_TEST_ADMIN_PASS=admin
npm run explore -- --target=local
```

Ausgaben:

- `docs/YYYYMMDD/ui-exploration-report.md` (menschenlesbar)
- `docs/YYYYMMDD/ui-exploration.json` (maschinenlesbar)
- `docs/YYYYMMDD/ui-exploration/screenshots/*.png`

## CI-Lauf

`.github/workflows/ui-exploration.yml` fuehrt denselben Befehl mit
`--target=ci` aus. Artefakte werden 30 Tage aufbewahrt.

## Verdicts

| Code | Bedeutung |
|---|---|
| `INHALT` | Route hat Tabelle/Form/Chart oder Leerzustand mit API-Call |
| `PLATZHALTER` | `<cvm-page-placeholder>` gefunden |
| `LEER` | Kein API-Call, fast leerer Main-Content |
| `FEHLER` | Konsolen-Error oder 4xx/5xx-API-Response |
| `NICHT_ERREICHBAR` | Navigation schlug fehl (Timeout, Auth, ...) |

Die Heuristik ist konservativ: lieber faelschlich `LEER` markieren
als ein Problem uebersehen. Die finale Bewertung macht der Leser
(Mensch oder Claude in spaeteren Sessions).
