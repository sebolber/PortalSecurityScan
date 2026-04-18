/**
 * Route-Definition fuer die UI-Exploration (Iteration 23C, CVM-60).
 *
 * Enthaelt die 19 Sidebar-Routen aus `cvm-frontend/src/app/app.routes.ts`
 * (Stand 2026-04-18) plus den Settings-Einstieg. Die Settings-Rubriken
 * sind heute Tabs/Accordion innerhalb einer Komponente - die Exploration
 * steuert sie per `settingsSections`-Klick, nicht per Unter-Route.
 */

export interface ExploreRoute {
  /** Pfad OHNE fuehrenden Slash, wie in app.routes.ts notiert. */
  readonly path: string;
  /** Menschenlesbarer Slug fuer Screenshot-Dateinamen. */
  readonly slug: string;
  /** Rollen-Hinweis zur Dokumentation im Report. */
  readonly requiredRoleHint: string;
  /** Erwartetes DOM-Signal bei gesundem Zustand (optional). */
  readonly expectIndicator?:
    | 'table'
    | 'form'
    | 'chart'
    | 'placeholder'
    | 'content';
}

export const EXPLORE_ROUTES: readonly ExploreRoute[] = [
  { path: 'dashboard', slug: 'dashboard', requiredRoleHint: 'public', expectIndicator: 'chart' },
  { path: 'queue', slug: 'queue', requiredRoleHint: 'ASSESSOR', expectIndicator: 'table' },
  { path: 'cves', slug: 'cves', requiredRoleHint: 'VIEWER', expectIndicator: 'table' },
  { path: 'components', slug: 'components', requiredRoleHint: 'VIEWER', expectIndicator: 'content' },
  { path: 'profiles', slug: 'profiles', requiredRoleHint: 'PROFILE_AUTHOR', expectIndicator: 'content' },
  { path: 'rules', slug: 'rules', requiredRoleHint: 'RULE_AUTHOR', expectIndicator: 'table' },
  { path: 'reports', slug: 'reports', requiredRoleHint: 'REPORTER', expectIndicator: 'content' },
  { path: 'ai-audit', slug: 'ai-audit', requiredRoleHint: 'AI_AUDITOR', expectIndicator: 'table' },
  { path: 'settings', slug: 'settings', requiredRoleHint: 'any', expectIndicator: 'form' },
  { path: 'admin/theme', slug: 'admin-theme', requiredRoleHint: 'ADMIN', expectIndicator: 'form' },
  { path: 'admin/products', slug: 'admin-products', requiredRoleHint: 'ADMIN', expectIndicator: 'table' },
  { path: 'admin/environments', slug: 'admin-environments', requiredRoleHint: 'ADMIN', expectIndicator: 'table' },
  { path: 'scans/upload', slug: 'scans-upload', requiredRoleHint: 'ASSESSOR', expectIndicator: 'form' },
  { path: 'waivers', slug: 'waivers', requiredRoleHint: 'VIEWER', expectIndicator: 'table' },
  { path: 'alerts/history', slug: 'alerts-history', requiredRoleHint: 'VIEWER', expectIndicator: 'table' },
  { path: 'reachability', slug: 'reachability', requiredRoleHint: 'ASSESSOR', expectIndicator: 'table' },
  { path: 'fix-verification', slug: 'fix-verification', requiredRoleHint: 'ASSESSOR', expectIndicator: 'table' },
  { path: 'anomaly', slug: 'anomaly', requiredRoleHint: 'AI_AUDITOR', expectIndicator: 'table' },
  { path: 'tenant-kpi', slug: 'tenant-kpi', requiredRoleHint: 'ADMIN', expectIndicator: 'chart' }
];

/**
 * Settings-Tabs innerhalb von /settings. CSS-Selektoren oder Link-Texte,
 * die das Skript per Click ansteuert, um die Rubriken sichtbar zu machen.
 * Wenn ein Selektor nicht matcht, wird die Rubrik als
 * `NICHT_ERREICHBAR` markiert, nicht abgebrochen.
 */
export const SETTINGS_SECTIONS: readonly {
  slug: string;
  clickSelector: string;
  label: string;
}[] = [
  { slug: 'settings-user', clickSelector: 'text=Benutzer', label: 'Benutzer' },
  { slug: 'settings-llm', clickSelector: 'text=LLM', label: 'LLM-Profile' },
  { slug: 'settings-tenant', clickSelector: 'text=Mandant', label: 'Mandant' }
];
