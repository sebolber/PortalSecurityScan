import { Injectable } from '@angular/core';
import { CVM_ROLES, CvmRole } from './cvm-roles';

export type MenuSection = 'workflow' | 'uebersicht' | 'einstellungen';

export interface MenuEntry {
  readonly id: string;
  readonly label: string;
  readonly path: string;
  readonly icon: string;
  readonly requiredRoles: readonly CvmRole[];
  /**
   * Fachliche Gruppierung in der Sidebar:
   * - `workflow` (Einstieg Scan - Queue - Analyse - Waiver - Report)
   * - `uebersicht` (Read-Only-Sichten)
   * - `einstellungen` (Hauptpunkt + Unterpunkte fuer Konfiguration)
   */
  readonly section: MenuSection;
  /**
   * Kinder-Eintraege fuer Gruppen unter "Einstellungen" (Profile,
   * Regeln, Produkte, Umgebungen, Theme).
   */
  readonly children?: readonly MenuEntry[];
}

const PROFILES_ENTRY: MenuEntry = {
  id: 'profiles',
  label: 'Profile',
  path: '/profiles',
  icon: 'tune',
  section: 'einstellungen',
  requiredRoles: [
    CVM_ROLES.PROFILE_AUTHOR,
    CVM_ROLES.PROFILE_APPROVER,
    CVM_ROLES.ADMIN
  ]
};

const RULES_ENTRY: MenuEntry = {
  id: 'rules',
  label: 'Regeln',
  path: '/rules',
  icon: 'gavel',
  section: 'einstellungen',
  requiredRoles: [
    CVM_ROLES.RULE_AUTHOR,
    CVM_ROLES.RULE_APPROVER,
    CVM_ROLES.ADMIN
  ]
};

const ADMIN_PRODUCTS_ENTRY: MenuEntry = {
  id: 'admin-products',
  label: 'Produkte',
  path: '/admin/products',
  icon: 'category',
  section: 'einstellungen',
  requiredRoles: [CVM_ROLES.ADMIN]
};

const ADMIN_ENVIRONMENTS_ENTRY: MenuEntry = {
  id: 'admin-environments',
  label: 'Umgebungen',
  path: '/admin/environments',
  icon: 'layers',
  section: 'einstellungen',
  requiredRoles: [CVM_ROLES.ADMIN]
};

const ADMIN_THEME_ENTRY: MenuEntry = {
  id: 'admin-theme',
  label: 'Theme & Branding',
  path: '/admin/theme',
  icon: 'palette',
  section: 'einstellungen',
  requiredRoles: [CVM_ROLES.ADMIN]
};

const ADMIN_LLM_ENTRY: MenuEntry = {
  id: 'admin-llm-configurations',
  label: 'LLM-Konfigurationen',
  path: '/admin/llm-configurations',
  icon: 'smart_toy',
  section: 'einstellungen',
  requiredRoles: [CVM_ROLES.ADMIN]
};

const SETTINGS_ENTRY: MenuEntry = {
  id: 'settings',
  label: 'Einstellungen',
  path: '/settings',
  icon: 'settings',
  section: 'einstellungen',
  requiredRoles: [
    CVM_ROLES.VIEWER,
    CVM_ROLES.ASSESSOR,
    CVM_ROLES.REVIEWER,
    CVM_ROLES.APPROVER,
    CVM_ROLES.PROFILE_AUTHOR,
    CVM_ROLES.PROFILE_APPROVER,
    CVM_ROLES.RULE_AUTHOR,
    CVM_ROLES.RULE_APPROVER,
    CVM_ROLES.REPORTER,
    CVM_ROLES.AI_AUDITOR,
    CVM_ROLES.ADMIN
  ],
  children: [
    PROFILES_ENTRY,
    RULES_ENTRY,
    ADMIN_PRODUCTS_ENTRY,
    ADMIN_ENVIRONMENTS_ENTRY,
    ADMIN_THEME_ENTRY,
    ADMIN_LLM_ENTRY
  ]
};

/**
 * Reihenfolge spiegelt den Anwender-Workflow:
 *
 *   Workflow:   Dashboard -> Scan hochladen -> Queue -> Reachability
 *               -> Fix-Verifikation -> Anomalie -> Waiver -> Berichte
 *   Uebersicht: CVEs, Komponenten, Alert-Historie, KI-Audit,
 *               Mandanten-KPIs
 *   Einstellungen: Einstellungen (Hauptseite) + Unterpunkte
 *                  Profile, Regeln, Produkte, Umgebungen, Theme
 */
const MENU: readonly MenuEntry[] = [
  // --- Workflow-Bereich ---
  {
    id: 'dashboard',
    label: 'Dashboard',
    path: '/dashboard',
    icon: 'dashboard',
    section: 'workflow',
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
    id: 'scan-upload',
    label: 'Scan hochladen',
    path: '/scans/upload',
    icon: 'cloud_upload',
    section: 'workflow',
    requiredRoles: [CVM_ROLES.ADMIN, CVM_ROLES.ASSESSOR]
  },
  {
    id: 'queue',
    label: 'Bewertungs-Queue',
    path: '/queue',
    icon: 'rule',
    section: 'workflow',
    requiredRoles: [
      CVM_ROLES.ASSESSOR,
      CVM_ROLES.REVIEWER,
      CVM_ROLES.APPROVER,
      CVM_ROLES.ADMIN
    ]
  },
  {
    id: 'reachability',
    label: 'Reachability',
    path: '/reachability',
    icon: 'account_tree',
    section: 'workflow',
    requiredRoles: [
      CVM_ROLES.ASSESSOR,
      CVM_ROLES.REVIEWER,
      CVM_ROLES.APPROVER,
      CVM_ROLES.ADMIN
    ]
  },
  {
    id: 'fix-verification',
    label: 'Fix-Verifikation',
    path: '/fix-verification',
    icon: 'verified',
    section: 'workflow',
    requiredRoles: [
      CVM_ROLES.ASSESSOR,
      CVM_ROLES.REVIEWER,
      CVM_ROLES.APPROVER,
      CVM_ROLES.ADMIN
    ]
  },
  {
    id: 'anomaly',
    label: 'Anomalie-Board',
    path: '/anomaly',
    icon: 'sensors',
    section: 'workflow',
    requiredRoles: [CVM_ROLES.AI_AUDITOR, CVM_ROLES.ADMIN]
  },
  {
    id: 'waivers',
    label: 'Waiver',
    path: '/waivers',
    icon: 'rule_folder',
    section: 'workflow',
    requiredRoles: [
      CVM_ROLES.VIEWER,
      CVM_ROLES.ASSESSOR,
      CVM_ROLES.REVIEWER,
      CVM_ROLES.APPROVER,
      CVM_ROLES.ADMIN
    ]
  },
  {
    id: 'reports',
    label: 'Berichte',
    path: '/reports',
    icon: 'description',
    section: 'workflow',
    requiredRoles: [
      CVM_ROLES.VIEWER,
      CVM_ROLES.REPORTER,
      CVM_ROLES.ADMIN
    ]
  },

  // --- Uebersicht-Bereich ---
  {
    id: 'cves',
    label: 'CVEs',
    path: '/cves',
    icon: 'bug_report',
    section: 'uebersicht',
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
    section: 'uebersicht',
    requiredRoles: [
      CVM_ROLES.VIEWER,
      CVM_ROLES.REPORTER,
      CVM_ROLES.ADMIN
    ]
  },
  {
    id: 'alerts-history',
    label: 'Alert-Historie',
    path: '/alerts/history',
    icon: 'history',
    section: 'uebersicht',
    requiredRoles: [
      CVM_ROLES.VIEWER,
      CVM_ROLES.REVIEWER,
      CVM_ROLES.ADMIN
    ]
  },
  {
    id: 'ai-audit',
    label: 'KI-Audit',
    path: '/ai-audit',
    icon: 'fact_check',
    section: 'uebersicht',
    requiredRoles: [CVM_ROLES.AI_AUDITOR, CVM_ROLES.ADMIN]
  },
  {
    id: 'tenant-kpi',
    label: 'Mandanten-KPIs',
    path: '/tenant-kpi',
    icon: 'insights',
    section: 'uebersicht',
    requiredRoles: [CVM_ROLES.ADMIN]
  },

  // --- Einstellungen (Hauptpunkt + Unterpunkte) ---
  SETTINGS_ENTRY
];

function hasMatchingRole(
  entry: MenuEntry,
  userRoleSet: ReadonlySet<string>
): boolean {
  return entry.requiredRoles.some((r) => userRoleSet.has(r));
}

/**
 * Filtert das statische Menue gegen die Rollen des angemeldeten Users.
 * Kinder-Eintraege werden ebenfalls gefiltert; ein Parent bleibt nur
 * sichtbar, wenn er selbst rollenkonform ist ODER mindestens ein Kind
 * rollenkonform ist.
 */
@Injectable({ providedIn: 'root' })
export class RoleMenuService {
  visibleEntries(userRoles: readonly string[]): readonly MenuEntry[] {
    const set = new Set(userRoles);
    return MENU.map((entry) => this.filterEntry(entry, set)).filter(
      (e): e is MenuEntry => e !== null
    );
  }

  /** Flache Liste aller Eintraege (inkl. Kinder) fuer hasAccess-Lookup. */
  allEntries(): readonly MenuEntry[] {
    const flat: MenuEntry[] = [];
    for (const entry of MENU) {
      flat.push(entry);
      for (const child of entry.children ?? []) {
        flat.push(child);
      }
    }
    return flat;
  }

  hasAccess(path: string, userRoles: readonly string[]): boolean {
    const entry = this.allEntries().find((m) => m.path === path);
    if (!entry) {
      return false;
    }
    const set = new Set(userRoles);
    return hasMatchingRole(entry, set);
  }

  private filterEntry(
    entry: MenuEntry,
    roles: ReadonlySet<string>
  ): MenuEntry | null {
    const selfVisible = hasMatchingRole(entry, roles);
    const filteredChildren = (entry.children ?? [])
      .map((c) => this.filterEntry(c, roles))
      .filter((c): c is MenuEntry => c !== null);
    if (!selfVisible && filteredChildren.length === 0) {
      return null;
    }
    if (filteredChildren.length === 0 && entry.children) {
      return { ...entry, children: [] };
    }
    return filteredChildren.length > 0
      ? { ...entry, children: filteredChildren }
      : entry;
  }
}
