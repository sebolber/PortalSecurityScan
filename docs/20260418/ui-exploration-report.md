# UI-Exploration-Report

**Ziel**: `local` (http://localhost:4200)
**Stand**: 20260418
**User**: a.admin@ahs.test

## Zusammenfassung

| Verdict | Anzahl |
|---|---|
| INHALT | 0 |
| PLATZHALTER | 0 |
| LEER | 0 |
| FEHLER | 19 |
| NICHT_ERREICHBAR | 0 |

## Routen im Detail

### /dashboard (FEHLER)

- Rolle (Hinweis): `public` - Navigation: 200
- Screenshot: `ui-exploration/screenshots/dashboard.png`
- Notiz: Konsolen-Fehler oder HTTP 5xx in API-Antworten.
- DOM: main-children=2, table=false, form=false, chart=true, placeholder=false, empty-state=false, heading="Workflow"
- API-Calls:
  - GET /api/v1/alerts/banner -> 200
  - GET /api/v1/theme -> 200
- Konsole:
  - [warning] An iframe which has both allow-scripts and allow-same-origin for its sandbox attribute can escape its sandboxing.
  - [error] Access to XMLHttpRequest at 'http://localhost:8080/realms/cvm-local/account' from origin 'http://localhost:4200' has been blocked by CORS policy: No 'Access-Control-Allow-Origin' header is present on the requested resource.
  - [error] Failed to load resource: net::ERR_FAILED

### /queue (FEHLER)

- Rolle (Hinweis): `ASSESSOR` - Navigation: 200
- Screenshot: `ui-exploration/screenshots/queue.png`
- Notiz: Konsolen-Fehler oder HTTP 5xx in API-Antworten.
- DOM: main-children=2, table=true, form=false, chart=false, placeholder=false, empty-state=false, heading="Workflow"
- API-Calls:
  - GET /api/v1/alerts/banner -> 200
  - GET /api/v1/theme -> 200
- Konsole:
  - [warning] An iframe which has both allow-scripts and allow-same-origin for its sandbox attribute can escape its sandboxing.
  - [error] Access to XMLHttpRequest at 'http://localhost:8080/realms/cvm-local/account' from origin 'http://localhost:4200' has been blocked by CORS policy: No 'Access-Control-Allow-Origin' header is present on the requested resource.
  - [error] Failed to load resource: net::ERR_FAILED
  - [error] ERROR RuntimeError: NG0600: Writing to signals is not allowed in a `computed` or an `effect` by default. Use `allowSignalWrites` in the `CreateEffectOptions` to enable this inside effects.
    at http://localhost:4200/@fs/Users/olberding/Projects/portalsecurityscan/cvm-frontend/.angular/cache/18.2.21/cvm-frontend/vite/deps/chunk-TAKSCWT4.js?v=68a04003:20176:11
    at throwInvalidWriteToSignalError (http://localhost:4200/@fs/Users/olberding/Projects/portalsecurityscan/cvm-frontend/.angular/cache/18.2.21/cvm-frontend/vite/deps/chunk-TAKSCWT4.js?v=68a04003:267:3)
    at signalSetFn (http://localhost:4200/@fs/Users/olberding/Projects/portalsecurityscan/cvm-frontend/.angular/cache/18.2.21/cvm-frontend/vite/deps/chunk-TAKSCWT4.js?v=68a04003:285:5)
    at signalFn.set (http://localhost:4200/@fs/Users/olberding/Projects/portalsecurityscan/cvm-frontend/.angular/cache/18.2.21/cvm-frontend/vite/deps/chunk-TAKSCWT4.js?v=68a04003:12369:32)
    at _QueueStore.<anonymous> (http://localhost:4200/chunk-IRFZX6TV.js:182:23)
    at Generator.next (<anonymous>)
    at http://localhost:4200/chunk-Y5RQAIA6.js:37:61
    at new ZoneAwarePromise (http://localhost:4200/polyfills.js:2160:23)
    at __async (http://localhost:4200/chunk-Y5RQAIA6.js:21:10)
    at _QueueStore.reload (http://localhost:4200/chunk-IRFZX6TV.js:181:12)
  - [error] Access to XMLHttpRequest at 'http://localhost:8080/realms/cvm-local/account' from origin 'http://localhost:4200' has been blocked by CORS policy: No 'Access-Control-Allow-Origin' header is present on the requested resource.
  - ... (1 weitere)

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
  - [error] Access to XMLHttpRequest at 'http://localhost:8080/realms/cvm-local/account' from origin 'http://localhost:4200' has been blocked by CORS policy: No 'Access-Control-Allow-Origin' header is present on the requested resource.
  - [error] Failed to load resource: net::ERR_FAILED
  - [error] Access to XMLHttpRequest at 'http://localhost:8080/realms/cvm-local/account' from origin 'http://localhost:4200' has been blocked by CORS policy: No 'Access-Control-Allow-Origin' header is present on the requested resource.
  - [error] Failed to load resource: net::ERR_FAILED
  - ... (1 weitere)

### /components (FEHLER)

- Rolle (Hinweis): `VIEWER` - Navigation: 200
- Screenshot: `ui-exploration/screenshots/components.png`
- Notiz: Konsolen-Fehler oder HTTP 5xx in API-Antworten.
- DOM: main-children=2, table=true, form=false, chart=false, placeholder=false, empty-state=false, heading="Workflow"
- API-Calls:
  - GET /api/v1/alerts/banner -> 200
  - GET /api/v1/theme -> 200
  - GET /api/v1/products -> 200
  - GET /api/v1/products/2d6abe21-bef2-43de-9e6a-f5e29555c76a/versions -> 200
- Konsole:
  - [warning] An iframe which has both allow-scripts and allow-same-origin for its sandbox attribute can escape its sandboxing.
  - [error] Access to XMLHttpRequest at 'http://localhost:8080/realms/cvm-local/account' from origin 'http://localhost:4200' has been blocked by CORS policy: No 'Access-Control-Allow-Origin' header is present on the requested resource.
  - [error] Failed to load resource: net::ERR_FAILED
  - [error] Access to XMLHttpRequest at 'http://localhost:8080/realms/cvm-local/account' from origin 'http://localhost:4200' has been blocked by CORS policy: No 'Access-Control-Allow-Origin' header is present on the requested resource.
  - [error] Failed to load resource: net::ERR_FAILED

### /profiles (FEHLER)

- Rolle (Hinweis): `PROFILE_AUTHOR` - Navigation: 200
- Screenshot: `ui-exploration/screenshots/profiles.png`
- Notiz: Konsolen-Fehler oder HTTP 5xx in API-Antworten.
- DOM: main-children=2, table=false, form=false, chart=false, placeholder=false, empty-state=false, heading="Workflow"
- API-Calls:
  - GET /api/v1/alerts/banner -> 200
  - GET /api/v1/theme -> 200
  - GET /api/v1/environments -> 200
- Konsole:
  - [warning] An iframe which has both allow-scripts and allow-same-origin for its sandbox attribute can escape its sandboxing.
  - [error] Access to XMLHttpRequest at 'http://localhost:8080/realms/cvm-local/account' from origin 'http://localhost:4200' has been blocked by CORS policy: No 'Access-Control-Allow-Origin' header is present on the requested resource.
  - [error] Failed to load resource: net::ERR_FAILED
  - [error] Access to XMLHttpRequest at 'http://localhost:8080/realms/cvm-local/account' from origin 'http://localhost:4200' has been blocked by CORS policy: No 'Access-Control-Allow-Origin' header is present on the requested resource.
  - [error] Failed to load resource: net::ERR_FAILED

### /rules (FEHLER)

- Rolle (Hinweis): `RULE_AUTHOR` - Navigation: 200
- Screenshot: `ui-exploration/screenshots/rules.png`
- Notiz: Konsolen-Fehler oder HTTP 5xx in API-Antworten.
- DOM: main-children=2, table=false, form=false, chart=false, placeholder=false, empty-state=false, heading="Workflow"
- API-Calls:
  - GET /api/v1/alerts/banner -> 200
  - GET /api/v1/theme -> 200
  - GET /api/v1/rules -> 200
- Konsole:
  - [warning] An iframe which has both allow-scripts and allow-same-origin for its sandbox attribute can escape its sandboxing.
  - [error] Access to XMLHttpRequest at 'http://localhost:8080/realms/cvm-local/account' from origin 'http://localhost:4200' has been blocked by CORS policy: No 'Access-Control-Allow-Origin' header is present on the requested resource.
  - [error] Failed to load resource: net::ERR_FAILED
  - [error] Access to XMLHttpRequest at 'http://localhost:8080/realms/cvm-local/account' from origin 'http://localhost:4200' has been blocked by CORS policy: No 'Access-Control-Allow-Origin' header is present on the requested resource.
  - [error] Failed to load resource: net::ERR_FAILED

### /reports (FEHLER)

- Rolle (Hinweis): `REPORTER` - Navigation: 200
- Screenshot: `ui-exploration/screenshots/reports.png`
- Notiz: Konsolen-Fehler oder HTTP 5xx in API-Antworten.
- DOM: main-children=2, table=false, form=true, chart=false, placeholder=false, empty-state=false, heading="Workflow"
- API-Calls:
  - GET /api/v1/alerts/banner -> 200
  - GET /api/v1/theme -> 200
- Konsole:
  - [warning] An iframe which has both allow-scripts and allow-same-origin for its sandbox attribute can escape its sandboxing.
  - [error] Access to XMLHttpRequest at 'http://localhost:8080/realms/cvm-local/account' from origin 'http://localhost:4200' has been blocked by CORS policy: No 'Access-Control-Allow-Origin' header is present on the requested resource.
  - [error] Failed to load resource: net::ERR_FAILED
  - [error] Access to XMLHttpRequest at 'http://localhost:8080/realms/cvm-local/account' from origin 'http://localhost:4200' has been blocked by CORS policy: No 'Access-Control-Allow-Origin' header is present on the requested resource.
  - [error] Failed to load resource: net::ERR_FAILED

### /ai-audit (FEHLER)

- Rolle (Hinweis): `AI_AUDITOR` - Navigation: 200
- Screenshot: `ui-exploration/screenshots/ai-audit.png`
- Notiz: Konsolen-Fehler oder HTTP 5xx in API-Antworten.
- DOM: main-children=2, table=false, form=true, chart=false, placeholder=false, empty-state=false, heading="Workflow"
- API-Calls:
  - GET /api/v1/alerts/banner -> 200
  - GET /api/v1/theme -> 200
  - GET /api/v1/ai/audits?page=0&size=20 -> 200
- Konsole:
  - [warning] An iframe which has both allow-scripts and allow-same-origin for its sandbox attribute can escape its sandboxing.
  - [error] Access to XMLHttpRequest at 'http://localhost:8080/realms/cvm-local/account' from origin 'http://localhost:4200' has been blocked by CORS policy: No 'Access-Control-Allow-Origin' header is present on the requested resource.
  - [error] Failed to load resource: net::ERR_FAILED
  - [error] Access to XMLHttpRequest at 'http://localhost:8080/realms/cvm-local/account' from origin 'http://localhost:4200' has been blocked by CORS policy: No 'Access-Control-Allow-Origin' header is present on the requested resource.
  - [error] Failed to load resource: net::ERR_FAILED

### /settings (FEHLER)

- Rolle (Hinweis): `any` - Navigation: 200
- Screenshot: `ui-exploration/screenshots/settings.png`
- Notiz: Konsolen-Fehler oder HTTP 5xx in API-Antworten.
- DOM: main-children=2, table=false, form=true, chart=false, placeholder=false, empty-state=false, heading="Workflow"
- API-Calls:
  - GET /api/v1/alerts/banner -> 200
  - GET /api/v1/theme -> 200
  - GET /api/v1/environments -> 200
  - GET /api/v1/llm-model-profiles -> 200
- Konsole:
  - [warning] An iframe which has both allow-scripts and allow-same-origin for its sandbox attribute can escape its sandboxing.
  - [error] Access to XMLHttpRequest at 'http://localhost:8080/realms/cvm-local/account' from origin 'http://localhost:4200' has been blocked by CORS policy: No 'Access-Control-Allow-Origin' header is present on the requested resource.
  - [error] Failed to load resource: net::ERR_FAILED
  - [error] Access to XMLHttpRequest at 'http://localhost:8080/realms/cvm-local/account' from origin 'http://localhost:4200' has been blocked by CORS policy: No 'Access-Control-Allow-Origin' header is present on the requested resource.
  - [error] Failed to load resource: net::ERR_FAILED

### /admin/theme (FEHLER)

- Rolle (Hinweis): `ADMIN` - Navigation: 200
- Screenshot: `ui-exploration/screenshots/admin-theme.png`
- Notiz: Konsolen-Fehler oder HTTP 5xx in API-Antworten.
- DOM: main-children=2, table=false, form=true, chart=false, placeholder=false, empty-state=false, heading="Workflow"
- API-Calls:
  - GET /api/v1/alerts/banner -> 200
  - GET /api/v1/theme -> 200
  - GET /api/v1/theme -> 200
- Konsole:
  - [warning] An iframe which has both allow-scripts and allow-same-origin for its sandbox attribute can escape its sandboxing.
  - [error] Access to XMLHttpRequest at 'http://localhost:8080/realms/cvm-local/account' from origin 'http://localhost:4200' has been blocked by CORS policy: No 'Access-Control-Allow-Origin' header is present on the requested resource.
  - [error] Failed to load resource: net::ERR_FAILED
  - [error] Access to XMLHttpRequest at 'http://localhost:8080/realms/cvm-local/account' from origin 'http://localhost:4200' has been blocked by CORS policy: No 'Access-Control-Allow-Origin' header is present on the requested resource.
  - [error] Failed to load resource: net::ERR_FAILED

### /admin/products (FEHLER)

- Rolle (Hinweis): `ADMIN` - Navigation: 200
- Screenshot: `ui-exploration/screenshots/admin-products.png`
- Notiz: Konsolen-Fehler oder HTTP 5xx in API-Antworten.
- DOM: main-children=2, table=true, form=true, chart=false, placeholder=false, empty-state=false, heading="Workflow"
- API-Calls:
  - GET /api/v1/alerts/banner -> 200
  - GET /api/v1/theme -> 200
  - GET /api/v1/products -> 200
- Konsole:
  - [warning] An iframe which has both allow-scripts and allow-same-origin for its sandbox attribute can escape its sandboxing.
  - [error] Access to XMLHttpRequest at 'http://localhost:8080/realms/cvm-local/account' from origin 'http://localhost:4200' has been blocked by CORS policy: No 'Access-Control-Allow-Origin' header is present on the requested resource.
  - [error] Failed to load resource: net::ERR_FAILED
  - [error] Access to XMLHttpRequest at 'http://localhost:8080/realms/cvm-local/account' from origin 'http://localhost:4200' has been blocked by CORS policy: No 'Access-Control-Allow-Origin' header is present on the requested resource.
  - [error] Failed to load resource: net::ERR_FAILED

### /admin/environments (FEHLER)

- Rolle (Hinweis): `ADMIN` - Navigation: 200
- Screenshot: `ui-exploration/screenshots/admin-environments.png`
- Notiz: Konsolen-Fehler oder HTTP 5xx in API-Antworten.
- DOM: main-children=2, table=false, form=false, chart=false, placeholder=false, empty-state=true, heading="Workflow"
- API-Calls:
  - GET /api/v1/alerts/banner -> 200
  - GET /api/v1/theme -> 200
  - GET /api/v1/environments -> 200
- Konsole:
  - [warning] An iframe which has both allow-scripts and allow-same-origin for its sandbox attribute can escape its sandboxing.
  - [error] Access to XMLHttpRequest at 'http://localhost:8080/realms/cvm-local/account' from origin 'http://localhost:4200' has been blocked by CORS policy: No 'Access-Control-Allow-Origin' header is present on the requested resource.
  - [error] Failed to load resource: net::ERR_FAILED
  - [error] Access to XMLHttpRequest at 'http://localhost:8080/realms/cvm-local/account' from origin 'http://localhost:4200' has been blocked by CORS policy: No 'Access-Control-Allow-Origin' header is present on the requested resource.
  - [error] Failed to load resource: net::ERR_FAILED

### /scans/upload (FEHLER)

- Rolle (Hinweis): `ASSESSOR` - Navigation: 200
- Screenshot: `ui-exploration/screenshots/scans-upload.png`
- Notiz: Konsolen-Fehler oder HTTP 5xx in API-Antworten.
- DOM: main-children=2, table=false, form=true, chart=false, placeholder=false, empty-state=false, heading="Workflow"
- API-Calls:
  - GET /api/v1/alerts/banner -> 200
  - GET /api/v1/theme -> 200
  - GET /api/v1/products -> 200
  - GET /api/v1/environments -> 200
- Konsole:
  - [warning] An iframe which has both allow-scripts and allow-same-origin for its sandbox attribute can escape its sandboxing.
  - [error] Access to XMLHttpRequest at 'http://localhost:8080/realms/cvm-local/account' from origin 'http://localhost:4200' has been blocked by CORS policy: No 'Access-Control-Allow-Origin' header is present on the requested resource.
  - [error] Failed to load resource: net::ERR_FAILED
  - [error] Access to XMLHttpRequest at 'http://localhost:8080/realms/cvm-local/account' from origin 'http://localhost:4200' has been blocked by CORS policy: No 'Access-Control-Allow-Origin' header is present on the requested resource.
  - [error] Failed to load resource: net::ERR_FAILED

### /waivers (FEHLER)

- Rolle (Hinweis): `VIEWER` - Navigation: 200
- Screenshot: `ui-exploration/screenshots/waivers.png`
- Notiz: Konsolen-Fehler oder HTTP 5xx in API-Antworten.
- DOM: main-children=2, table=false, form=false, chart=false, placeholder=false, empty-state=true, heading="Workflow"
- API-Calls:
  - GET /api/v1/alerts/banner -> 200
  - GET /api/v1/theme -> 200
  - GET /api/v1/waivers?status=ACTIVE -> 200
- Konsole:
  - [warning] An iframe which has both allow-scripts and allow-same-origin for its sandbox attribute can escape its sandboxing.
  - [error] Access to XMLHttpRequest at 'http://localhost:8080/realms/cvm-local/account' from origin 'http://localhost:4200' has been blocked by CORS policy: No 'Access-Control-Allow-Origin' header is present on the requested resource.
  - [error] Failed to load resource: net::ERR_FAILED
  - [error] Access to XMLHttpRequest at 'http://localhost:8080/realms/cvm-local/account' from origin 'http://localhost:4200' has been blocked by CORS policy: No 'Access-Control-Allow-Origin' header is present on the requested resource.
  - [error] Failed to load resource: net::ERR_FAILED

### /alerts/history (FEHLER)

- Rolle (Hinweis): `VIEWER` - Navigation: 200
- Screenshot: `ui-exploration/screenshots/alerts-history.png`
- Notiz: Konsolen-Fehler oder HTTP 5xx in API-Antworten.
- DOM: main-children=2, table=false, form=false, chart=false, placeholder=false, empty-state=true, heading="Workflow"
- API-Calls:
  - GET /api/v1/alerts/banner -> 200
  - GET /api/v1/theme -> 200
  - GET /api/v1/alerts/history?limit=50 -> 200
- Konsole:
  - [warning] An iframe which has both allow-scripts and allow-same-origin for its sandbox attribute can escape its sandboxing.
  - [error] Access to XMLHttpRequest at 'http://localhost:8080/realms/cvm-local/account' from origin 'http://localhost:4200' has been blocked by CORS policy: No 'Access-Control-Allow-Origin' header is present on the requested resource.
  - [error] Failed to load resource: net::ERR_FAILED
  - [error] Access to XMLHttpRequest at 'http://localhost:8080/realms/cvm-local/account' from origin 'http://localhost:4200' has been blocked by CORS policy: No 'Access-Control-Allow-Origin' header is present on the requested resource.
  - [error] Failed to load resource: net::ERR_FAILED

### /reachability (FEHLER)

- Rolle (Hinweis): `ASSESSOR` - Navigation: 200
- Screenshot: `ui-exploration/screenshots/reachability.png`
- Notiz: Konsolen-Fehler oder HTTP 5xx in API-Antworten.
- DOM: main-children=2, table=false, form=false, chart=false, placeholder=false, empty-state=true, heading="Workflow"
- API-Calls:
  - GET /api/v1/alerts/banner -> 200
  - GET /api/v1/theme -> 200
  - GET /api/v1/reachability?limit=50 -> 200
- Konsole:
  - [warning] An iframe which has both allow-scripts and allow-same-origin for its sandbox attribute can escape its sandboxing.
  - [error] Access to XMLHttpRequest at 'http://localhost:8080/realms/cvm-local/account' from origin 'http://localhost:4200' has been blocked by CORS policy: No 'Access-Control-Allow-Origin' header is present on the requested resource.
  - [error] Failed to load resource: net::ERR_FAILED
  - [error] Access to XMLHttpRequest at 'http://localhost:8080/realms/cvm-local/account' from origin 'http://localhost:4200' has been blocked by CORS policy: No 'Access-Control-Allow-Origin' header is present on the requested resource.
  - [error] Failed to load resource: net::ERR_FAILED

### /fix-verification (FEHLER)

- Rolle (Hinweis): `ASSESSOR` - Navigation: 200
- Screenshot: `ui-exploration/screenshots/fix-verification.png`
- Notiz: Konsolen-Fehler oder HTTP 5xx in API-Antworten.
- DOM: main-children=2, table=false, form=false, chart=false, placeholder=false, empty-state=true, heading="Workflow"
- API-Calls:
  - GET /api/v1/alerts/banner -> 200
  - GET /api/v1/theme -> 200
  - GET /api/v1/fix-verification?limit=50 -> 200
- Konsole:
  - [warning] An iframe which has both allow-scripts and allow-same-origin for its sandbox attribute can escape its sandboxing.
  - [error] Access to XMLHttpRequest at 'http://localhost:8080/realms/cvm-local/account' from origin 'http://localhost:4200' has been blocked by CORS policy: No 'Access-Control-Allow-Origin' header is present on the requested resource.
  - [error] Failed to load resource: net::ERR_FAILED
  - [error] Access to XMLHttpRequest at 'http://localhost:8080/realms/cvm-local/account' from origin 'http://localhost:4200' has been blocked by CORS policy: No 'Access-Control-Allow-Origin' header is present on the requested resource.
  - [error] Failed to load resource: net::ERR_FAILED

### /anomaly (FEHLER)

- Rolle (Hinweis): `AI_AUDITOR` - Navigation: 200
- Screenshot: `ui-exploration/screenshots/anomaly.png`
- Notiz: Konsolen-Fehler oder HTTP 5xx in API-Antworten.
- DOM: main-children=2, table=false, form=false, chart=false, placeholder=false, empty-state=true, heading="Workflow"
- API-Calls:
  - GET /api/v1/alerts/banner -> 200
  - GET /api/v1/theme -> 200
  - GET /api/v1/anomalies?hours=24 -> 200
- Konsole:
  - [warning] An iframe which has both allow-scripts and allow-same-origin for its sandbox attribute can escape its sandboxing.
  - [error] Access to XMLHttpRequest at 'http://localhost:8080/realms/cvm-local/account' from origin 'http://localhost:4200' has been blocked by CORS policy: No 'Access-Control-Allow-Origin' header is present on the requested resource.
  - [error] Failed to load resource: net::ERR_FAILED
  - [error] Access to XMLHttpRequest at 'http://localhost:8080/realms/cvm-local/account' from origin 'http://localhost:4200' has been blocked by CORS policy: No 'Access-Control-Allow-Origin' header is present on the requested resource.
  - [error] Failed to load resource: net::ERR_FAILED

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
  - [error] Access to XMLHttpRequest at 'http://localhost:8080/realms/cvm-local/account' from origin 'http://localhost:4200' has been blocked by CORS policy: No 'Access-Control-Allow-Origin' header is present on the requested resource.
  - [error] Failed to load resource: net::ERR_FAILED
  - [error] Access to XMLHttpRequest at 'http://localhost:8080/realms/cvm-local/account' from origin 'http://localhost:4200' has been blocked by CORS policy: No 'Access-Control-Allow-Origin' header is present on the requested resource.
  - [error] Failed to load resource: net::ERR_FAILED
  - ... (2 weitere)
