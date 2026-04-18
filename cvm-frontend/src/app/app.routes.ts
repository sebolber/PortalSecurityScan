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
      }
    ]
  },
  { path: 'login-callback', component: LoginCallbackComponent },
  { path: '**', redirectTo: 'dashboard' }
];
