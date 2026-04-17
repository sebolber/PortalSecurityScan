/**
 * Keycloak-Realm-Rollen, die das CVM-Frontend kennt.
 *
 * Mapping (siehe 07-Frontend-Shell.md, Konzept v0.2 Abschnitt 4.5):
 * - CVE_VIEWER     → Lesezugriff (Dashboard, Queue read-only).
 * - CVE_ASSESSOR   → darf PROPOSED-Vorschlaege anlegen.
 * - CVE_APPROVER   → darf approve/reject (Vier-Augen).
 * - CVE_ADMIN      → Profile/Regeln/Workflow-Admin.
 * - PRODUCT_OWNER  → Produkt-/Versionspflege, Berichte.
 * - AI_AUDITOR     → KI-Audit-Trail Lesezugriff.
 */
export const CVM_ROLES = {
  VIEWER: 'CVE_VIEWER',
  ASSESSOR: 'CVE_ASSESSOR',
  APPROVER: 'CVE_APPROVER',
  ADMIN: 'CVE_ADMIN',
  PRODUCT_OWNER: 'PRODUCT_OWNER',
  AI_AUDITOR: 'AI_AUDITOR'
} as const;

export type CvmRole = (typeof CVM_ROLES)[keyof typeof CVM_ROLES];
