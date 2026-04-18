/**
 * Keycloak-Realm-Rollen, die das CVM-Frontend kennt.
 *
 * Spiegelt {@code infra/keycloak/dev-realm.json} eins zu eins. Bei
 * Anpassungen am Realm muessen die Konstanten hier mitgepflegt werden.
 * Iteration 23 hat die feingranularen Rollen im Realm eingefuehrt;
 * Iteration 24 synchronisiert den UI-Stand.
 */
export const CVM_ROLES = {
  VIEWER: 'CVM_VIEWER',
  ASSESSOR: 'CVM_ASSESSOR',
  REVIEWER: 'CVM_REVIEWER',
  APPROVER: 'CVM_APPROVER',
  PROFILE_AUTHOR: 'CVM_PROFILE_AUTHOR',
  PROFILE_APPROVER: 'CVM_PROFILE_APPROVER',
  RULE_AUTHOR: 'CVM_RULE_AUTHOR',
  RULE_APPROVER: 'CVM_RULE_APPROVER',
  REPORTER: 'CVM_REPORTER',
  AI_AUDITOR: 'AI_AUDITOR',
  ADMIN: 'CVM_ADMIN'
} as const;

export type CvmRole = (typeof CVM_ROLES)[keyof typeof CVM_ROLES];

/**
 * Human-readable Kurzform fuer Rollen-Chips im Userpanel.
 */
export const CVM_ROLE_LABELS: Readonly<Record<CvmRole, string>> = {
  [CVM_ROLES.VIEWER]: 'Viewer',
  [CVM_ROLES.ASSESSOR]: 'Bewerter',
  [CVM_ROLES.REVIEWER]: 'Reviewer',
  [CVM_ROLES.APPROVER]: 'Freigeber',
  [CVM_ROLES.PROFILE_AUTHOR]: 'Profil-Autor',
  [CVM_ROLES.PROFILE_APPROVER]: 'Profil-Freigeber',
  [CVM_ROLES.RULE_AUTHOR]: 'Regel-Autor',
  [CVM_ROLES.RULE_APPROVER]: 'Regel-Freigeber',
  [CVM_ROLES.REPORTER]: 'Berichte',
  [CVM_ROLES.AI_AUDITOR]: 'KI-Audit',
  [CVM_ROLES.ADMIN]: 'Admin'
};
