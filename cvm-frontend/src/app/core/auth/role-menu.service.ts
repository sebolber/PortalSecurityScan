import { Injectable } from '@angular/core';
import { CVM_ROLES, CvmRole } from './cvm-roles';

export interface MenuEntry {
  readonly id: string;
  readonly label: string;
  readonly path: string;
  readonly icon: string;
  readonly requiredRoles: readonly CvmRole[];
}

const MENU: readonly MenuEntry[] = [
  {
    id: 'dashboard',
    label: 'Dashboard',
    path: '/dashboard',
    icon: 'dashboard',
    requiredRoles: [
      CVM_ROLES.VIEWER,
      CVM_ROLES.ASSESSOR,
      CVM_ROLES.REVIEWER,
      CVM_ROLES.APPROVER,
      CVM_ROLES.REPORTER,
      CVM_ROLES.ADMIN
    ]
  },
  {
    id: 'queue',
    label: 'Bewertungs-Queue',
    path: '/queue',
    icon: 'rule',
    requiredRoles: [
      CVM_ROLES.ASSESSOR,
      CVM_ROLES.REVIEWER,
      CVM_ROLES.APPROVER,
      CVM_ROLES.ADMIN
    ]
  },
  {
    id: 'cves',
    label: 'CVEs',
    path: '/cves',
    icon: 'bug_report',
    requiredRoles: [
      CVM_ROLES.VIEWER,
      CVM_ROLES.ASSESSOR,
      CVM_ROLES.REVIEWER,
      CVM_ROLES.APPROVER,
      CVM_ROLES.ADMIN
    ]
  },
  {
    id: 'components',
    label: 'Komponenten',
    path: '/components',
    icon: 'inventory_2',
    requiredRoles: [
      CVM_ROLES.VIEWER,
      CVM_ROLES.REPORTER,
      CVM_ROLES.ADMIN
    ]
  },
  {
    id: 'profiles',
    label: 'Profile',
    path: '/profiles',
    icon: 'tune',
    requiredRoles: [
      CVM_ROLES.PROFILE_AUTHOR,
      CVM_ROLES.PROFILE_APPROVER,
      CVM_ROLES.ADMIN
    ]
  },
  {
    id: 'rules',
    label: 'Regeln',
    path: '/rules',
    icon: 'gavel',
    requiredRoles: [
      CVM_ROLES.RULE_AUTHOR,
      CVM_ROLES.RULE_APPROVER,
      CVM_ROLES.ADMIN
    ]
  },
  {
    id: 'reports',
    label: 'Berichte',
    path: '/reports',
    icon: 'description',
    requiredRoles: [
      CVM_ROLES.VIEWER,
      CVM_ROLES.REPORTER,
      CVM_ROLES.ADMIN
    ]
  },
  {
    id: 'ai-audit',
    label: 'KI-Audit',
    path: '/ai-audit',
    icon: 'fact_check',
    requiredRoles: [CVM_ROLES.AI_AUDITOR, CVM_ROLES.ADMIN]
  }
];

/**
 * Filtert das statische Menue gegen die Rollen des angemeldeten Users.
 * Wird von der Shell-Sidebar konsumiert. Pure Logik, keine Spring/RxJS-
 * Abhaengigkeiten -> einfach unit-testbar.
 */
@Injectable({ providedIn: 'root' })
export class RoleMenuService {
  visibleEntries(userRoles: readonly string[]): readonly MenuEntry[] {
    const set = new Set(userRoles);
    return MENU.filter((entry) => entry.requiredRoles.some((r) => set.has(r)));
  }

  allEntries(): readonly MenuEntry[] {
    return MENU;
  }

  hasAccess(path: string, userRoles: readonly string[]): boolean {
    const entry = MENU.find((m) => m.path === path);
    if (!entry) {
      return false;
    }
    const set = new Set(userRoles);
    return entry.requiredRoles.some((r) => set.has(r));
  }
}
