import { Routes } from '@angular/router';
import { ShellComponent } from './shell/shell.component';
import { LoginCallbackComponent } from './login-callback/login-callback.component';
import { authGuard } from './core/auth/auth.guard';
import { CVM_ROLES } from './core/auth/cvm-roles';

export const APP_ROUTES: Routes = [
  {
    path: '',
    component: ShellComponent,
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      {
        // Dashboard ist bewusst public, damit die Shell auch dann
        // sichtbar ist, wenn Keycloak nicht laeuft. Echte Daten kommen
        // erst nach Login (Bearer-Token via Interceptor).
        path: 'dashboard',
        loadComponent: () =>
          import('./features/dashboard/dashboard.component')
            .then((m) => m.DashboardComponent)
      },
      {
        path: 'queue',
        canActivate: [authGuard],
        data: {
          requiredRoles: [
            CVM_ROLES.ASSESSOR,
            CVM_ROLES.REVIEWER,
            CVM_ROLES.APPROVER,
            CVM_ROLES.ADMIN
          ]
        },
        loadComponent: () =>
          import('./features/queue/queue.component').then((m) => m.QueueComponent)
      },
      {
        path: 'cves',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/cves/cves.component').then((m) => m.CvesComponent)
      },
      {
        path: 'cves/:cveId',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/cve-detail/cve-detail.component')
            .then((m) => m.CveDetailComponent)
      },
      {
        path: 'components',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/components/components.component')
            .then((m) => m.ComponentsComponent)
      },
      {
        path: 'profiles',
        canActivate: [authGuard],
        data: {
          requiredRoles: [
            CVM_ROLES.PROFILE_AUTHOR,
            CVM_ROLES.PROFILE_APPROVER,
            CVM_ROLES.ADMIN
          ]
        },
        loadComponent: () =>
          import('./features/profiles/profiles.component')
            .then((m) => m.ProfilesComponent)
      },
      {
        path: 'rules',
        canActivate: [authGuard],
        data: {
          requiredRoles: [
            CVM_ROLES.RULE_AUTHOR,
            CVM_ROLES.RULE_APPROVER,
            CVM_ROLES.ADMIN
          ]
        },
        loadComponent: () =>
          import('./features/rules/rules.component').then((m) => m.RulesComponent)
      },
      {
        path: 'reports',
        canActivate: [authGuard],
        data: {
          requiredRoles: [
            CVM_ROLES.VIEWER,
            CVM_ROLES.REPORTER,
            CVM_ROLES.ADMIN
          ]
        },
        loadComponent: () =>
          import('./features/reports/reports.component')
            .then((m) => m.ReportsComponent)
      },
      {
        path: 'ai-audit',
        canActivate: [authGuard],
        data: { requiredRoles: [CVM_ROLES.AI_AUDITOR, CVM_ROLES.ADMIN] },
        loadComponent: () =>
          import('./features/ai-audit/ai-audit.component')
            .then((m) => m.AiAuditComponent)
      },
      {
        path: 'settings',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/settings/settings.component')
            .then((m) => m.SettingsComponent)
      },
      {
        path: 'admin/theme',
        canActivate: [authGuard],
        data: { requiredRoles: [CVM_ROLES.ADMIN] },
        loadComponent: () =>
          import('./features/admin-theme/admin-theme.component')
            .then((m) => m.AdminThemeComponent)
      },
      {
        path: 'admin/products',
        canActivate: [authGuard],
        data: { requiredRoles: [CVM_ROLES.ADMIN] },
        loadComponent: () =>
          import('./features/admin-products/admin-products.component')
            .then((m) => m.AdminProductsComponent)
      },
      {
        path: 'admin/environments',
        canActivate: [authGuard],
        data: { requiredRoles: [CVM_ROLES.ADMIN] },
        loadComponent: () =>
          import('./features/admin-environments/admin-environments.component')
            .then((m) => m.AdminEnvironmentsComponent)
      },
      {
        path: 'admin/llm-configurations',
        canActivate: [authGuard],
        data: { requiredRoles: [CVM_ROLES.ADMIN] },
        loadComponent: () =>
          import('./features/admin-llm-configurations/admin-llm-configurations.component')
            .then((m) => m.AdminLlmConfigurationsComponent)
      },
      {
        path: 'scans/upload',
        canActivate: [authGuard],
        data: {
          requiredRoles: [CVM_ROLES.ADMIN, CVM_ROLES.ASSESSOR]
        },
        loadComponent: () =>
          import('./features/scan-upload/scan-upload.component')
            .then((m) => m.ScanUploadComponent)
      },
      {
        path: 'waivers',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/waivers/waivers.component')
            .then((m) => m.WaiversComponent)
      },
      {
        path: 'alerts/history',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/alerts-history/alerts-history.component')
            .then((m) => m.AlertsHistoryComponent)
      },
      {
        path: 'reachability',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/reachability/reachability.component')
            .then((m) => m.ReachabilityComponent)
      },
      {
        path: 'fix-verification',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/fix-verification/fix-verification.component')
            .then((m) => m.FixVerificationComponent)
      },
      {
        path: 'anomaly',
        canActivate: [authGuard],
        data: { requiredRoles: [CVM_ROLES.AI_AUDITOR, CVM_ROLES.ADMIN] },
        loadComponent: () =>
          import('./features/anomaly/anomaly.component')
            .then((m) => m.AnomalyComponent)
      },
      {
        path: 'tenant-kpi',
        canActivate: [authGuard],
        data: { requiredRoles: [CVM_ROLES.ADMIN] },
        loadComponent: () =>
          import('./features/tenant-kpi/tenant-kpi.component')
            .then((m) => m.TenantKpiComponent)
      }
    ]
  },
  { path: 'login-callback', component: LoginCallbackComponent },
  { path: '**', redirectTo: 'dashboard' }
];
