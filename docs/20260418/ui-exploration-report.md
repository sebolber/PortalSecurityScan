# UI-Exploration-Report

**Ziel**: `local` (http://localhost:4200)
**Stand**: 20260418
**User**: a.admin@ahs.test

## Zusammenfassung

| Verdict | Anzahl |
|---|---|
| INHALT | 17 |
| PLATZHALTER | 0 |
| LEER | 0 |
| FEHLER | 2 |
| NICHT_ERREICHBAR | 0 |

## Routen im Detail

### /dashboard (INHALT)

- Rolle (Hinweis): `public` - Navigation: 200
- Screenshot: `ui-exploration/screenshots/dashboard.png`
- DOM: main-children=2, table=false, form=false, chart=true, placeholder=false, empty-state=false, heading="Workflow"
- API-Calls:
  - GET /api/v1/alerts/banner -> 200
  - GET /api/v1/theme -> 200
- Konsole:
  - [warning] An iframe which has both allow-scripts and allow-same-origin for its sandbox attribute can escape its sandboxing.

### /queue (INHALT)

- Rolle (Hinweis): `ASSESSOR` - Navigation: 200
- Screenshot: `ui-exploration/screenshots/queue.png`
- DOM: main-children=2, table=true, form=false, chart=false, placeholder=false, empty-state=false, heading="Workflow"
- API-Calls:
  - GET /api/v1/alerts/banner -> 200
  - GET /api/v1/theme -> 200
  - GET /api/v1/findings -> 200
- Konsole:
  - [warning] An iframe which has both allow-scripts and allow-same-origin for its sandbox attribute can escape its sandboxing.

### /cves (FEHLER)

- Rolle (Hinweis): `VIEWER` - Navigation: 200
- Screenshot: `ui-exploration/screenshots/cves.png`
- Notiz: Konsolen-Fehler oder HTTP 5xx in API-Antworten.
- DOM: main-children=2, table=true, form=true, chart=false, placeholder=false, empty-state=false, heading="Workflow"
- API-Calls:
  - GET /api/v1/alerts/banner -> 200
  - GET /api/v1/theme -> 200
  - GET /api/v1/cves?kev=false&page=0&size=25 -> 200
- Konsole:
  - [warning] An iframe which has both allow-scripts and allow-same-origin for its sandbox attribute can escape its sandboxing.
  - [error] NG0303: Can't bind to 'matRowDefTrackBy' since it isn't a known property of 'tr' (used in the '_CvesComponent' component template).
1. If 'tr' is an Angular component and it has the 'matRowDefTrackBy' input, then verify that it is included in the '@Component.imports' of this component.
2. To allow any property add 'NO_ERRORS_SCHEMA' to the '@Component.schemas' of this component.

### /components (INHALT)

- Rolle (Hinweis): `VIEWER` - Navigation: 200
- Screenshot: `ui-exploration/screenshots/components.png`
- DOM: main-children=2, table=true, form=false, chart=false, placeholder=false, empty-state=false, heading="Workflow"
- API-Calls:
  - GET /api/v1/alerts/banner -> 200
  - GET /api/v1/theme -> 200
  - GET /api/v1/products -> 200
  - GET /api/v1/products/2d6abe21-bef2-43de-9e6a-f5e29555c76a/versions -> 200
- Konsole:
  - [warning] An iframe which has both allow-scripts and allow-same-origin for its sandbox attribute can escape its sandboxing.

### /profiles (INHALT)

- Rolle (Hinweis): `PROFILE_AUTHOR` - Navigation: 200
- Screenshot: `ui-exploration/screenshots/profiles.png`
- DOM: main-children=2, table=false, form=false, chart=false, placeholder=false, empty-state=false, heading="Workflow"
- API-Calls:
  - GET /api/v1/alerts/banner -> 200
  - GET /api/v1/theme -> 200
  - GET /api/v1/environments -> 200
- Konsole:
  - [warning] An iframe which has both allow-scripts and allow-same-origin for its sandbox attribute can escape its sandboxing.

### /rules (INHALT)

- Rolle (Hinweis): `RULE_AUTHOR` - Navigation: 200
- Screenshot: `ui-exploration/screenshots/rules.png`
- DOM: main-children=2, table=false, form=false, chart=false, placeholder=false, empty-state=false, heading="Workflow"
- API-Calls:
  - GET /api/v1/alerts/banner -> 200
  - GET /api/v1/theme -> 200
  - GET /api/v1/rules -> 200
- Konsole:
  - [warning] An iframe which has both allow-scripts and allow-same-origin for its sandbox attribute can escape its sandboxing.

### /reports (INHALT)

- Rolle (Hinweis): `REPORTER` - Navigation: 200
- Screenshot: `ui-exploration/screenshots/reports.png`
- DOM: main-children=2, table=false, form=true, chart=false, placeholder=false, empty-state=false, heading="Workflow"
- API-Calls:
  - GET /api/v1/alerts/banner -> 200
  - GET /api/v1/theme -> 200
  - GET /api/v1/products -> 200
  - GET /api/v1/environments -> 200
  - GET /api/v1/products/2d6abe21-bef2-43de-9e6a-f5e29555c76a/versions -> 200
- Konsole:
  - [warning] An iframe which has both allow-scripts and allow-same-origin for its sandbox attribute can escape its sandboxing.

### /ai-audit (INHALT)

- Rolle (Hinweis): `AI_AUDITOR` - Navigation: 200
- Screenshot: `ui-exploration/screenshots/ai-audit.png`
- DOM: main-children=2, table=false, form=true, chart=false, placeholder=false, empty-state=false, heading="Workflow"
- API-Calls:
  - GET /api/v1/alerts/banner -> 200
  - GET /api/v1/theme -> 200
  - GET /api/v1/ai/audits?page=0&size=20 -> 200
- Konsole:
  - [warning] An iframe which has both allow-scripts and allow-same-origin for its sandbox attribute can escape its sandboxing.

### /settings (INHALT)

- Rolle (Hinweis): `any` - Navigation: 200
- Screenshot: `ui-exploration/screenshots/settings.png`
- DOM: main-children=2, table=false, form=true, chart=false, placeholder=false, empty-state=false, heading="Workflow"
- API-Calls:
  - GET /api/v1/alerts/banner -> 200
  - GET /api/v1/theme -> 200
  - GET /api/v1/environments -> 200
  - GET /api/v1/llm-model-profiles -> 200
- Konsole:
  - [warning] An iframe which has both allow-scripts and allow-same-origin for its sandbox attribute can escape its sandboxing.

### /admin/theme (INHALT)

- Rolle (Hinweis): `ADMIN` - Navigation: 200
- Screenshot: `ui-exploration/screenshots/admin-theme.png`
- DOM: main-children=2, table=false, form=true, chart=false, placeholder=false, empty-state=false, heading="Workflow"
- API-Calls:
  - GET /api/v1/alerts/banner -> 200
  - GET /api/v1/theme -> 200
  - GET /api/v1/theme -> 200
  - GET /api/v1/admin/theme/history?limit=20 -> 200
- Konsole:
  - [warning] An iframe which has both allow-scripts and allow-same-origin for its sandbox attribute can escape its sandboxing.

### /admin/products (INHALT)

- Rolle (Hinweis): `ADMIN` - Navigation: 200
- Screenshot: `ui-exploration/screenshots/admin-products.png`
- DOM: main-children=2, table=true, form=true, chart=false, placeholder=false, empty-state=false, heading="Workflow"
- API-Calls:
  - GET /api/v1/alerts/banner -> 200
  - GET /api/v1/theme -> 200
  - GET /api/v1/products -> 200
- Konsole:
  - [warning] An iframe which has both allow-scripts and allow-same-origin for its sandbox attribute can escape its sandboxing.

### /admin/environments (INHALT)

- Rolle (Hinweis): `ADMIN` - Navigation: 200
- Screenshot: `ui-exploration/screenshots/admin-environments.png`
- Notiz: Leerzustand mit echtem API-Call (Backend liefert leere Liste).
- DOM: main-children=2, table=false, form=false, chart=false, placeholder=false, empty-state=true, heading="Workflow"
- API-Calls:
  - GET /api/v1/alerts/banner -> 200
  - GET /api/v1/theme -> 200
  - GET /api/v1/environments -> 200
- Konsole:
  - [warning] An iframe which has both allow-scripts and allow-same-origin for its sandbox attribute can escape its sandboxing.

### /scans/upload (INHALT)

- Rolle (Hinweis): `ASSESSOR` - Navigation: 200
- Screenshot: `ui-exploration/screenshots/scans-upload.png`
- DOM: main-children=2, table=false, form=true, chart=false, placeholder=false, empty-state=false, heading="Workflow"
- API-Calls:
  - GET /api/v1/alerts/banner -> 200
  - GET /api/v1/theme -> 200
  - GET /api/v1/products -> 200
  - GET /api/v1/environments -> 200
- Konsole:
  - [warning] An iframe which has both allow-scripts and allow-same-origin for its sandbox attribute can escape its sandboxing.

### /waivers (INHALT)

- Rolle (Hinweis): `VIEWER` - Navigation: 200
- Screenshot: `ui-exploration/screenshots/waivers.png`
- Notiz: Leerzustand mit echtem API-Call (Backend liefert leere Liste).
- DOM: main-children=2, table=false, form=false, chart=false, placeholder=false, empty-state=true, heading="Workflow"
- API-Calls:
  - GET /api/v1/alerts/banner -> 200
  - GET /api/v1/theme -> 200
  - GET /api/v1/waivers?status=ACTIVE -> 200
- Konsole:
  - [warning] An iframe which has both allow-scripts and allow-same-origin for its sandbox attribute can escape its sandboxing.

### /alerts/history (INHALT)

- Rolle (Hinweis): `VIEWER` - Navigation: 200
- Screenshot: `ui-exploration/screenshots/alerts-history.png`
- Notiz: Leerzustand mit echtem API-Call (Backend liefert leere Liste).
- DOM: main-children=2, table=false, form=false, chart=false, placeholder=false, empty-state=true, heading="Workflow"
- API-Calls:
  - GET /api/v1/alerts/banner -> 200
  - GET /api/v1/theme -> 200
  - GET /api/v1/alerts/history?limit=50 -> 200
- Konsole:
  - [warning] An iframe which has both allow-scripts and allow-same-origin for its sandbox attribute can escape its sandboxing.

### /reachability (INHALT)

- Rolle (Hinweis): `ASSESSOR` - Navigation: 200
- Screenshot: `ui-exploration/screenshots/reachability.png`
- Notiz: Leerzustand mit echtem API-Call (Backend liefert leere Liste).
- DOM: main-children=2, table=false, form=false, chart=false, placeholder=false, empty-state=true, heading="Workflow"
- API-Calls:
  - GET /api/v1/alerts/banner -> 200
  - GET /api/v1/theme -> 200
  - GET /api/v1/reachability?limit=50 -> 200
- Konsole:
  - [warning] An iframe which has both allow-scripts and allow-same-origin for its sandbox attribute can escape its sandboxing.

### /fix-verification (INHALT)

- Rolle (Hinweis): `ASSESSOR` - Navigation: 200
- Screenshot: `ui-exploration/screenshots/fix-verification.png`
- Notiz: Leerzustand mit echtem API-Call (Backend liefert leere Liste).
- DOM: main-children=2, table=false, form=false, chart=false, placeholder=false, empty-state=true, heading="Workflow"
- API-Calls:
  - GET /api/v1/alerts/banner -> 200
  - GET /api/v1/theme -> 200
  - GET /api/v1/fix-verification?limit=50 -> 200
- Konsole:
  - [warning] An iframe which has both allow-scripts and allow-same-origin for its sandbox attribute can escape its sandboxing.

### /anomaly (INHALT)

- Rolle (Hinweis): `AI_AUDITOR` - Navigation: 200
- Screenshot: `ui-exploration/screenshots/anomaly.png`
- Notiz: Leerzustand mit echtem API-Call (Backend liefert leere Liste).
- DOM: main-children=2, table=false, form=false, chart=false, placeholder=false, empty-state=true, heading="Workflow"
- API-Calls:
  - GET /api/v1/alerts/banner -> 200
  - GET /api/v1/theme -> 200
  - GET /api/v1/anomalies?hours=24 -> 200
- Konsole:
  - [warning] An iframe which has both allow-scripts and allow-same-origin for its sandbox attribute can escape its sandboxing.

### /tenant-kpi (FEHLER)

- Rolle (Hinweis): `ADMIN` - Navigation: 200
- Screenshot: `ui-exploration/screenshots/tenant-kpi.png`
- Notiz: Konsolen-Fehler oder HTTP 5xx in API-Antworten.
- DOM: main-children=2, table=true, form=false, chart=true, placeholder=false, empty-state=false, heading="Workflow"
- API-Calls:
  - GET /api/v1/alerts/banner -> 200
  - GET /api/v1/theme -> 200
  - GET /api/v1/kpis?window=90d -> 200
- Konsole:
  - [warning] An iframe which has both allow-scripts and allow-same-origin for its sandbox attribute can escape its sandboxing.
  - [error] [ECharts] Component grid is used but not imported.
import { GridComponent } from 'echarts/components';
echarts.use([GridComponent]);
  - [error] [ECharts] Series line is used but not imported.
import { LineChart } from 'echarts/charts';
echarts.use([LineChart]);
